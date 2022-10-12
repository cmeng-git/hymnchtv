/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv;

import java.util.HashMap;
import java.util.Map;

/**
 * Medias type currently supported by hymnchtv
 * (Many are yet to be implemented)
 *
 * @author Eng Chong Meng
 */
public enum MediaType
{
    HYMN_MEDIA(0),
    HYMN_BANZOU(1),
    HYMN_JIAOCHANG(2),
    HYMN_CHANGSHI(3);

    private final int value;
    private static final Map<Integer, MediaType> map = new HashMap<>();

    static {
        for (MediaType mediaType : MediaType.values()) {
            map.put(mediaType.value, mediaType);
        }
    }

    MediaType(int value) {
        this.value = value;
    }

    public static MediaType valueOf(int hymnType) {
        return map.get(hymnType);
    }

    public int getValue() {
        return value;
    }

    String mediaDir[] = {"_midi/%s.mid", "_ac/%s.mp3", "_ln/%s.mp3", "_mp3/%s.mp3", ""};

    public String getSubDir(MediaType mType) {
        return mediaDir[mType.getValue()];
    }
}