#!/bin/bash
# set -x

echo "### Generate from created ./debug/hymnchtv-debug.aab; and install hymnchtv-debug.apks to the device locally ###"

if [[ $# -eq 0 ]] || [[ ! -f "./hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apks" ]]; then
  java -jar ../bundletool.jar build-apks --bundle=./hymnchtv/build/outputs/bundle/debug/hymnchtv-debug.aab --output=./hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apks --overwrite --local-testing
fi

java -jar ../bundletool.jar install-apks --apks=./hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apks




