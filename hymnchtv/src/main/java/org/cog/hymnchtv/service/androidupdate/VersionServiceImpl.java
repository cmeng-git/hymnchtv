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
package org.cog.hymnchtv.service.androidupdate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.cog.hymnchtv.HymnsApp;

/**
 * Android version service implementation:
 * The current version is parsed from android:versionName attribute of the PackageInfo.
 *
 * @author Eng Chong Meng
 */
public class VersionServiceImpl
{
    private static VersionServiceImpl mInstance;

    /**
     * Current version instance.
     */
    private int CURRENT_VERSION_CODE;
    private String CURRENT_VERSION_NAME;

    /**
     * Creates a new instance of <tt>VersionServiceImpl</tt> and parses the current version from
     * android:versionName attribute of the PackageInfo.
     */
    public static VersionServiceImpl getInstance() {
        if (mInstance == null)
            mInstance = new VersionServiceImpl();
        return mInstance;
    }

    public VersionServiceImpl()
    {
        mInstance = this;

        Context ctx = HymnsApp.getGlobalContext();
        PackageManager pckgMan = ctx.getPackageManager();
        try {
            PackageInfo pckgInfo = pckgMan.getPackageInfo(ctx.getPackageName(), 0);

            String versionName = pckgInfo.versionName;
            int versionCode = pckgInfo.versionCode;

            CURRENT_VERSION_NAME = versionName;
            CURRENT_VERSION_CODE = versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the <tt>Version</tt> of the current running hymnchtv app.
     *
     * @return  the <tt>Version</tt> of the current running hymntv app.
     */
    public int getCurrentVersionCode()
    {
        return CURRENT_VERSION_CODE;
    }

    public String getCurrentVersionName()
    {
        return CURRENT_VERSION_NAME;
    }
}
