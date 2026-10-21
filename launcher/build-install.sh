#!/usr/bin/env bash
# Build the launcher, install it on the box and make it Home.
# Disabling the stock launcher is a separate, guarded step: uv run tools/boxclean.py disable lists/3-launcher.txt
# Usage: TV_HOST=192.168.1.50 launcher/build-install.sh
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the box IP, e.g. TV_HOST=192.168.1.50 $0}"
ADB="${ADB:-$(command -v adb || echo "$HOME/Library/Android/sdk/platform-tools/adb")}"
S="$TV_HOST:${TV_PORT:-5555}"
APK=app/build/outputs/apk/release/app-release.apk
HOME_ACTIVITY=com.boxclean.home/.MainActivity

[ -f local.properties ] || echo "sdk.dir=${ANDROID_HOME:-$HOME/Library/Android/sdk}" > local.properties
./gradlew -q --console=plain assembleRelease

OUT="$("$ADB" connect "$S")"
case "$OUT" in *"connected to"*) ;; *) echo "adb connect $S: $OUT" >&2; exit 1 ;; esac

# `adb install` can wait forever for an on-screen confirmation on some firmware; pm install doesn't.
if ! "$ADB" -s "$S" install -r "$APK"; then
  "$ADB" -s "$S" push "$APK" /data/local/tmp/boxclean-home.apk
  "$ADB" -s "$S" shell pm install -r /data/local/tmp/boxclean-home.apk
  "$ADB" -s "$S" shell rm -f /data/local/tmp/boxclean-home.apk
fi

"$ADB" -s "$S" shell cmd package set-home-activity "$HOME_ACTIVITY"
"$ADB" -s "$S" shell input keyevent KEYCODE_HOME
sleep 2
FOCUS="$("$ADB" -s "$S" shell dumpsys activity activities | grep -m1 -E 'mFocusedApp|topResumedActivity' || true)"
echo "$FOCUS"
case "$FOCUS" in
  *com.boxclean.home*) echo "Home is now BoxClean Home." ;;
  *) echo "Installed, but Home still opens something else. On Google TV the stock launcher and the setup" \
          "wizard keep Home until they are disabled: uv run tools/boxclean.py disable lists/3-launcher.txt" ;;
esac
