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

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.utils.ZoomTextView.STEP_SCALE_FACTOR;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zqc.opencc.android.lib.ChineseConverter;
import com.zqc.opencc.android.lib.ConversionType;

import org.cog.hymnchtv.glide.MyGlideApp;
import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
import org.cog.hymnchtv.utils.ChineseS2TSelection;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
import org.cog.hymnchtv.utils.ZoomTextView;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class displays the hymn lyrics content selected by user;
 * It is a part of the whole Hymn lyrics content UI display
 *
 * Note: The context menu needs to be created here, instead its parent, for it to be visible
 *
 * @author Eng Chong Meng
 */
public class ContentView extends Fragment implements ZoomTextView.ZoomTextListener, View.OnClickListener,
        View.OnLongClickListener, LyricsEnglishRecord.EnglishLyricsListener {
    public static String LYRICS_DB_SCORE = "lyrics_db_score/";
    public static String LYRICS_BB_SCORE = "lyrics_bb_score/";
    public static String LYRICS_XB_SCORE = "lyrics_xb_score/";
    public static String LYRICS_XG_SCORE = "lyrics_csr_score/";
    public static String LYRICS_ER_SCORE = "lyrics_er_score/";

    public static String LYRICS_DB_TEXT = "lyrics_db_text/";
    public static String LYRICS_BB_TEXT = "lyrics_bb_text/";
    public static String LYRICS_XB_TEXT = "lyrics_xb_text/";
    public static String LYRICS_XG_TEXT = "lyrics_csr_text/";
    public static String LYRICS_ER_TEXT = "lyrics_er_text/";

    public static String LYRICS_TOC = "lyrics_toc/";

    public final static String LYRICS_TYPE = "lyricsType";
    public final static String LYRICS_INDEX = "lyricsIndex";

    public static final String EXTR_KEY_HAS_CHANGES = "hasChanges";
    public final static String PREF_SCORE_COLOR = "ScoreColor";
    public static final String PREF_CONVERSION_TYPE = "ConversionType";
    public static final String PREF_SIMPLIFY = "LyricsSimplify";
    public static final String PREF_LYRICS_SCALE_P = "LyricsScaleP";
    public static final String PREF_LYRICS_SCALE_L = "LyricsScaleL";
    public static final String PREF_LYRICS_ENGLISH_SCALE_P = "LyricsScaleEP";
    public static final String PREF_LYRICS_ENGLISH_SCALE_L = "LyricsScaleEL";
    public ContentHandler mContext;
    private LyricsEnglishRecord mLyricsEnglishRecord;
    private ConversionType mConversionType = ConversionType.S2T;

    private Button btn_english;
    private View mConvertView;
    private View lyricsView;
    private ZoomTextView lyricsSimplify;
    private ZoomTextView lyricsTraditional;
    private WebView lyricsEnglish;

    private ImageView mContentView = null;
    private Integer mHymnNoEng = null;
    private boolean isSimplify;
    private boolean isErGe;
    private boolean mLyricsLoaded = false;
    private boolean hasEnglishLyrics = false;

    private static final float[] mColorRange = new float[]{0, -0.9f, -0.8f, -0.7f};
    private static int mScoreColor = 0;
    private static ColorFilter mMatrix = null;

    private static float lyricsScaleP;
    private static float lyricsScaleL;
    private static float lyricsScaleEP;
    private static float lyricsScaleEL;

    private String mResPrefix;
    private int[] mHymnScoreInfo;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

// Need this to prevent crash on rotation if there are other constructors implementation
// public ContentView() { }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContext = (ContentHandler) context;

        mLyricsEnglishRecord = LyricsEnglishRecord.getInstanceFor(mContext);
        mLyricsEnglishRecord.registerLyricsListener(this);

        mSharedPref = mContext.getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mConvertView = inflater.inflate(R.layout.content_lyrics, container, false);
        mContentView = mConvertView.findViewById(R.id.contentView);

        Button btn_ts = mConvertView.findViewById(R.id.button_ts);
        btn_ts.setOnClickListener(this);
        btn_ts.setOnLongClickListener(this);

        btn_english = mConvertView.findViewById(R.id.button_english);
        btn_english.setOnClickListener(this);
        btn_english.setOnLongClickListener(this);

        lyricsView = mConvertView.findViewById(R.id.lyricsView);
        lyricsSimplify = mConvertView.findViewById(R.id.lyrics_simplified);
        lyricsSimplify.registerZoomTextListener(this);
        lyricsTraditional = mConvertView.findViewById(R.id.lyrics_traditional);

        lyricsEnglish = mConvertView.findViewById(R.id.lyrics_english);
        lyricsEnglish.setBackgroundColor(Color.TRANSPARENT);

        lyricsScaleP = mSharedPref.getFloat(PREF_LYRICS_SCALE_P, 1.0f);
        lyricsScaleL = mSharedPref.getFloat(PREF_LYRICS_SCALE_L, 1.0f);
        lyricsScaleEP = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_P, 1.0f);
        lyricsScaleEL = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_L, 1.0f);

        isSimplify = mSharedPref.getBoolean(PREF_SIMPLIFY, true);
        mConversionType = ConversionType.valueOf(mSharedPref.getString(PREF_CONVERSION_TYPE, ConversionType.S2T.toString()));

        mScoreColor = mSharedPref.getInt(PREF_SCORE_COLOR, 0);
        mMatrix = (mScoreColor == 0) ? null : getColorMatrix(mColorRange[mScoreColor]);

        mLyricsLoaded = false;
        hasEnglishLyrics = false;

        toggleLyricsView();

        Bundle bundle = getArguments();
        if (bundle != null) {
            String lyricsType = getArguments().getString(LYRICS_TYPE);
            int lyricsIndex = getArguments().getInt(LYRICS_INDEX);

            if (!TextUtils.isEmpty(lyricsType)) {
                updateHymnContent(lyricsType, lyricsIndex);
            }
        }
        return mConvertView;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerForContextMenu(lyricsView);

        // get the corresponding English lyrics# or null if none
        mHymnNoEng = mContext.getHymnNoEng();
        btn_english.setVisibility((mHymnNoEng != null) ? View.VISIBLE : View.GONE);
        if (mContext.mAutoEnglish) {
            hasEnglishLyrics = true;
            toggleLyricsView();
        }
    }

    @Override
    public void onPause() {
        unregisterForContextMenu(lyricsView);
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NotNull ContextMenu menu, @NotNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mContext.getMenuInflater().inflate(R.menu.menu_content, menu);

        // Hide "英文歌词" if no associated English lyrics
        menu.findItem(R.id.lyrcsEnglish).setVisible(mHymnNoEng != null);
        menu.findItem(R.id.lyrcsEnglishDelete).setVisible(mHymnNoEng != null && hasEnglishLyrics);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_ts:
                if (!hasEnglishLyrics) {
                    isSimplify = !isSimplify;
                    mEditor.putBoolean(PREF_SIMPLIFY, isSimplify);
                    mEditor.apply();
                }
                else {
                    hasEnglishLyrics = false;
                }
                toggleLyricsView();
                break;

            case R.id.button_english:
                hasEnglishLyrics = !hasEnglishLyrics;
                toggleLyricsView();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.button_ts:
                mStartForResult.launch(new Intent(mContext, ChineseS2TSelection.class));
                return true;

            case R.id.button_english:
                mContext.initWebView(ContentHandler.UrlType.englishLyrics);
                return true;
        }
        return false;
    }

    /**
     * The lyrics png/jpg file has the following formats: HYMN_ER, HYMN_XB, HYMN_XG, HYMN_BB, HYMN_DB
     * i.e. er, xb, csr, bb, db followed by the hymn number, a, b, c etc for more than one page;
     * The files are stored in asset respective sub-dir e.g. LYRICS_XB_SCORE
     * The content view can support up to 5 pages for user vertical scrolls
     *
     * @param hymnType see below cases
     * @param hymnIndex hymn index provided by the page adapter when user scroll
     */
    private void updateHymnContent(String hymnType, int hymnIndex) {
        String resFName;
        mHymnScoreInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(hymnType, hymnIndex);

        // Chinese lyrics#
        int lyricsNo = mHymnScoreInfo[0];
        isErGe = HYMN_ER.equals(hymnType);

        switch (hymnType) {
            case HYMN_DB:
                mResPrefix = LYRICS_DB_SCORE + "db" + lyricsNo;
                resFName = LYRICS_DB_TEXT + "db" + lyricsNo + ".txt";
                break;

            case HYMN_BB:
                mResPrefix = LYRICS_BB_SCORE + "bb" + lyricsNo;
                resFName = LYRICS_BB_TEXT + "bb" + lyricsNo + ".txt";
                break;

            case HYMN_XG:
                mResPrefix = LYRICS_XG_SCORE + "csr" + lyricsNo;
                resFName = LYRICS_XG_TEXT + "csr" + lyricsNo + ".txt";
                break;

            case HYMN_XB:
                mResPrefix = LYRICS_XB_SCORE + "xb" + lyricsNo;
                resFName = LYRICS_XB_TEXT + "xb" + lyricsNo + ".txt";
                break;

            case HYMN_ER:
                mResPrefix = LYRICS_ER_SCORE + lyricsNo;
                resFName = LYRICS_ER_TEXT + "er" + lyricsNo + ".txt";
                break;

            default:
                Timber.e("Unsupported content type: %s", hymnType);
                return;
        }

        // Show Hymn Lyric Scores for the selected hymnNo
        showLyricsScore(mResPrefix, mHymnScoreInfo);

        // Show Hymn Lyric Text for the selected hymnNo
        if (!TextUtils.isEmpty(resFName)) {
            showLyricsChText(resFName);
        }
    }

    /**
     * <a href="https://medium.com/mobile-app-development-publication/android-image-color-change-with-colormatrix-e927d7fb6eb4">
     * Android Image Color Change With ColorMatrix</a>
     *
     * @see ColorMatrix();
     */
    // Invert but make background color closer to theme dark with multiplier = -0.9f
    private static ColorFilter getColorMatrix(float multiplier) {
        return new ColorMatrixColorFilter(
                new float[]{
                        multiplier, .0f, .0f, .0f, 255.0f,  // red
                        .0f, multiplier, .0f, .0f, 255.0f,  // green
                        .0f, .0f, multiplier, .0f, 255.0f,  // blue
                        .0f, .0f, .0f, 1.0f, .0f            // alpha
                }
        );
    }

    public void toggleScoreColor() {
        mScoreColor = ++mScoreColor % mColorRange.length;
        mMatrix = (mScoreColor == 0) ? null : getColorMatrix(mColorRange[mScoreColor]);
        mEditor.putInt(PREF_SCORE_COLOR, mScoreColor);
        mEditor.apply();

        // Update Hymn Lyric Scores for the selected hymnNo with the new color inversion
        showLyricsScore(mResPrefix, mHymnScoreInfo);
    }

    /**
     * Display the selected Hymn Lyric Scores. Scores with multi-pages have suffixed with a, b, c and d.
     * i.e. support a total of 5 pages maximum.
     *
     * @param resPrefix The selected Hymn Lyric scores fileName prefix
     * @param hymnScoreInfo Contain info for the hymnNo and number of pages of the selected Lyric Scores
     */
    private void showLyricsScore(String resPrefix, int[] hymnScoreInfo) {
        int pages = hymnScoreInfo[1]; // The number of pages for the current hymn number
        ImageView contentView;
        Context ctx = getContext();

        mContentView.setColorFilter(mMatrix);
        String resName = resPrefix + ".png";
        // Uri resUri = Uri.fromFile(new File("//android_asset/", resName));
        // MyGlideApp.loadImage(ctx, mContentView, resUri);
        MyGlideApp.loadImage(ctx, mContentView, resName);

        if (pages > 1) {
            contentView = mConvertView.findViewById(R.id.contentView_a);
            contentView.setColorFilter(mMatrix);

            resName = resPrefix + "a.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(ctx, contentView, resName);
        }
        else {
            return;
        }

        if (pages > 2) {
            contentView = mConvertView.findViewById(R.id.contentView_b);
            contentView.setColorFilter(mMatrix);

            resName = resPrefix + "b.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(ctx, contentView, resName);
        }
        else {
            return;
        }

        if (pages > 3) {
            contentView = mConvertView.findViewById(R.id.contentView_c);
            contentView.setColorFilter(mMatrix);

            resName = resPrefix + "c.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(ctx, contentView, resName);
        }
        else {
            return;
        }

        if (pages > 4) {
            contentView.setColorFilter(mMatrix);

            resName = resPrefix + "d.png";
            // resUri = Uri.fromFile(new File("//android_asset/", resName));
            MyGlideApp.loadImage(ctx, contentView, resName);
        }
    }

    /**
     * Display the selected hymn lyrics text
     *
     * @param resFName Lyrics text resource fileName
     */
    private void showLyricsChText(String resFName) {
        setLyricsTextScale();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open(resFName)));
            StringBuilder lyrics = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                lyrics.append(line);
                lyrics.append('\n');
            }
            lyricsSimplify.setText(lyrics);
            lyricsTraditional.setText(ChineseConverter.convert(lyrics.toString(), mConversionType, mContext));

        } catch (IOException e) {
            Timber.w("Error reading file: %s", resFName);
        }
    }

    /**
     * Update the lyrics text view default size and the stored scale factor
     * Also being used onConfiguration change
     */
    public void setLyricsTextScale() {
        // English lyrics text size in webSettings
        final WebSettings webSettings = lyricsEnglish.getSettings();

        if (HymnsApp.isPortrait) {
            lyricsSimplify.scaleTextSize(20, lyricsScaleP);
            lyricsTraditional.scaleTextSize(20, lyricsScaleP);
            webSettings.setDefaultFontSize((int) (18 * lyricsScaleEP));
        }
        else {
            lyricsSimplify.scaleTextSize(35, lyricsScaleL);
            lyricsTraditional.scaleTextSize(35, lyricsScaleL);
            webSettings.setDefaultFontSize((int) (26 * lyricsScaleEL));
        }
    }

    /**
     * Increase or decrease the lyrics text scale factor
     *
     * @param stepInc true if size increment else decrement
     */
    public void setLyricsTextSize(boolean stepInc) {
        if (lyricsEnglish.getVisibility() == View.VISIBLE) {
            setLyricsEnglishTS(stepInc);
        }
        else {
            lyricsSimplify.onTextSizeChange(stepInc);
            lyricsTraditional.onTextSizeChange(stepInc);
        }
    }

    // Handler for english lyrics textSize changes
    private void setLyricsEnglishTS(boolean stepInc) {
        float tmpScale = stepInc ? STEP_SCALE_FACTOR : -STEP_SCALE_FACTOR;

        if (HymnsApp.isPortrait) {
            lyricsScaleEP += tmpScale;
            mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_P, lyricsScaleEP);
        }
        else {
            lyricsScaleEL += tmpScale;
            mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_L, lyricsScaleEL);
        }
        mEditor.apply();
        setLyricsTextScale();
    }

    /**
     * Save the user selected scale factory to preference settings
     *
     * @param scaleFactor scale factor
     */
    @Override
    public void updateTextScale(Float scaleFactor) {
        if (HymnsApp.isPortrait) {
            lyricsScaleP = scaleFactor;
            mEditor.putFloat(PREF_LYRICS_SCALE_P, scaleFactor);
        }
        else {
            lyricsScaleL = scaleFactor;
            mEditor.putFloat(PREF_LYRICS_SCALE_L, scaleFactor);
        }
        mEditor.apply();
    }

    private void toggleLyricsView() {
        lyricsTraditional.setVisibility(View.GONE);
        lyricsSimplify.setVisibility(View.GONE);
        lyricsEnglish.setVisibility(View.GONE);

        if (hasEnglishLyrics) {
            lyricsEnglish.setVisibility(View.VISIBLE);
            Timber.d("Lyrics loaded: %s", mLyricsLoaded);
            if (!mLyricsLoaded) {
                showLyricsEnglish(LyricsEnglishRecord
                        .toHtml("<h3>" + getResources().getString(R.string.download_wait) + "</h3>"));
            }
            mLyricsEnglishRecord.fetchLyrics(mHymnNoEng, isErGe);
        }
        else {
            if (isSimplify) {
                lyricsSimplify.setVisibility(View.VISIBLE);
            }
            else {
                lyricsTraditional.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void showLyricsEnglish(final String lyrics) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (lyrics != null) {
                mLyricsLoaded = true;
                lyricsEnglish.loadDataWithBaseURL(null, lyrics, "text/html", "utf8", null);
            }
            else {
                lyricsEnglish.loadUrl(LyricsEnglishRecord.HYMNAL_LINK_MAIN + mHymnNoEng);
            }
        });
    }

    /**
     * standard ActivityResultContract#StartActivityForResult
     */
    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();
            if (intent != null) {
                boolean hasChanges = intent.getBooleanExtra(EXTR_KEY_HAS_CHANGES, false);
                if (!isSimplify && hasChanges) {
                    mConversionType = ConversionType.valueOf(mSharedPref.getString(PREF_CONVERSION_TYPE, ConversionType.S2T.toString()));
                    toggleLyricsView();
                }
            }
        }
    });
}
