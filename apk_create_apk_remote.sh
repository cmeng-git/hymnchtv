#!/bin/bash

fnapk="hymnchtv-debug"

echo "### Generate single monolithic $fnapk.apk from ./debug/$fnapk.aab, for remote installation ###"

java -jar ../bundletool.jar build-apks --bundle=./hymnchtv/build/outputs/bundle/debug/$fnapk.aab --output=./hymnchtv/build/outputs/apk/debug/$fnapk.apks --overwrite --local-testing --mode=universal

pushd ~/workspace/android/hymnchtv/hymnchtv/build/outputs/apk/debug || exit

apktool d -f -s $fnapk.apks -o ./tmp
mv ./tmp/unknown/universal.apk  ~/workspace/android/hymnchtv/hymnchtv/release/$fnapk.apk
rm $fnapk.apks

popd || return
