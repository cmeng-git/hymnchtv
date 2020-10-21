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
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import org.cog.hymnchtv.*;

import java.io.InputStream;

/**
 * Class load the hymn lyrics into the given image view
 *
 * @author Eng Chong Meng
 */
@GlideModule
public class MyGlideApp extends AppGlideModule
{
//    @Override
//    public void applyOptions(Context context, GlideBuilder builder)
//    {
//        builder.setMemoryCache(new LruResourceCache(10 * 1024 * 1024));
//    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry)
    {
        registry.append(OBBFile.class, InputStream.class, new OBBStreamLoader.Factory());
    }

    /**
     * Display ResId as image view; Must use ResId instead of Uri, else display incorrect png
     *
     * @param imageView image preview holder
     * @param resId the image file resId
     */
    public static void loadImage(ImageView imageView, int resId)
    {
        Context ctx = HymnsApp.getGlobalContext();
        GlideApp.with(ctx)
                .load(resId)
                .override(HymnsApp.screenWidth, HymnsApp.screenHeight)
                .error(R.drawable.phrase)
                .into(imageView);
    }

    public static void loadImage(ImageView imageView, Uri uri)
    {
        Context ctx = HymnsApp.getGlobalContext();
        GlideApp.with(ctx)
                .load(uri)
                .override(HymnsApp.screenWidth, HymnsApp.screenHeight)
                .error(R.drawable.phrase)
                .into(imageView);
    }

    public static void loadImage(ImageView imageView, String path)
    {
        Context ctx = HymnsApp.getGlobalContext();
        GlideApp.with(ctx)
                .load(new OBBFile(ctx, path))
                .override(HymnsApp.screenWidth, HymnsApp.screenHeight)
                .error(R.drawable.phrase)
                .into(imageView);
    }
}
