#!/usr/bin/env bash
# The no-app fallback: put an audio file straight into the watch's system sound
# picker over adb. Useful before Cufflink is installed, or to check that the watch
# picks up custom sounds at all.
#
# Usage: tools/push-sound.sh <file> <ringtone|notification|alarm> [watch-serial]
# Then on the watch: Settings > Sounds and vibration > Ringtone (or Notification sound).
set -euo pipefail
[[ $# -lt 2 ]] && { sed -n '2,8p' "$0"; exit 1; }
FILE=$1; ROLE=$2
ADB=(adb)
[[ $# -ge 3 ]] && ADB=(adb -s "$3")
case "$ROLE" in
  ringtone) DIR=Ringtones ;;
  notification) DIR=Notifications ;;
  alarm) DIR=Alarms ;;
  *) echo "role must be ringtone, notification or alarm" >&2; exit 1 ;;
esac
NAME=$(basename "$FILE")
"${ADB[@]}" push "$FILE" "/sdcard/$DIR/$NAME"
# Ask MediaStore to index it now rather than at the next reboot.
"${ADB[@]}" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d "file:///sdcard/$DIR/$NAME" >/dev/null || true
echo "Pushed to /sdcard/$DIR/$NAME."
echo "If it is missing from the watch's sound picker, restart the watch once (forces a media scan)."
