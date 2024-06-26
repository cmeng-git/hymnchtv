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
package org.cog.hymnchtv.glide;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.*;

import timber.log.Timber;

/**
 * Class implements the reader for the android Play Asset Delivery object for the glideApp
 *
 * @author Eng Chong Meng
 */
public class AssetFile
{
    private final String mPath;
    private static AssetManager assetManager = null;

    public AssetFile(Context context, String path)
    {
        mPath = path;
        if (assetManager == null) {
            try {
                Context ctx = context.createPackageContext("org.cog.hymnchtv", 0);
                assetManager = ctx.getAssets();
            } catch (Exception e) {
                Timber.w("Create AssetManager Exception: %s", e.getMessage());
            }
        }
    }

    public InputStream getInputStream()
    {
        try {
            return (assetManager == null) ? null : assetManager.open(mPath);
        } catch (Exception e) {
            Timber.w("AssetFile get inputStream exception: %s", e.getMessage());
            return null;
        }
    }

    public String getPath()
    {
        return mPath;
    }
}
