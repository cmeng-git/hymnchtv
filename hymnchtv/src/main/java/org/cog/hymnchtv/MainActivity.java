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

import static org.cog.hymnchtv.HymnToc.TOC_ENGLISH;
import static org.cog.hymnchtv.HymnToc.hymnTocPage;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_DUMMY;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.WallPaperUtil.DIR_WALLPAPER;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.zqc.opencc.android.lib.ChineseConverter;
import com.zqc.opencc.android.lib.ConversionType;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.hymnhistory.HistoryRecord;
import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.DialogActivity;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.cog.hymnchtv.utils.LocaleHelper;
import org.cog.hymnchtv.utils.MySwipeListAdapter;
import org.cog.hymnchtv.utils.ThemeHelper;
import org.cog.hymnchtv.utils.ThemeHelper.Theme;
import org.cog.hymnchtv.utils.TouchListener;
import org.cog.hymnchtv.utils.WallPaperUtil;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

/**
 * MainActivity: The hymnchtv app main user interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener, LifecycleEventObserver,
        ActivityCompat.OnRequestPermissionsResultCallback {
    public static String HYMNCHTV_FAQ = "https://cmeng-git.github.io/hymnchtv/faq.html";
    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

    public static final String ATTR_HYMN_TYPE = "hymn_type";
    public static final String ATTR_HYMN_NUMBER = "hymn_number";
    public static final String ATTR_MEDIA_URI = "media_uri";

    public static final String ATTR_SEARCH = "search";
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_AUTO_PLAY = "autoPlay";
    public static final String ATTR_ENGLISH_NO = "englishNo";

    public static final String HYMN_DB = "hymn_db";
    public static final String HYMN_BB = "hymn_bb";
    public static final String HYMN_XG = "hymn_xg";
    public static final String HYMN_XB = "hymn_xb";
    public static final String HYMN_ER = "hymn_er";

    public static final String PREF_MENU_SHOW = "MenuShow";
    public static final String PREF_SETTINGS = "Settings";
    public static final String PREF_BACKGROUND = "Background";
    public static final String PREF_TEXT_COLOR = "TextColor";
    public static final String PREF_TEXT_SIZE = "TextSize";
    public static final String PREF_THEME = "Theme";
    public static final String PREF_LOCALE = "Locale";
    public static final String PREF_WALLPAPER = "WallPaper";

    public static final String PREF_MEDIA_HYMN = "MediaHymn";
    private static final String mTocFile = "lyrics_toc/toc_all_eng2ch.txt";
    private static final int FONT_SIZE_DEFAULT = 35;

    private static String mHymnType = HYMN_DB;
    private static int mHymnNo = -1;

    public static boolean mHasUpdate = false;
    /**
     * Indicate if aTalk is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    private Button btn_n0;
    private Button btn_n1;
    private Button btn_n2;
    private Button btn_n3;
    private Button btn_n4;
    private Button btn_n5;
    private Button btn_n6;
    private Button btn_n7;
    private Button btn_n8;
    private Button btn_n9;

    private Button btn_fu;
    private Button btn_del;

    private Button btn_db;
    private Button btn_bb;
    private Button btn_xg;
    private Button btn_xb;
    private Button btn_er;
    private Button btn_search;
    private Button btn_update;
    private Button btn_english;

    private Spinner mTocSpinner;
    private MySwipeListAdapter<HistoryRecord> mHistoryAdapter;
    private ListView mHistoryListView;
    private TextView mTocSpinnerItem;
    private TextView mEntry;
    private EditText tv_Search;

    private LinearLayout background;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

    private boolean autoClear = false;
    private boolean isFu = false;
    // Indicate that a TOC item has been selected
    private boolean isToc = false;

    private int mFontSize = FONT_SIZE_DEFAULT;
    private int mFsDelta = FONT_SIZE_DEFAULT - 10;

    // Default to a valid COLOR just in case (see initUserSettings()).
    private int mFontColor = Color.BLACK;

    private String sNumber = "";
    private String mTocPage;

    private static MainActivity mInstance;

    // Available background wall papers
    public static int[] bgResId = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
            R.drawable.bg20, R.drawable.bg21, R.drawable.bg22, R.drawable.bg23, R.drawable.bg24, R.drawable.bg25};

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onCreate(Bundle savedInstanceState) {
        mInstance = this;
        // Must setTheme() before super.onCreate(), otherwise not working
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();

        String theme = mSharedPref.getString(PREF_THEME, Theme.DARK.toString());
        setAppTheme(theme, false);

        super.onCreate(savedInstanceState);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        setContentView(R.layout.main);
        registerForContextMenu(findViewById(R.id.viewMain));

        mHistoryListView = findViewById(R.id.historyListView);
        mHistoryListView.setVisibility(View.GONE);

        initButton();
        initUserSettings();
        // Request all the permissions required by Hymnchtv; only valid if user does not manually disallow it.
        PermissionUtils.checkHymnPermissionAndRequest(this);

        // allow 15 seconds for first launch login to complete before showing history log if the activity is still active
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            runOnUiThread(() -> new Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    cl.getLogDialog().show();
                }
            }, 15000));
            /*
             * Disable importUrlAssetFile for debug version on start; rely on updateServiceImpl instead.
             * Likely the DB has already been updated when user is prompt to update apk.
             * See MediaConfig#URL_IMPORT_VERSION value setting.
             */
            // MediaConfig.importUrlAssetFile();
        }

        // 大本诗歌
        btn_db.setOnClickListener(v -> onHymnButtonClicked(HYMN_DB));
        // 补充本
        btn_bb.setOnClickListener(v -> onHymnButtonClicked(HYMN_BB));
        // 新詩歌本
        btn_xg.setOnClickListener(v -> onHymnButtonClicked(HYMN_XG));
        // 新歌颂咏
        btn_xb.setOnClickListener(v -> onHymnButtonClicked(HYMN_XB));
        // 儿童诗歌
        btn_er.setOnClickListener(v -> onHymnButtonClicked(HYMN_ER));

        // Numeric number entry handlers for 0~9
        btn_n0.setOnClickListener(this::onNumberClick);
        btn_n1.setOnClickListener(this::onNumberClick);
        btn_n2.setOnClickListener(this::onNumberClick);
        btn_n3.setOnClickListener(this::onNumberClick);
        btn_n4.setOnClickListener(this::onNumberClick);
        btn_n5.setOnClickListener(this::onNumberClick);
        btn_n6.setOnClickListener(this::onNumberClick);
        btn_n7.setOnClickListener(this::onNumberClick);
        btn_n8.setOnClickListener(this::onNumberClick);
        btn_n9.setOnClickListener(this::onNumberClick);

        btn_fu.setOnClickListener(v -> {
            isFu = true;
            sNumber = "";
            autoClear = false;
            onNumberClick(v);
        });

        btn_del.setOnClickListener(v -> {
            sNumber = "";
            mEntry.setText(sNumber);
            isToc = false;
            isFu = false;
        });

        btn_search.setOnClickListener(v -> {
            String sValue = tv_Search.getText().toString();
            sValue = sValue.trim();
            if (TextUtils.isEmpty(sValue)) {
                HymnsApp.showToastMessage(R.string.error_search_empty);
                return;
            }
            sValue = ChineseConverter.convert(sValue, ConversionType.T2S, this);
            tv_Search.setText(sValue);

            Intent intent = new Intent();
            intent.setClass(this, ContentSearch.class);
            Bundle bundle = new Bundle();
            bundle.putString(ATTR_SEARCH, sValue);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        // replace special hymns character; unable to enter from a standard keyboard.
        btn_search.setOnLongClickListener(v -> {
            String sValue = tv_Search.getText().toString();
            if (!TextUtils.isEmpty(sValue)) {
                sValue = ChineseConverter.convert(sValue, ConversionType.T2S, this);
                sValue = sValue.replaceAll("他", "祂");
                tv_Search.setText(sValue);
            }
            return true;
        });

        btn_update.setOnClickListener(v -> {
            new Thread() {
                @Override
                public void run() {
                    UpdateServiceImpl.getInstance().checkForUpdates();
                }
            }.start();
        });

        btn_english.setOnClickListener(v -> {
            showHymnFromEng(false);
        });
        btn_english.setOnLongClickListener(v -> {
            showHymnFromEng(true);
            return true;
        });
        handleIntent(getIntent());
    }

    /**
     * Called when new <tt>Intent</tt> is received(this <tt>Activity</tt> is launched in <tt>singleTask</tt> mode.
     *
     * @param intent new <tt>Intent</tt> data.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handle share intent to extract the share text content or URIS
     *
     * @param intent <tt>Activity</tt> <tt>Intent</tt>.
     */
    private void handleIntent(Intent intent) {
        super.onStart();
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        final String type = intent.getType();

        String mediaLink = null;
        if (Intent.ACTION_SEND.equals(action) && (type != null)) {
            if ("text/plain".equals(type)) {
                mediaLink = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else {
                mediaLink = getFile(intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && (type != null)) {
            final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null)
                mediaLink = getFile(uris.get(0));
        }

        if (mediaLink != null) {
            intent = new Intent(this, MediaConfig.class);
            Bundle bundle = new Bundle();
            bundle.putString(ATTR_MEDIA_URI, mediaLink);
            bundle.putString(ATTR_HYMN_TYPE, mHymnType);
            bundle.putInt(ATTR_HYMN_NUMBER, mHymnNo);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoClear = true;
        if (mHasUpdate) {
            btn_update.setVisibility(View.VISIBLE);
            // adding the color to be shown
            ObjectAnimator animator = ObjectAnimator.ofInt(btn_update, "textColor", Color.BLUE, Color.RED, Color.GREEN);

            // duration of one color
            animator.setDuration(15000);
            animator.setEvaluator(new ArgbEvaluator());
            // color will be show in reverse manner
            animator.setRepeatCount(Animation.REVERSE);
            // It will be repeated up to infinite time
            animator.setRepeatCount(Animation.INFINITE);
            animator.start();
        }
        else {
            btn_update.setVisibility(View.GONE);
        }
        configureToolBar();
    }

    /**
     * Configure the main activity action bar using
     * a. Android actionBar
     * b. Customer action_bar layout
     */
    private void configureToolBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_USE_LOGO
                    | ActionBar.DISPLAY_SHOW_TITLE);

            // ensure actual logo size is ~64x64
            actionBar.setLogo(R.drawable.logo_hymnchtv);
            actionBar.setTitle(R.string.app_title_main);
        }
    }

    public static MainActivity getInstance() {
        return mInstance;
    }

    // ========= LifecycleEventObserver implementations ======= //
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (Lifecycle.Event.ON_START == event) {
            isForeground = true;
            Timber.d("APP FOREGROUNDED");
        }
        else if (Lifecycle.Event.ON_STOP == event) {
            isForeground = false;
            Timber.d("APP BACKGROUNDED");
        }
    }

    /**
     * Returns true if the device is locked or screen turned off (in case password not set)
     */
    public static boolean isDeviceLocked() {
        boolean isLocked;

        // First we check the locked state
        KeyguardManager keyguardManager = (KeyguardManager) mInstance.getSystemService(Context.KEYGUARD_SERVICE);
        boolean inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode();

        if (inKeyguardRestrictedInputMode) {
            isLocked = true;
        }
        else {
            // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
            // so we need to check if screen on for this case
            PowerManager powerManager = (PowerManager) mInstance.getSystemService(Context.POWER_SERVICE);
            isLocked = !powerManager.isInteractive();
        }
        Timber.d("Android device is %s.", isLocked ? "locked" : "unlocked");
        return isLocked;
    }

    private String getFile(Uri uri) {
        File inFile = new File(FilePathHelper.getFilePath(this, uri));
        if (inFile.exists()) {
            return inFile.getPath();
        }
        else
            HymnsApp.showToastMessage(R.string.file_does_not_exist);

        return null;
    }

    // 目录 Spinner selector handler
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTocPage = hymnTocPage.get(position);
        isToc = (position > 0);

        if (isToc) {
            sNumber = "";
            // mEntry.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
            mEntry.setText(mTocPage);
        }
        else if (TextUtils.isEmpty(sNumber)) {
            // mEntry.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
            mEntry.setText("");
        }
        initTocSpinnerItem();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Routine to handle Fu and all numeric buttons click
     *
     * @param btnView fu and number buttons views
     */
    private void onNumberClick(View btnView) {
        // Auto clear sNumber to "" if this is first resume
        if (autoClear) {
            autoClear = false;
            isFu = false;
            sNumber = "";
        }

        sNumber = sNumber + ((Button) btnView).getText();
        mEntry.setText(sNumber);

        // Re-init TOC and Search Fields to default on hymn number entry
        if (sNumber.length() == 1) {
            mTocSpinner.setSelection(0);
            tv_Search.setText("");
        }
    }

    /**
     * Handler for user hymnType button clicks;
     * Show TOC is selected else the content for the hymnNo if valid
     *
     * @param hymnType the button being clicked
     */
    private void onHymnButtonClicked(String hymnType) {
        if (!isToc) {
            sNumber = mEntry.getText().toString();
            if (isFu) {
                sNumber = sNumber.substring(1);
            }
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }

            int hymnNo = Integer.parseInt(sNumber);
            if (isFu) {
                hymnNo += HYMN_DB_NO_MAX;
            }

            int nui = HymnNoValidate.validateHymnNo(hymnType, hymnNo, isFu);
            if (nui != -1) {
                mHymnType = hymnType;
                mHymnNo = hymnNo;
                showContent(this, hymnType, nui, false);
            }
            // Only clear the user entry hymnNo if user entry is Fu and HymnType is not HYMN_DB
            else if (isFu && !hymnType.equals(HYMN_DB)) {
                sNumber = "";
                mEntry.setText(sNumber);
                isFu = false;
            }
            else {
                autoClear = true;
            }
        }
        else {
            showHymnToc(hymnType);
        }
    }

    /**
     * Save the user selected hymn into the history table, and
     * Show the content of user selected hymnType and hymnNo
     *
     * @param ctx Context
     * @param hymnType lyrics content of the hymnType
     * @param hymnNo the content of hymnNo to display
     * @param autoPlay start media playback if true after the lyrics content is shown
     */
    public static void showContent(Context ctx, String hymnType, int hymnNo, boolean autoPlay, Integer... engNo) {
        // Save the user selection into history record
        boolean isFu = MediaRecord.isFu(hymnType, hymnNo);
        HistoryRecord historyRecord = new HistoryRecord(hymnType, hymnNo, isFu);
        DatabaseBackend.getInstance(ctx).storeHymnHistory(historyRecord);

        Intent intent = new Intent(ctx, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_HYMN_TYPE, hymnType);
        bundle.putInt(ATTR_HYMN_NUMBER, hymnNo);
        bundle.putBoolean(ATTR_AUTO_PLAY, autoPlay);
        bundle.putInt(ATTR_ENGLISH_NO, engNo.length == 0 ? -1 : engNo[0]);

        intent.putExtras(bundle);
        ctx.startActivity(intent);
    }

    /**
     * Show the TOC of the user selected hymn type
     *
     * @param hymnType Hymn Toc
     */
    private void showHymnToc(String hymnType) {
        // "英中对照" not implemented for HYMN_ER or HYMN_XB
        if (TOC_ENGLISH.equals(mTocPage) && (HYMN_ER.equals(hymnType) || HYMN_XB.equals(hymnType))) {
            HymnsApp.showToastMessage(R.string.en2ch_hymn_same);
            return;
        }

        Intent intent = new Intent(this, HymnToc.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_HYMN_TYPE, hymnType);
        bundle.putString(ATTR_PAGE, mTocPage);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Generate the expandable TOC list from the given tocFile sorted by the stroke or pinyin
     *
     * @param dbPage true to access the DB page instead of BB if exist.
     */
    private void showHymnFromEng(boolean dbPage) {
        if (isToc) {
            return;
        }

        sNumber = mEntry.getText().toString();
        if (TextUtils.isEmpty(sNumber)) {
            sNumber = "0";
        }
        else if (isFu) {
            sNumber = sNumber.substring(1);
        }

        int hymnEng = Integer.parseInt(sNumber);
        String sMatch = String.format(Locale.CHINA, "\\^ %04d:.+?", hymnEng);
        try {
            InputStream in2 = getResources().getAssets().open(mTocFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            int idx = 0; // trace next mList index for access to DB page.
            for (String item : mList) {
                idx++;
                if (item.matches(sMatch)) {
                    String hymnTN = item.replaceAll(".+? #(.+?)", "$1");
                    String hymnType = getHymnType(hymnTN);
                    int hymnNo = Integer.parseInt(hymnTN.substring(2));

                    // Set up to display DB page if dbPage and content exist.
                    if (dbPage && mList[idx].matches(sMatch)) {
                        hymnTN = mList[idx].replaceAll(".+? #(.+?)", "$1");
                        hymnType = getHymnType(hymnTN);
                        hymnNo = Integer.parseInt(hymnTN.substring(2));
                    }
                    showContent(this, hymnType, hymnNo, false, hymnEng);
                    return;
                }
            }
            // Pass in an non-existence HYMN_BB_DUMMY for chinese hymnNo
            showContent(this, HYMN_BB, HYMN_BB_DUMMY, false, hymnEng);
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
            HymnsApp.showToastMessage(R.string.in_development);
        }
    }

    private String getHymnType(String hymnTN) {
        if (hymnTN.startsWith("xg")) {
            return HYMN_XG;
        }
        else {
            return hymnTN.startsWith("db") ? HYMN_DB : HYMN_BB;
        }
    }

    /**
     * KeyEvent handler for KeyEvent.KEYCODE_BACK.
     * Remove the fragment view if triggers from a fragment else close app
     *
     * @param keyCode android keyCode
     * @param event KeyEvent
     *
     * @return handler state from android super
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mHistoryListView.getVisibility() == View.VISIBLE) {
                mEntry.setHint(R.string.hint_hymn_number_enter);
                mHistoryListView.setVisibility(View.GONE);
            }
            else if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                super.onBackPressed();
            }
            else {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Initial the option item menu
     *
     * @param menu the menu container
     *
     * @return true always
     */
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        /*
        // if (BuildConfig.DEBUG) {
        //     menu.findItem(R.id.sn_convert).setVisible(true);
        // }
         */
        return true;
    }

    /**
     * Pop up the main menu if user long press on the main UI
     *
     * @param menu menu
     * @param v view
     * @param menuInfo info
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
    }

    /**
     * Handler for the Context item clicked; use the same handlers as Option Item clicked
     *
     * @param item Option Item
     *
     * @return the handle state
     */
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Handler for the option item clicked
     *
     * @param item menu Item
     *
     * @return the handle state
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            // === Set app theme ===
            case R.id.themeDark:
                setAppTheme(Theme.DARK.toString(), true);
                return true;

            case R.id.themeLight:
                setAppTheme(Theme.LIGHT.toString(), true);
                return true;

            case R.id.localeChinese:
                setAppLocale(LocaleHelper.LocaleChinese);
                return true;

            case R.id.localeenglish:
                setAppLocale(LocaleHelper.LocaleEnglish);
                return true;

            // === Set font size ===
            case R.id.small:
                mFontSize = FONT_SIZE_DEFAULT - 5;
                setFontSize(mFontSize, true);
                return true;

            case R.id.middle:
                mFontSize = FONT_SIZE_DEFAULT;
                setFontSize(mFontSize, true);
                return true;

            case R.id.lager:
                mFontSize = FONT_SIZE_DEFAULT + 5;
                setFontSize(mFontSize, true);
                return true;

            case R.id.xlager:
                mFontSize = FONT_SIZE_DEFAULT + 10;
                setFontSize(mFontSize, true);
                return true;

            case R.id.inc:
                mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) + 2;
                setFontSize(mFontSize, true);
                return true;

            case R.id.dec:
                mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) - 2;
                setFontSize(mFontSize, true);
                return true;

            // === Set font color ===
            case R.id.red:
                setFontColor(Color.RED, true);
                return true;

            case R.id.blue:
                setFontColor(Color.BLUE, true);
                return true;

            case R.id.white:
                setFontColor(Color.WHITE, true);
                return true;

            case R.id.grey:
                setFontColor(Color.GRAY, true);
                return true;

            case R.id.cyan:
                setFontColor(Color.CYAN, true);
                return true;

            case R.id.yellow:
                setFontColor(Color.YELLOW, true);
                return true;

            case R.id.green:
                setFontColor(Color.GREEN, true);
                return true;

            case R.id.black:
                setFontColor(getResources().getColor(R.color.grey900), true);
                return true;

            // === Set background color ===
            case R.id.sbg1:
                setBgColor(0, R.drawable.bg0);
                return true;

            case R.id.sbg2:
                setBgColor(1, R.drawable.bg1);
                return true;

            case R.id.sbg3:
                setBgColor(2, R.drawable.bg2);
                return true;

            case R.id.sbg4:
                setBgColor(3, R.drawable.bg3);
                return true;

            case R.id.sbg5:
                setBgColor(4, R.drawable.bg4);
                return true;

            case R.id.sbg6:
                setBgColor(5, R.drawable.bg5);
                return true;

            case R.id.sbg7:
                setBgColor(6, R.drawable.bg20);
                return true;

            case R.id.sbg8:
                setBgColor(7, R.drawable.bg21);
                return true;

            case R.id.sbg9:
                setBgColor(8, R.drawable.bg22);
                return true;

            case R.id.sbg10:
                setBgColor(9, R.drawable.bg23);
                return true;

            case R.id.sbg11:
                setBgColor(10, R.drawable.bg24);
                return true;

            case R.id.sbg12:
                setBgColor(11, R.drawable.bg25);
                return true;

            case R.id.sbguser:
                mStartForResult.launch(new Intent(this, WallPaperUtil.class));
                return true;

            case R.id.sn_convert:
                // HymnIdx2NoConvert.validateIdx2NoConversion(HYMN_ER, HYMN_ER_INDEX_MAX);
                // HymnNo2IdxConvert.validateNo2IdxConversion(HYMN_DB, HYMN_DB_NO_TMAX);
                // Hymn2SnConvert.startConvert(); use for old to new file name conversion for 1.1.0 only
                return true;

            case R.id.media_config:
                intent = new Intent(this, MediaConfig.class);
                startActivity(intent);
                return true;

            case R.id.permission_request:
                onInfoButtonClicked();
                return true;

            case R.id.online_help:
                About.hymnUrlAccess(this, HYMNCHTV_FAQ);
                return true;

            case R.id.about:
                intent = new Intent(this, About.class);
                startActivity(intent);
                return true;

            case R.id.exit:
                LogUploadServiceImpl.purgeDebugLog();
                finishAndRemoveTask();
                System.exit(0);
                return true;

            case R.id.menutoggle:
            case R.id.alwayshow:
            case R.id.alwayhide:
            case R.id.bg:
            case R.id.fontColor:
            default:
                return false;
        }
    }

    /**
     * Bind all the button to its resource id
     */
    private void initButton() {
        background = findViewById(R.id.viewMain);
        mEntry = findViewById(R.id.tv_entry);

        mEntry.setOnClickListener(view -> {
            if (mHistoryListView.getVisibility() == View.GONE) {
                mEntry.setHint(R.string.hint_hymn_history);
                initHistoryList();
                mHistoryListView.setVisibility(View.VISIBLE);
            }
            else {
                mEntry.setHint(R.string.hint_hymn_number_enter);
                mHistoryListView.setVisibility(View.GONE);
            }
        });

        tv_Search = findViewById(R.id.tv_search);

        btn_n0 = findViewById(R.id.n0);
        btn_n1 = findViewById(R.id.n1);
        btn_n2 = findViewById(R.id.n2);
        btn_n3 = findViewById(R.id.n3);
        btn_n4 = findViewById(R.id.n4);
        btn_n5 = findViewById(R.id.n5);
        btn_n6 = findViewById(R.id.n6);
        btn_n7 = findViewById(R.id.n7);
        btn_n8 = findViewById(R.id.n8);
        btn_n9 = findViewById(R.id.n9);

        btn_fu = findViewById(R.id.n10);
        btn_del = findViewById(R.id.n11);

        btn_db = findViewById(R.id.bs_db);
        btn_bb = findViewById(R.id.bs_bb);
        btn_xg = findViewById(R.id.bs_xg);
        btn_xb = findViewById(R.id.bs_xb);
        btn_er = findViewById(R.id.bs_er);

        btn_search = findViewById(R.id.btn_search);
        btn_update = findViewById(R.id.btn_update);
        btn_english = findViewById(R.id.btn_english);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, hymnTocPage);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_radio);

        mTocSpinner = findViewById(R.id.spinner_toc);
        mTocSpinner.setAdapter(mAdapter);

        // Must allow to trigger onItemSelected() to show correct color on orientation change
        mTocSpinner.setOnItemSelectedListener(this);
        mTocSpinner.setSelection(0, false);
    }

    /**
     * Retrieve all the user preference settings and initialize the UI accordingly
     */
    private void initUserSettings() {
        setWallpaper();

        mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT);
        mFontColor = mSharedPref.getInt(PREF_TEXT_COLOR, getResources().getColor(R.color.grey900));
        initTocSpinnerItem();

        setFontSize(mFontSize, false);
        setFontColor(mFontColor, false);
    }

    // Must re-init mTocSpinnerItem reference here whenever a new item is selected
    private void initTocSpinnerItem() {
        mTocSpinnerItem = mTocSpinner.findViewById(R.id.textItem);
        mTocSpinnerItem.setGravity(Gravity.CENTER);
        mTocSpinnerItem.setTypeface(null, Typeface.BOLD);
        mTocSpinnerItem.setTextSize(mFsDelta);
        mTocSpinnerItem.setTextColor(mFontColor);
    }

    private void showHymn(HistoryRecord sRecord) {
        mEntry.setHint(R.string.hint_hymn_number_enter);
        mHistoryListView.setVisibility(View.GONE);

        sNumber = sRecord.getHymnNoFu();
        mEntry.setText(sNumber);
        isFu = sRecord.isFu();
        showContent(this, sRecord.getHymnType(), sRecord.getHymnNo(), false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initHistoryList() {
        List<HistoryRecord> historyRecords = mDB.getHistoryRecords();
        mHistoryAdapter = new MySwipeListAdapter<HistoryRecord>(this, historyRecords) {
            @Override
            public void remove(HistoryRecord sRecord) {
                int count = mDB.deleteHymnHistory(sRecord);
                if (count == 1) {
                    super.remove(sRecord);
                }
            }

            @Override
            public void open(@NonNull HistoryRecord sRecord) {
                showHymn(sRecord);
            }
        };
        mHistoryListView.setAdapter(mHistoryAdapter);
        mHistoryListView.setOnTouchListener(touchListener);
    }

    /**
     * TouchListener to support singleTap, doubleTap and longPress for the view for site visit
     */
    TouchListener touchListener = new TouchListener(HymnsApp.getGlobalContext()) {
        @Override
        public boolean onSingleTap(View v, int pos) {
            HistoryRecord sRecord = (HistoryRecord) ((ListView) v).getItemAtPosition(pos);
            if (sRecord != null) {
                showHymn(sRecord);
            }
            return true;
        }

        @Override
        public void onLongPress(View v, int pos) {
            HistoryRecord sRecord = (HistoryRecord) ((ListView) v).getItemAtPosition(pos);
            if (sRecord == null)
                return;

            DialogActivity.showConfirmDialog(MainActivity.this,
                    R.string.delete,
                    R.string.delete_history,
                    R.string.delete, new DialogActivity.DialogListener() {
                        public boolean onConfirmClicked(DialogActivity dialog) {
                            mHistoryAdapter.setSelectState(pos, false);
                            mHistoryAdapter.remove(sRecord);
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog) {
                        }
                    }, sRecord.toString());

        }

        @Override
        public boolean onSwipeRight(View v, int idx) {
            mHistoryAdapter.setSelectState(idx, false);
            int pos = idx - ((ListView) v).getFirstVisiblePosition();
            return showActionButton(pos, false);
        }

        @Override
        public boolean onSwipeLeft(View v, int idx) {
            mHistoryAdapter.setSelectState(idx, true);
            int pos = idx - ((ListView) v).getFirstVisiblePosition();
            return showActionButton(pos, true);
        }

        /**
         * Toggle between primary and alt view layout pending on action and current state
         *
         * @param pos the actual ListView item view location on display; no the same as HistoryRecord index
         * @param show true is to reveal the alt layout
         * @return true always
         */
        private boolean showActionButton(int pos, boolean show) {
            ViewSwitcher child = (ViewSwitcher) mHistoryListView.getChildAt(pos);
            if (child != null) {
                if ((child.getDisplayedChild() == 0) == show) {
                    child.showNext();
                }
            }
            return true;
        }
    };

    /**
     * Set app Theme as per sTheme. Need to restart MainActivity to reflect newly selected theme.
     *
     * @param sTheme Request Theme
     * @param prefChange true if use change theme
     */
    private void setAppTheme(String sTheme, boolean prefChange) {
        Timber.d("Set App Theme: %s => %s", prefChange, sTheme);
        if (prefChange) {
            mEditor.putString(PREF_THEME, sTheme);
            mEditor.apply();

            finish();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else {
            Theme theme = Theme.valueOf(sTheme);
            ThemeHelper.setTheme(this, theme);
        }
    }

    /**
     * Set HymnApp locale per user selected language.
     * Must commit preference change immediately before perform system restart.
     * Note: Restart MainActivity does not apply to HymnApp Application class.
     * HymnApp mBase Context can only be changed with Application restart.
     *
     * @param language Locale language
     */
    private void setAppLocale(String language) {
        mEditor.putString(PREF_LOCALE, language);
        mEditor.commit();

        doRestart();
    }

    // Need to restart whole app to make HymnApp Locale change working
    private void doRestart() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * Init the main UI wallpaper with one of the predefined image in drawable or user own if bgResId == -1
     */
    private void setWallpaper() {
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        int bgResId = mSharedPref.getInt(PREF_BACKGROUND, 5);
        if (bgResId != -1) {
            background.setBackgroundResource(MainActivity.bgResId[bgResId]);
        }
        else {
            String fileName = mSharedPref.getString(PREF_WALLPAPER, null);
            File wpFile = FileBackend.getHymnchtvStore(DIR_WALLPAPER + fileName, false);
            if ((wpFile != null) && wpFile.exists()) {
                Drawable drawable = Drawable.createFromPath(wpFile.getAbsolutePath());
                background.setBackground(drawable);
            }
        }
    }

    /**
     * Set the font size of the buttons' labels
     *
     * @param size button label font size
     * @param update true to update the preference settings
     */
    private void setFontSize(int size, boolean update) {
        if (update) {
            mEditor.putInt(PREF_TEXT_SIZE, size);
            mEditor.apply();
        }
        mFsDelta = size - 10;

        btn_n0.setTextSize(size);
        btn_n1.setTextSize(size);
        btn_n2.setTextSize(size);
        btn_n3.setTextSize(size);
        btn_n4.setTextSize(size);
        btn_n5.setTextSize(size);
        btn_n6.setTextSize(size);
        btn_n7.setTextSize(size);
        btn_n8.setTextSize(size);
        btn_n9.setTextSize(size);
        // mEntry.setTextSize(size);

        btn_fu.setTextSize(mFsDelta);
        btn_del.setTextSize(mFsDelta);

        btn_db.setTextSize(mFsDelta);
        btn_bb.setTextSize(mFsDelta);
        btn_xg.setTextSize(mFsDelta);
        btn_xb.setTextSize(mFsDelta);
        btn_er.setTextSize(mFsDelta);
        btn_english.setTextSize(mFsDelta);

        btn_search.setTextSize(mFsDelta);
        btn_update.setTextSize(mFsDelta);
        mTocSpinnerItem.setTextSize(mFsDelta);
    }

    /**
     * Set the color of the buttons' labels
     *
     * @param color text color
     * @param update true to update the preference settings
     */
    private void setFontColor(int color, boolean update) {
        mFontColor = color;
        if (update) {
            mEditor.putInt(PREF_TEXT_COLOR, color);
            mEditor.apply();
        }

        // set hint text alpha to 40%
        mEntry.setHintTextColor(color & 0x66FFFFFF);
        mEntry.setTextColor(color);

        tv_Search.setHintTextColor(color & 0x66FFFFFF);
        tv_Search.setTextColor(color);

        btn_n0.setTextColor(color);
        btn_n1.setTextColor(color);
        btn_n2.setTextColor(color);
        btn_n3.setTextColor(color);
        btn_n4.setTextColor(color);
        btn_n5.setTextColor(color);
        btn_n6.setTextColor(color);
        btn_n7.setTextColor(color);
        btn_n8.setTextColor(color);
        btn_n9.setTextColor(color);

        btn_fu.setTextColor(color);
        btn_del.setTextColor(color);

        btn_db.setTextColor(color);
        btn_bb.setTextColor(color);
        btn_xg.setTextColor(color);
        btn_xb.setTextColor(color);
        btn_er.setTextColor(color);

        btn_search.setTextColor(color);
        btn_update.setTextColor(color);
        btn_english.setTextColor(color);
        mTocSpinnerItem.setTextColor(color);
    }

    /**
     * Set the main UI background wall paper
     *
     * @param bgMode the selected background wall paper
     * @param resId the android drawable resource Id for the selected wall paper
     */
    private void setBgColor(int bgMode, int resId) {
        mEditor.putInt(PREF_BACKGROUND, bgMode);
        mEditor.apply();
        background.setBackgroundResource(resId);
    }

    /**
     * Update both the hymnType and hymnNo for share auto-fill
     *
     * @param hymnType Update HymnType as given
     * @param hymnNo Update HymnNo as given
     */
    public static void setHymnTypeNo(String hymnType, int hymnNo) {
        mHymnType = hymnType;
        mHymnNo = hymnNo;
    }

    /**
     * standard ActivityResultContract#StartActivityForResult
     */
    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();
            Uri uri = (intent == null) ? null : intent.getData();
            if (uri == null) {
                Timber.d("No image data selected: %s", intent);
            }
            else {
                setWallpaper();
            }
        }
    });

    /**
     * Checks if the result contains a {@link PackageManager#PERMISSION_GRANTED} result for a
     * permission from a runtime permissions request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                String permission = permissions[i];
                String message = getResources().getString(R.string.permission_app_rational,
                        permission.substring(permission.lastIndexOf(".") + 1));

                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                    message = getResources().getString(R.string.permission_storage_required);
                }
                else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
                    message = getResources().getString(R.string.permission_notifications_required);
                }
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        getResources().getString(R.string.permission_request), message);
            }
        }
    }

    public void onInfoButtonClicked() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }
}
