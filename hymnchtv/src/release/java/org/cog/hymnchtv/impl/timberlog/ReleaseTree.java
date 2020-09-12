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

import android.util.Log;

import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Release tree to log only WARN, ERROR and WTF.
 * Do not log if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == TimberLevel.FINE);
 * Log Log.INFO only if enabled for released apk
 *
 * @author Eng Chong Meng
 */
public class ReleaseTree extends Timber.DebugTree
{
    @Override
    protected boolean isLoggable(@Nullable String tag, int priority)
    {
        // return (priority < TimberLog.FINE && priority > Log.DEBUG) && (priority != Log.INFO || TimberLog.isInfoEnabled());
        return (priority == Log.WARN || priority == Log.ERROR || priority == Log.ASSERT
                || (priority == Log.INFO && TimberLog.isInfoEnable));
    }

//    @Override
//    protected void log(int priority, String tag, String message, Throwable throwable)
//    {
//        super.log(priority, tag, message, throwable);
//
//        if (priority >= Log.ERROR) {
//            Crashlytics.log(priority, tag, message);
//
//            if (throwable != null) {
//                Crashlytics.logException(throwable);
//            }
//        }
//    }
}
