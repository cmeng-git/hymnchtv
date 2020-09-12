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
package org.cog.hymnchtv.impl.timberlog;

/**
 * Class for defining the scope of debug info. The settings here are for standard release
 *
 * @author Eng Chong Meng
 */
public class TimberLog
{
    /**
     * Priority constant for the println method; use Timber.log; mainly for fine tracing debug messages
     */
    public static final int FINER = 10;
    public static final int FINEST = 11;

    /*
     * Set this to true to enable Timber.FINEST for tracing debug message.
     * It is also used to collect and format info for more detailed debug message display.
     */
    public static boolean isFinestEnable = false;

    /*
     * Set this to true to enable Timber.FINER tracing debug message
     */
    public static boolean isTraceEnable = false;

    /**
     * To specify if the info logging is enabled for released version
     */
    public static boolean isInfoEnable = true;
}
