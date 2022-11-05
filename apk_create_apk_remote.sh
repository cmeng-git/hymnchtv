#!/bin/bash

echo "### Generate single monolithic hymnchtv-debug.apk from ./debug/hymnchtv-debug.aab, for remote installation ###"

java -jar ../bundletool.jar build-apks --bundle=./hymnchtv/build/outputs/bundle/debug/hymnchtv-debug.aab --output=./hymnchtv/build/outputs/apk/debug/hymnchtv-debug2.apks --overwrite --local-testing --mode=universal

pushd ~/workspace/android/hymnchtv/hymnchtv/build/outputs/apk/debug || exit

apktool d -f -s hymnchtv-debug2.apks -o ./tmp
mv ./tmp/unknown/universal.apk  ~/workspace/android/hymnchtv/hymnchtv/release/hymnchtv-debug.apk

popd || return
