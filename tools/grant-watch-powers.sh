#!/usr/bin/env bash
# Grant Cufflink the two watch permissions that Galaxy watches give no screen for.
#
#   WRITE_SETTINGS          -> set ringtone / notification / alarm sound in one tap
#   MANAGE_EXTERNAL_STORAGE -> browse every folder, not just the media library
#
# Usage: tools/grant-watch-powers.sh [watch-serial]
#   Watch: Settings > Developer options > Wireless debugging, then
#   `adb pair <ip:port>` and `adb connect <ip:port>` (pairing first — connect alone fails).
set -euo pipefail
PKG=com.ogenrwotdaniel.cufflink
ADB=(adb)
[[ $# -ge 1 ]] && ADB=(adb -s "$1")

"${ADB[@]}" shell pm path "$PKG" >/dev/null || { echo "Cufflink is not installed on that device." >&2; exit 1; }
for op in WRITE_SETTINGS MANAGE_EXTERNAL_STORAGE; do
  "${ADB[@]}" shell appops set "$PKG" "$op" allow
  printf '%-24s %s\n' "$op" "$("${ADB[@]}" shell appops get "$PKG" "$op")"
done
echo "Done. Reopen Cufflink › Powers on the watch: both should show ✓."
