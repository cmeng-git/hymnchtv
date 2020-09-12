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

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Class load the hymn lyrics into the given image view
 *
 * @author Eng Chong Meng
 */
@GlideModule
public class MyGlideApp extends AppGlideModule
{
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

    public static void loadImageLs(ImageView imageView, int resId)
    {
        Context ctx = HymnsApp.getGlobalContext();

        GlideApp.with(ctx)
                .load(resId)
                .override(HymnsApp.screenWidth, HymnsApp.screenHeight)
                .error(R.drawable.phrase)
                .into(imageView);
    }
}
