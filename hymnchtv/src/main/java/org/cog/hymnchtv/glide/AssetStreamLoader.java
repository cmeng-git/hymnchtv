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

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

/**
 * Class extends the ModelLoader and ModelLoaderFactory for the glideApp
 * for loading generate the inputStream from the Play Access Delivery file
 *
 * @author Eng Chong Meng
 */
public final class AssetStreamLoader implements ModelLoader<AssetFile, InputStream>
{
    @Override
    public LoadData<InputStream> buildLoadData(AssetFile model, int width, int height, @NonNull Options options)
    {
        Key diskCacheKey = new ObjectKey(model.getPath());
        return new LoadData<>(diskCacheKey, new AssetDataFetcher(model));
    }

    @Override
    public boolean handles(@NonNull AssetFile model)
    {
        return true;
    }

    /**
     * Class extends ModelLoaderFactory for prepend in GlideApp
     *
     * @author Eng Chong Meng
     */
    public static final class Factory implements ModelLoaderFactory<AssetFile, InputStream>
    {
        @NonNull
        @Override
        public ModelLoader<AssetFile, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory)
        {
            return new AssetStreamLoader();
        }

        @Override
        public void teardown()
        {
        }
    }
}
