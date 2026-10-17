#!/usr/bin/env bash
# Build and install both halves. Phone and watch must be signed with the SAME key
# (debug builds from one machine are) or the Data Layer will not connect them.
#
# Usage: tools/install.sh <phone-serial> <watch-serial>      (see `adb devices`)
set -euo pipefail
[[ $# -lt 2 ]] && { sed -n '2,5p' "$0"; exit 1; }
cd "$(dirname "$0")/.."
./gradlew :mobile:assembleDebug :wear:assembleDebug
adb -s "$1" install -r mobile/build/outputs/apk/debug/mobile-debug.apk
adb -s "$2" install -r wear/build/outputs/apk/debug/wear-debug.apk
echo "Installed. Optional: tools/grant-watch-powers.sh $2"
