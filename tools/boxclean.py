#!/usr/bin/env python3
"""Debloat an Android TV box over network ADB, and put it back.

    TV_HOST=192.168.1.50 uv run tools/boxclean.py probe
    ... backup                        save packages, props and settings to backups/<time>/
    ... disable lists/x.txt [--dry-run] [--force PKG]
    ... restore lists/x.txt [--dry-run]
    ... shot shots/01.png
    ... closed                        exit 0 only when port 5555 refuses the connection

Packages are disabled for user 0 (pm disable-user), never uninstalled. Before the first change the
state of every listed package and the Home component go to <list>.record.json; restore reads it.
"""

import argparse
import datetime
import json
import os
import re
import shutil
import socket
import subprocess
import sys
from pathlib import Path

PKG_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")
COMPONENT_RE = re.compile(r"^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$")
SETUPWRAITH = "com.google.android.tungsten.setupwraith"
# They answer HOME but are not launchers; Google TV's wizard taking Home crash-loops the box.
NOT_LAUNCHERS = {SETUPWRAITH, "com.android.tv.settings", "com.android.settings", "com.android.provision"}
STOCK_LAUNCHERS = {
    "com.google.android.apps.tv.launcherx",
    "com.google.android.tvlauncher",
    "com.google.android.leanbacklauncher",
}
HOME_INTENT = "-a android.intent.action.MAIN -c android.intent.category.HOME"


# ---- parsing (pure) ----


def parse_list(text):
    """Package names from a debloat list. Refuses the whole list if any entry isn't a bare name,
    because each name ends up inside a shell command on the TV."""
    names = []
    for line in text.lstrip("﻿").splitlines():
        entry = line.split("#", 1)[0].strip()
        if entry:
            names.append(entry)
    bad = [n for n in names if not PKG_RE.fullmatch(n)]
    if bad:
        raise ValueError(f"not a package name: {', '.join(map(repr, bad))}")
    return names


def parse_packages(out):
    return {
        line.strip().split(":", 1)[1].rsplit("=", 1)[-1]
        for line in out.splitlines()
        if line.startswith("package:")
    }


def home_component(out):
    """The component `cmd package resolve-activity --brief` names, or None (e.g. the chooser "android")."""
    lines = [line.strip() for line in out.splitlines() if line.strip()]
    return lines[-1] if lines and COMPONENT_RE.fullmatch(lines[-1]) else None


def components(out):
    return re.findall(r"^\s*([A-Za-z0-9_.]+/[A-Za-z0-9_.$]+)\s*$", out, re.MULTILINE)


def wifi_owners(out):
    return set(re.findall(r"cname=([A-Za-z0-9_.]+)", out))


def requests_tvinput(dumpsys_out):
    """Only the package's own requested-permissions block counts, not its shared user's."""
    m = re.search(
        r"requested permissions:(.*?)(install permissions:|runtime permissions:|User \d+:|$)",
        dumpsys_out,
        re.DOTALL,
    )
    return bool(m) and "TV_INPUT_HARDWARE" in m.group(1)


# ---- planning (pure) ----


def plan_disable(pkgs, *, installed, disabled, home, claimants, wifi, tvinput, force):
    """(packages to disable, {skipped package: reason})."""
    todo, skipped = [], {}
    home_pkg = home.split("/")[0] if home else None
    # Home unknown (query failed, or the chooser answered): treat every launcher as the owner.
    homes = {home_pkg} if home_pkg else {c.split("/")[0] for c in claimants} | STOCK_LAUNCHERS
    for p in pkgs:
        if p not in installed:
            skipped[p] = "not installed"
        elif p in disabled:
            skipped[p] = "already disabled"
        elif p in force:
            todo.append(p)
        elif p in homes and not _other_launchers(p, claimants, pkgs):
            skipped[p] = (
                "it owns Home and no other launcher is enabled; install the new launcher first "
                "(or --force it)"
            )
        elif p == SETUPWRAITH and (not wifi or SETUPWRAITH in wifi):
            skipped[p] = (
                "the saved Wi-Fi network belongs to it (or could not be read): disabling it deletes "
                "that network. Forget and re-join Wi-Fi in Settings first (or --force it on Ethernet)"
            )
        elif p in tvinput:
            skipped[p] = "requests TV_INPUT_HARDWARE, so it may drive HDMI/tuner inputs (--force it if not)"
        else:
            todo.append(p)
    return todo, skipped


