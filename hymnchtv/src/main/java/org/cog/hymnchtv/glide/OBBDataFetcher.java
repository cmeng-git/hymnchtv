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

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Class extends the DataFetcher for the glideApp
 *
 * @author Eng Chong Meng
 */
public class OBBDataFetcher implements DataFetcher<InputStream>
{
    private InputStream obbFileStream;

    private OBBFile obbFile;

    public OBBDataFetcher(OBBFile model)
    {
        obbFile = model;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback)
    {
        try {
            obbFileStream = obbFile.getInputStream();
            callback.onDataReady(obbFileStream);
        } catch (Exception e) {
            callback.onLoadFailed(e);
            Timber.w("obbFileStream Exception: %s", e.getMessage());
        }
    }

    @Override
    public void cleanup()
    {
        try {
            if (obbFileStream != null) {
                obbFileStream.close();
            }
        } catch (IOException e) {
            Timber.w("OBBDataFetcher Cannot clean up after stream %s", e.getMessage());
        }
    }

    @Override
    public void cancel()
    {
        // do nothing
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass()
    {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource()
    {
        return DataSource.LOCAL;
    }
}
