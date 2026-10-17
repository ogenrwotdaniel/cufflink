#!/usr/bin/env bash
# Build and install both halves. Phone and watch must be signed with the SAME key
# (debug builds from one machine are) or the Data Layer will not connect them.
# The watch gets the R8-shrunk release build (~2 MB, debug-signed): Wi-Fi adb drops a 38 MB push.
#
# Usage: tools/install.sh <phone-serial> <watch-serial>      (see `adb devices`)
set -euo pipefail
[[ $# -lt 2 ]] && { sed -n '2,6p' "$0"; exit 1; }
cd "$(dirname "$0")/.."
./gradlew :mobile:assembleDebug :wear:assembleRelease
adb -s "$1" install -r mobile/build/outputs/apk/debug/mobile-debug.apk
adb -s "$2" install -r --no-streaming wear/build/outputs/apk/release/wear-release.apk
echo "Installed. Optional: tools/grant-watch-powers.sh $2"