def _other_launchers(home_pkg, claimants, pkgs):
    return {
        c.split("/")[0]
        for c in claimants
        if not c.endswith("FallbackHome") and c.split("/")[0] not in NOT_LAUNCHERS | {home_pkg} | set(pkgs)
    }


def update_record(path, states, home):
    """Merge into the record without changing anything already in it: the first-seen state is the
    one to go back to, so a retry or a second batch must not overwrite it."""
    path = Path(path)
    rec = json.loads(path.read_text()) if path.exists() else {"home": None, "packages": {}}
    if rec["home"] is None:
        rec["home"] = home
    for p, s in states.items():
        rec["packages"].setdefault(p, s)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(rec, indent=2) + "\n")
    tmp.replace(path)


def plan_restore(record, *, disabled_now):
    pkgs = record["packages"]
    bad = [p for p in pkgs if not PKG_RE.fullmatch(p)]
    home = record.get("home")
    if bad or (home is not None and not COMPONENT_RE.fullmatch(home)):
        raise ValueError(f"record has invalid entries: {bad or home!r}")
    cmds = [f"pm enable {p}" for p, s in pkgs.items() if s == "enabled" and p in disabled_now]
    if home:
        cmds.append(f"cmd package set-home-activity {home}")
    return cmds


# ---- the TV ----


def adb_binary():
    sdk = Path.home() / "Library/Android/sdk/platform-tools/adb"
    return os.environ.get("ADB") or shutil.which("adb") or str(sdk)


class Tv:
    def __init__(self, host, port):
        self.adb = adb_binary()
        self.serial = f"{host}:{port}"
        out = subprocess.run(
            [self.adb, "connect", self.serial], check=False, capture_output=True, text=True, timeout=60
        )
        if "connected to" not in out.stdout:
            sys.exit(
                f"adb connect {self.serial}: {(out.stdout + out.stderr).strip()}\n"
                "Is USB debugging on, and did you accept 'Allow debugging?' on the TV?"
            )
        state = self.run("get-state", check=False).strip()
        if state != "device":
            sys.exit(
                f"TV is '{state or 'unreachable'}': accept the 'Allow debugging?' prompt on the TV, "
                "then run this again."
            )

    def run(self, *args, check=True, binary=False):
        r = subprocess.run(
            [self.adb, "-s", self.serial, *args],
            check=False,
            capture_output=True,
            text=not binary,
            timeout=180,
        )
        if check and r.returncode != 0:
            raise RuntimeError(f"adb {' '.join(args)}: {r.stderr or r.stdout}")
        return r.stdout

    def sh(self, cmd):
        return self.run("shell", cmd, check=False).replace("\r\n", "\n")


def tv_from_env():
    host = os.environ.get("TV_HOST")
    if not host:
        sys.exit("Set TV_HOST to the box's IP, e.g. TV_HOST=192.168.1.50 (Settings > Network on the box)")
    return Tv(host, int(os.environ.get("TV_PORT", "5555")))


def cmd_probe(tv, _args):
    for p in (
        "ro.product.model",
        "ro.product.device",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.fingerprint",
        "ro.adb.secure",
        "service.adb.tcp.port",
    ):
        print(f"{p} = {tv.sh(f'getprop {p}').strip()}")
    print(f"home = {home_component(tv.sh(f'cmd package resolve-activity --brief {HOME_INTENT}'))}")


def cmd_backup(tv, _args):
    out = Path("backups") / datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    out.mkdir(parents=True)
    for name, cmd in {
        "getprop": "getprop",
        "packages": "pm list packages -f",
        "packages-disabled": "pm list packages -d",
        "packages-system": "pm list packages -s",
        "packages-third-party": "pm list packages -3",
        "settings-global": "settings list global",
        "settings-secure": "settings list secure",
        "settings-system": "settings list system",
        "home-resolve": f"cmd package resolve-activity --brief {HOME_INTENT}",
        "home-claimants": f"cmd package query-activities --brief {HOME_INTENT}",
        "wifi-owners": "dumpsys wifi | grep -o 'cname=[^ ]*' | sort -u",
    }.items():
        (out / f"{name}.txt").write_text(tv.sh(cmd))
    print(f"saved {len(list(out.iterdir()))} files to {out}/")


