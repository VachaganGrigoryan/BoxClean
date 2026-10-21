# BoxClean

Debloat a Xiaomi Mi Box S 2nd gen (Google TV 11) over network ADB and give it a minimal, offline home
screen. Our own rebuild of the idea behind [CleanYourTV](https://github.com/Gathin23/cleanyourtv).

Everything is reversible: packages are **disabled** for user 0 (`pm disable-user`), never uninstalled.

## Steps
1. On the box: Settings → System → About → press OK on **Build** 7 times → Developer options →
   **USB debugging ON**. Note the IP (Settings → Network). Accept "Allow debugging?" when it appears.
2. Read-only: `TV_HOST=<ip> uv run tools/boxclean.py probe` and `... backup` (→ `backups/<time>/`).
3. Debloat in batches, screenshot between them:
   `TV_HOST=<ip> uv run tools/boxclean.py disable lists/1-telemetry-ads.txt --dry-run` (then without),
   `TV_HOST=<ip> uv run tools/boxclean.py shot shots/01.png`.
4. Launcher: `TV_HOST=<ip> launcher/build-install.sh`, then `... disable lists/3-launcher.txt`.
   If it skips setupwraith because it owns your Wi-Fi: forget the network, re-join it, run again.
5. Close ADB: Developer options → USB debugging **OFF**, "Revoke USB debugging authorisations", reboot,
   then `TV_HOST=<ip> uv run tools/boxclean.py closed` must say "refused".

## Undo
`TV_HOST=<ip> uv run tools/boxclean.py restore lists/<list>.txt` re-enables what that list disabled
(from `<list>.record.json`, written before the first change) and gives Home back to its old owner.
Run `restore lists/3-launcher.txt` before a factory reset.
After `restore lists/2-personal.txt`, put the Google screensaver back by hand (restore doesn't touch settings):
`adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop`.

## Change the dock or the remote buttons
Edit `launcher/app/src/main/res/values/config.xml` and run `launcher/build-install.sh`:
- `dock_packages`: the four dock apps, in order.
- `app_keys`: remote buttons whose own app is off, as `KEYCODE_BUTTON_7:ru.kinopoisk.tv`. On this remote
  Prime Video = `BUTTON_7`, Xiaomi TV+ = `BUTTON_5`. YouTube and Netflix stay with Xiaomi's own handler
  (`partnercustomizer`), and Apps opens this launcher's grid.

The buttons go through `KeyService`, an accessibility service that filters keys only and never sees the
screen. `build-install.sh` re-enables it after every install, because a reinstall unbinds it.
Check it with `adb shell dumpsys accessibility | grep -A3 'Bound services'`.

Turned back on by hand after the batches: `com.netflix.ninja` (for the Netflix button).
Screensaver: `settings put secure screensaver_components com.boxclean.home/.ClockDream`.

## Home layouts
Two home screens, switched with the **Layout** button on either one (remembered across reboots):
- **Bento** (default): a board of rounded cells (clock, the four dock apps, Wi-Fi, Settings, Layout) fills
  the first screen, and every other app sits in a grid below it.
- **Classic**: the Apple TV style header, hero name, dock and grid.

Look and motion are adapted from CleanYourTV (MIT); see `NOTICE`.

## Checks
`uv run ruff check . && uv run ruff format --check . && uv run pytest -q`;
`cd launcher && ./gradlew testReleaseUnitTest lintRelease assembleRelease`.
