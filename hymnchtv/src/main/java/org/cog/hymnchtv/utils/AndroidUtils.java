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
package org.cog.hymnchtv.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * The <tt>AndroidUtils</tt> class provides a set of utility methods allowing an easy way to show
 * an alert dialog on android, show a general notification, etc.
 *
 * @author Eng Chong Meng
 */
public class AndroidUtils {
    /**
     * Number of milliseconds in a second.
     */
    public static final long MILLIS_PER_SECOND = 1000;

    /**
     * Number of milliseconds in a standard minute.
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    /**
     * Number of milliseconds in a standard hour.
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * Number of milliseconds in a standard day.
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    /**
     * Formats the given long to X hour, Y min, Z sec.
     *
     * @param millis the time in milliseconds to format
     *
     * @return the formatted seconds
     */
    public static String formatSeconds(long millis) {
        long[] values = new long[4];
        values[0] = millis / MILLIS_PER_DAY;
        values[1] = (millis / MILLIS_PER_HOUR) % 24;
        values[2] = (millis / MILLIS_PER_MINUTE) % 60;
        values[3] = (millis / MILLIS_PER_SECOND) % 60;

        String[] fields = {" d ", " h ", " min ", " sec"};

        StringBuilder buf = new StringBuilder(64);
        boolean valueOutput = false;

        for (int i = 0; i < 4; i++) {
            long value = values[i];

            if (value == 0) {
                if (valueOutput)
                    buf.append('0').append(fields[i]);
            }
            else {
                valueOutput = true;
                buf.append(value).append(fields[i]);
            }
        }
        return buf.toString().trim();
    }

    public static String UrlEncode(String url) throws UnsupportedEncodingException {
        // Need to encode chinese link for safe access; revert all "%3A" and "%2F" to ":" and "/" etc
        String encDnLnk = URLEncoder.encode(url, "UTF-8")
                .replace("%23", "#")
                .replace("%26", "&")
                .replace("%2F", "/")
                .replace("%3A", ":")
                .replace("%3B", ";")
                .replace("%3D", "=")
                .replace("%3F", "?");
        // Timber.d("Download URL link encoded: %s", encDnLnk);
        return encDnLnk;
    }
}