def tv_state(tv):
    installed = parse_packages(tv.sh("pm list packages"))
    disabled = parse_packages(tv.sh("pm list packages -d"))
    if not installed:
        raise RuntimeError("the TV listed no packages; check the connection with `probe`")
    return installed, disabled


def cmd_disable(tv, args):
    list_path = Path(args.list)
    pkgs = parse_list(list_path.read_text())
    installed, disabled = tv_state(tv)
    home = home_component(tv.sh(f"cmd package resolve-activity --brief {HOME_INTENT}"))
    present = [p for p in pkgs if p in installed and p not in disabled]
    todo, skipped = plan_disable(
        pkgs,
        installed=installed,
        disabled=disabled,
        home=home,
        claimants=components(tv.sh(f"cmd package query-activities --brief {HOME_INTENT}")),
        wifi=wifi_owners(tv.sh("dumpsys wifi")) if SETUPWRAITH in present else set(),
        tvinput={p for p in present if requests_tvinput(tv.sh(f"dumpsys package {p}"))},
        force=set(args.force),
    )
    for p, why in skipped.items():
        print(f"skip {p}: {why}", file=sys.stderr)
    if args.dry_run or not todo:
        print("\n".join(f"would disable {p}" for p in todo) or "nothing to disable")
        return 0
    # Written before the first change: a record taken afterwards would describe a half-changed TV.
    record = list_path.with_name(list_path.name + ".record.json")
    states = {
        p: ("absent" if p not in installed else "disabled" if p in disabled else "enabled") for p in pkgs
    }
    update_record(record, states, home)
    for p in todo:  # one pm per call: chained commands can stop silently after the first
        print(f"{p}: {tv.sh(f'pm disable-user --user 0 {p}').strip()}")
    still = [p for p in todo if p not in parse_packages(tv.sh("pm list packages -d"))]
    if still:
        print(f"NOT disabled (firmware refused?): {' '.join(still)}", file=sys.stderr)
        return 1
    print(f"disabled {len(todo)}; undo with: restore {args.list}")
    return 0


def cmd_restore(tv, args):
    list_path = Path(args.list)
    record = list_path.with_name(list_path.name + ".record.json")
    if not record.exists():
        sys.exit(f"no record at {record}: nothing was disabled from this list by this tool")
    _, disabled = tv_state(tv)
    for c in plan_restore(json.loads(record.read_text()), disabled_now=disabled):
        print(f"$ {c}")
        if not args.dry_run:
            print(tv.sh(c).strip())
    return 0


def cmd_shot(tv, args):
    png = tv.run("exec-out", "screencap -p", binary=True)
    Path(args.png).parent.mkdir(parents=True, exist_ok=True)
    Path(args.png).write_bytes(png)
    if not png.startswith(b"\x89PNG"):
        print(f"{args.png}: no image ({len(png)} bytes); video/DRM screens capture as empty", file=sys.stderr)
        return 1
    print(f"{args.png} ({len(png)} bytes)")
    return 0


def cmd_closed(_tv, _args):
    host, port = os.environ.get("TV_HOST", ""), int(os.environ.get("TV_PORT", "5555"))
    try:
        socket.create_connection((host, port), timeout=3).close()
    except ConnectionRefusedError:
        print(f"{host}:{port} refused: network ADB is closed")
        return 0
    except OSError as e:
        print(f"{host}:{port}: {e} - only 'refused' proves it is closed; is the box on?", file=sys.stderr)
        return 2
    print(f"{host}:{port} is OPEN: turn USB debugging off on the box and reboot it", file=sys.stderr)
    return 1


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("probe")
    sub.add_parser("backup")
    d = sub.add_parser("disable")
    d.add_argument("list")
    d.add_argument("--dry-run", action="store_true")
    d.add_argument("--force", action="append", default=[], metavar="PKG", help="skip the guards for PKG")
    r = sub.add_parser("restore")
    r.add_argument("list")
    r.add_argument("--dry-run", action="store_true")
    s = sub.add_parser("shot")
    s.add_argument("png")
    sub.add_parser("closed")
    args = ap.parse_args(argv)
    handler = globals()[f"cmd_{args.cmd}"]
    if args.cmd == "closed":
        if not os.environ.get("TV_HOST"):
            sys.exit("Set TV_HOST")
        return handler(None, args)
    return handler(tv_from_env(), args) or 0


if __name__ == "__main__":
    sys.exit(main())
