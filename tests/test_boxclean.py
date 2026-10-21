import json

import boxclean as bc
import pytest

LAUNCHERX = "com.google.android.apps.tv.launcherx"
OURS = "com.boxclean.home"


def test_parse_list_strips_bom_crlf_comments():
    text = "﻿# header\r\ncom.a.b   # note\r\n\r\n  com.c_d.e\n"
    assert bc.parse_list(text) == ["com.a.b", "com.c_d.e"]


@pytest.mark.parametrize("bad", ["com.a;reboot", "nodots", "com.a b", "$(id).x", "com.a.b&&rm"])
def test_parse_list_refuses_whole_list_on_bad_entry(bad):
    with pytest.raises(ValueError, match="not a package name"):
        bc.parse_list(f"com.ok.one\n{bad}\n")


def test_parse_packages_handles_paths():
    out = "package:com.a.b\npackage:/system/app/X/X.apk=com.x.y\r\njunk\n"
    assert bc.parse_packages(out) == {"com.a.b", "com.x.y"}


def test_home_component_rejects_chooser():
    assert bc.home_component("priority=0 preferredOrder=0\n" + LAUNCHERX + "/.MainActivity\n") == (
        LAUNCHERX + "/.MainActivity"
    )
    assert bc.home_component("android") is None
    assert bc.home_component("") is None


def test_wifi_owners():
    out = "Network cname=com.google.android.tungsten.setupwraith x\n cname=com.android.tv.settings\n"
    assert bc.wifi_owners(out) == {"com.google.android.tungsten.setupwraith", "com.android.tv.settings"}


def plan(pkgs, **kw):
    state = {
        "installed": set(pkgs) | {OURS, LAUNCHERX, bc.SETUPWRAITH},
        "disabled": set(),
        "home": LAUNCHERX + "/.MainActivity",
        "claimants": [LAUNCHERX + "/.MainActivity", bc.SETUPWRAITH + "/.RecoveryActivity"],
        "wifi": {"com.android.tv.settings"},
        "tvinput": set(),
        "force": set(),
    }
    return bc.plan_disable(pkgs, **(state | kw))


def test_plan_skips_home_owner_when_no_other_launcher():
    todo, skipped = plan(["com.x.ads", LAUNCHERX])
    assert todo == ["com.x.ads"]
    assert LAUNCHERX in skipped


def test_plan_allows_home_owner_once_our_launcher_claims_home():
    claimants = [LAUNCHERX + "/.MainActivity", OURS + "/.MainActivity"]
    todo, _ = plan([LAUNCHERX], claimants=claimants)
    assert todo == [LAUNCHERX]


def test_plan_fallbackhome_and_settings_do_not_count_as_launchers():
    claimants = [
        LAUNCHERX + "/.Main",
        "com.android.tv.settings/.system.FallbackHome",
        "com.x.y/.FallbackHome",
    ]
    todo, skipped = plan([LAUNCHERX], claimants=claimants)
    assert todo == [] and LAUNCHERX in skipped


def test_plan_setupwraith_guard():
    assert plan([bc.SETUPWRAITH], wifi={bc.SETUPWRAITH})[0] == []
    assert plan([bc.SETUPWRAITH], wifi=set())[0] == []  # unknown owner: stay safe
    assert plan([bc.SETUPWRAITH])[0] == [bc.SETUPWRAITH]
    assert plan([bc.SETUPWRAITH], wifi=set(), force={bc.SETUPWRAITH})[0] == [bc.SETUPWRAITH]


def test_plan_skips_tvinput_absent_and_already_disabled():
    todo, skipped = plan(
        ["com.hw.input", "com.gone.app", "com.off.app", "com.x.ads"],
        installed={"com.hw.input", "com.off.app", "com.x.ads", LAUNCHERX},
        disabled={"com.off.app"},
        tvinput={"com.hw.input"},
    )
    assert todo == ["com.x.ads"]
    assert set(skipped) == {"com.hw.input", "com.gone.app", "com.off.app"}


def test_record_first_seen_state_wins(tmp_path):
    path = tmp_path / "l.record.json"
    bc.update_record(path, {"com.a.b": "enabled"}, "h/.A")
    bc.update_record(path, {"com.a.b": "disabled", "com.c.d": "enabled"}, "h/.B")
    rec = json.loads(path.read_text())
    assert rec == {"home": "h/.A", "packages": {"com.a.b": "enabled", "com.c.d": "enabled"}}


def test_plan_restore_enables_only_what_was_enabled_and_restores_home():
    rec = {"home": LAUNCHERX + "/.MainActivity", "packages": {"com.a.b": "enabled", "com.c.d": "disabled"}}
    cmds = bc.plan_restore(rec, disabled_now={"com.a.b", "com.c.d"})
    assert cmds == [
        "pm enable com.a.b",
        f"cmd package set-home-activity {LAUNCHERX}/.MainActivity",
    ]


def test_plan_restore_refuses_tampered_record():
    with pytest.raises(ValueError):
        bc.plan_restore({"home": None, "packages": {"x;reboot": "enabled"}}, disabled_now=set())
    with pytest.raises(ValueError):
        bc.plan_restore({"home": "a/b; reboot", "packages": {}}, disabled_now=set())


def test_plan_guards_launchers_when_home_is_unknown():
    # Home query failed or answered the chooser: any HOME claimant or known stock launcher stays.
    assert plan([LAUNCHERX], home=None)[0] == []
    assert plan([LAUNCHERX], home=None, claimants=[])[0] == []
    claimants = [LAUNCHERX + "/.MainActivity", OURS + "/.MainActivity"]
    assert plan([LAUNCHERX], home=None, claimants=claimants)[0] == [LAUNCHERX]
