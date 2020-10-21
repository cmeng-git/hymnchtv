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

import com.google.android.vending.expansion.zipfile.APKExpansionSupport;
import com.google.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Class implements the reader for the android OBB object for the glideApp
 *
 * @author Eng Chong Meng
 */
public class OBBFile
{
    public static final int mainVersion = 104000;
    public static final long mFileSize = 169701052L;
    public static final int patchVersion = -1;

    private final Context mContext;
    private final String path;

    private static ZipResourceFile obbZip;

    public OBBFile(Context context, String path)
    {
        this.path = path;
        mContext = context;

        if (obbZip == null) {
            try {
                obbZip = APKExpansionSupport.getAPKExpansionZipFile(mContext, mainVersion, patchVersion);
            } catch (IOException e) {
                Timber.w("getAPKExpansionZipFile exception: %s", e.getMessage());
            }
        }
    }

    public InputStream getInputStream() throws IOException
    {
        return (obbZip == null) ? null : obbZip.getInputStream(path);
    }

    public String getPath()
    {
        return path;
    }
}
