#!/bin/bash
#
# Copyright 2025 Eng Chong Meng
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# set -x

LIB_OPENCC_VER="1.1.9"
LIB_OPENCC="OpenCC"

if [[ -d ${LIB_OPENCC} ]] && [[ -f ${LIB_OPENCC}/package.json ]]; then
  version="$(grep 'version' < ${LIB_OPENCC}/package.json | sed 's/^.*\([1-9]\.[0-9]\.[0-9]\).*/\1/')"
  if [[ ${version} =~ ${LIB_OPENCC_VER} ]]; then
    echo -e "\n*** Current openCC found version:\n ${version}"
    exit 0
  fi
fi

rm -rf ${LIB_OPENCC}
echo -e "\n=== Fetching library source for ${LIB_OPENCC}: ${LIB_OPENCC_VER} ===="
wget -O- https://github.com/BYVoid/OpenCC/archive/refs/tags/ver.${LIB_OPENCC_VER}.tar.gz | tar xz --strip-components=1 --one-top-level=${LIB_OPENCC}
echo -e "=== Completed openCC library source update ===="
