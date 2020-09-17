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
    HYMN_MIDI(0),
    HYMN_ACCPLAY(1),
    HYMN_LEARN(2),
    HYMN_MP3(3);

    private final int value;
    private static final Map<Integer, MediaType> map = new HashMap<>();

    MediaType(int value) {
        this.value = value;
    }

    static {
        for (MediaType mediaType : MediaType.values()) {
            map.put(mediaType.value, mediaType);
        }
    }

    public static MediaType valueOf(int hymnType) {
        return (MediaType) map.get(hymnType);
    }

    public int getValue() {
        return value;
    }

    String mediaDir[] = {"_midi/%s.mid", "_ac/%s.mp3", "_ln/%s.mp3", "_mp3/%s.mp3"};

    public String getSubDir(MediaType mType) {
        return mediaDir[mType.getValue()];
    }
}

/*
learn: https://heavenlyfood.cn/hymns/music/bu/B123.mp3

mp3: https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E8%A1%A5%E5%85%85%E6%9C%AC/01%E7%81%B5%E4%B8%8E%E7%94%9F%E5%91%BD/B145%E5%BD%93%E5%B0%86%E4%B8%BB%E6%9D%83%E5%AE%8C%E5%85%A8%E5%BD%92%E4%B8%BB.mp3

mp3: https://heavenlyfood.cn/hymnal/%E8%AF%97%E6%AD%8C/%E8%A1%A5%E5%85%85%E6%9C%AC/01%E7%81%B5%E4%B8%8E%E7%94%9F%E5%91%BD/B123%E8%80%B6%E7%A8%A3%E6%B4%BB%E5%9C%A8%E6%88%91%E9%87%8C%E9%9D%A2.mp3
mp3: https://heavenlyfood.cn/hymnal/诗歌/补充本/01灵与生命#B123耶稣活在我里面.mp3 => need above to download
*/
