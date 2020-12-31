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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.yalantis.ucrop.UCrop;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;

import java.io.File;

import timber.log.Timber;

import static org.cog.hymnchtv.MainActivity.PREF_BACKGROUND;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MainActivity.PREF_WALLPAPER;
import static org.cog.hymnchtv.persistance.FileBackend.TMP;

/**
 * The class allows user to use own defined wallpaper for the main UI.
 * Embedded UCrop class to ease user zoom/crop etc
 * The max allowed image is 1/2 of screen resolution
 *
 * @author Eng Chong Meng
 */
public class WallPaperUtil extends FragmentActivity implements View.OnClickListener
{
    public static final String DIR_WALLPAPER = "wallpaper/";

    private static final int CROP_MAX_SIZE_WIDTH = HymnsApp.screenWidth / 2;
    private static final int CROP_MAX_SIZE_HEIGHT = HymnsApp.screenWidth / 2;

    private ImageView wallpaperView;

    private File inFile;
    private File wpFile;
    private boolean hasChanges = false;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallpaper_editor);

        ActivityResultLauncher<String> mGetContent = getWallpaperContent();
        findViewById(R.id.btnRenew).setOnClickListener(view -> mGetContent.launch("image/*"));

        wallpaperView = findViewById(R.id.wallpaper);
        SharedPreferences sPref = getSharedPreferences(PREF_SETTINGS, 0);
        String fileName = sPref.getString(PREF_WALLPAPER, null);
        wpFile = FileBackend.getHymnchtvStore(DIR_WALLPAPER + fileName, false);
        initWallpaperView();

        wallpaperView.setOnClickListener(view -> {
            if (wpFile.exists()) {
                uCropWallpaper(Uri.fromFile(wpFile));
            }
        });

        findViewById(R.id.btnCancel).setOnClickListener(this);
        findViewById(R.id.btnOk).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.btnOk:
                updateWallPaperPref();
                break;

            case R.id.btnCancel:
                checkUnsavedChanges();
                break;
        }
    }

    /**
     * A contract specifying that an activity can be called with an input of type I
     * and produce an output of type O
     *
     * @return an instant of ActivityResultLauncher<String>
     * @see ActivityResultCaller
     */
    private ActivityResultLauncher<String> getWallpaperContent()
    {
        return registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) {
                Timber.d("No image data selected: null!");
            }
            else {
                uCropWallpaper(uri);
            }
        });
    }

    /**
     * Call to UCrop to edit the given uri
     *
     * @param uri image file uri
     */
    private void uCropWallpaper(Uri uri)
    {
        // get the proper filename and to allow crop itself
        inFile = new File(FilePathHelper.getFilePath(this, uri));
        String fileName = inFile.getName();
        File outFile = new File(FileBackend.getHymnchtvStore(DIR_WALLPAPER, true), fileName);

        UCrop.of(Uri.fromFile(inFile), Uri.fromFile(outFile))
                .withAspectRatio(9, 16)
                .withMaxResultSize(CROP_MAX_SIZE_WIDTH, CROP_MAX_SIZE_HEIGHT)
                .start(WallPaperUtil.this);
    }

    /**
     * Method handles callbacks from UCrop with return {@link Intent}
     *
     * @param requestCode the request code
     * @param resultCode the result code
     * @param data the source {@link Intent} that returns the result
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        // Cleanup tmp input file
        if (inFile != null && inFile.exists() && inFile.getPath().contains(TMP)) {
            inFile.delete();
        }

        switch (requestCode) {
            case UCrop.REQUEST_CROP:
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri == null) {
                    setResult(RESULT_CANCELED);
                }
                else {
                    wpFile = new File(resultUri.getPath());
                    initWallpaperView();
                    hasChanges = true;
                }
                break;

            case UCrop.RESULT_ERROR:
                final Throwable cropError = UCrop.getError(data);
                String errMsg = "Image crop error: ";
                if (cropError != null)
                    errMsg += cropError.getMessage();
                Timber.e("%s", errMsg);
                showError();
                break;
        }
    }

    /**
     * Save the user defined image file to Preference Settings.
     */
    private void updateWallPaperPref()
    {
        if (hasChanges && (wpFile != null) && wpFile.exists() && wpFile.length() != 0) {
            hasChanges = false;

            SharedPreferences sdPref = getSharedPreferences(PREF_SETTINGS, 0);
            SharedPreferences.Editor editor = sdPref.edit();
            editor.putString(PREF_WALLPAPER, wpFile.getName());
            editor.putInt(PREF_BACKGROUND, -1);
            editor.apply();

            Intent _result = new Intent();
            _result.setData(Uri.fromFile(wpFile));
            setResult(RESULT_OK, _result);
        }
        finish();
    }

    /**
     * Update the wallpaper preview with the user edited image file
     */
    private void initWallpaperView()
    {
        if (wpFile.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(wpFile.getAbsolutePath(), options);
            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(imageWidth, imageHeight);
            parms.gravity = Gravity.CENTER;
            wallpaperView.setLayoutParams(parms);

            Drawable drawable = Drawable.createFromPath(wpFile.getAbsolutePath());
            wallpaperView.setImageDrawable(drawable);
        }
    }

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void checkUnsavedChanges()
    {
        if (hasChanges) {
            DialogActivity.showConfirmDialog(this,
                    R.string.gui_to_be_added,
                    R.string.gui_unsaved_changes,
                    R.string.gui_add_renew, new DialogActivity.DialogListener()
                    {
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            updateWallPaperPref();
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog)
                        {
                            finish();
                        }
                    });
        }
        else {
            finish();
        }
    }

    private void showError()
    {
        HymnsApp.showToastMessage("wallpaer image  selection error");
    }
}
