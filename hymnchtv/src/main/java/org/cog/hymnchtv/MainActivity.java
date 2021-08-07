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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.hymnhistory.HistoryRecord;
import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.persistance.*;
import org.cog.hymnchtv.utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

import static org.cog.hymnchtv.HymnToc.TOC_ENGLISH;
import static org.cog.hymnchtv.HymnToc.hymnTocPage;
import static org.cog.hymnchtv.utils.WallPaperUtil.DIR_WALLPAPER;
import static org.cog.hymnchtv.utils.ZoomTextView.MAX_SCALE_FACTOR;

/**
 * MainActivity: The hymnchtv app main user interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class MainActivity extends FragmentActivity implements AdapterView.OnItemSelectedListener
{
    public static String HYMNCHTV_FAQ = "https://cmeng-git.github.io/hymnchtv/faq.html";
    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

    public static final String ATTR_SELECT = "select";
    public static final String ATTR_NUMBER = "number";
    public static final String ATTR_SEARCH = "search";
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_AUTO_PLAY = "autoPlay";

    public static final String HYMN_ER = "hymn_er";
    public static final String HYMN_XB = "hymn_xb";
    public static final String HYMN_BB = "hymn_bb";
    public static final String HYMN_DB = "hymn_db";

    public static final String PREF_MENU_SHOW = "MenuShow";
    public static final String PREF_SETTINGS = "Settings";
    public static final String PREF_TEXT_SIZE = "TextSize";
    public static final String PREF_TEXT_COLOR = "TextColor";
    public static final String PREF_BACKGROUND = "Background";
    public static final String PREF_WALLPAPER = "WallPaper";

    public static final String PREF_MEDIA_HYMN = "MediaHymn";
    private static final int FONT_SIZE_DEFAULT = 35;

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

    private Button btn_er;
    private Button btn_xb;
    private Button btn_bb;
    private Button btn_db;
    private Button btn_search;

    private Spinner tocSpinner;
    private EditText tv_Search;
    private ListView mHistoryListView;
    private TextView mTocSpinnerItem;
    private TextView mEntry;

    private LinearLayout background;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

    private boolean autoClear = false;
    private boolean isFu = false;
    private boolean isToc = false;

    private int fontSize = FONT_SIZE_DEFAULT;
    private int fontColor = Color.BLACK;
    private float lyricsScaleFactor = MAX_SCALE_FACTOR;

    private String sNumber = "";
    private String tocPage;

    // Available background wall papers
    public static int[] bgResId = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
            R.drawable.bg20, R.drawable.bg21, R.drawable.bg22, R.drawable.bg23, R.drawable.bg24, R.drawable.bg25};

    @SuppressLint("CommitPrefEdits")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        registerForContextMenu(findViewById(R.id.viewMain));
        setTitle(R.string.app_title_main);

        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();

        mHistoryListView = findViewById(R.id.historyListView);
        mHistoryListView.setVisibility(View.GONE);
        initButton();
        initUserSettings();

        // PermissionUtils.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        PermissionUtils.requestPermission(this, 1001,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, false);

        // allow 15 seconds wait for first launch before showing history log
        // new ChangeLog(this).getLogDialog().show();
        runOnUiThread(() -> new Handler().postDelayed(() -> {
            ChangeLog cl = new ChangeLog(this);
            if (cl.isFirstRun() && !isFinishing()) {
                cl.getLogDialog().show();
            }
        }, 15000));

        // 儿童诗歌
        btn_er.setOnClickListener(v -> onHymnButtonClicked(HYMN_ER));

        // 新歌颂咏
        btn_xb.setOnClickListener(v -> onHymnButtonClicked(HYMN_XB));

        // 补充本
        btn_bb.setOnClickListener(v -> onHymnButtonClicked(HYMN_BB));

        // 大本诗歌
        btn_db.setOnClickListener(v -> onHymnButtonClicked(HYMN_DB));

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
                HymnsApp.showToastMessage(R.string.gui_error_search_empty);
                return;
            }

            Intent intent = new Intent();
            intent.setClass(this, ContentSearch.class);
            Bundle bundle = new Bundle();
            bundle.putString(ATTR_SEARCH, sValue);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        // replace special hymns character; unable to enter from a standard keyboard.
        btn_search.setOnLongClickListener(v -> {
            String sValue = tv_Search.getText().toString().replaceAll("他", "祂");
            tv_Search.setText(sValue);
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
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handle share intent to extract the share text content or URIS
     *
     * @param intent <tt>Activity</tt> <tt>Intent</tt>.
     */
    private void handleIntent(Intent intent)
    {
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
            mediaLink = getFile(uris.get(0));
        }

        if (mediaLink != null) {
            intent = new Intent(this, MediaConfig.class);
            Bundle bundle = new Bundle();
            bundle.putString(MediaConfig.ATTR_MEDIA_URI, mediaLink);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        autoClear = true;
    }

    private String getFile(Uri uri)
    {
        File inFile = new File(FilePathHelper.getFilePath(this, uri));
        if (inFile.exists()) {
            return inFile.getPath();
        }
        else
            HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);

        return null;
    }

    // 目录 Spinner selector handler
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        tocPage = hymnTocPage.get(position);
        isToc = (position > 0);

        if (isToc) {
            sNumber = "";
            mEntry.setText(tocPage);
        }
        else if (TextUtils.isEmpty(sNumber)) {
            mEntry.setText("");
        }

        // Need to re-init mTocSpinnerItem here whenever a new item is selected
        mTocSpinnerItem = tocSpinner.findViewById(R.id.textItem);
        mTocSpinnerItem.setTypeface(null, Typeface.BOLD);
        mTocSpinnerItem.setGravity(Gravity.CENTER);

        mTocSpinnerItem.setTextSize(fontSize - 10);
        mTocSpinnerItem.setTextColor(fontColor);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    /**
     * Routine to handle Fu and all numeric buttons click
     *
     * @param btnView fu and number buttons views
     */
    private void onNumberClick(View btnView)
    {
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
            tocSpinner.setSelection(0);
            tv_Search.setText("");
        }
    }

    /**
     * Handler for user hymnType button clicks;
     * Show TOC is selected else the content for the hymnNo if valid
     *
     * @param hymnType the button being clicked
     */
    private void onHymnButtonClicked(String hymnType)
    {
        if (!isToc) {
            sNumber = mEntry.getText().toString();
            if (isFu) {
                sNumber = sNumber.substring(1);
            }
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }
            int hymnNo = Integer.parseInt(sNumber);

            int nui = HymnNoValidate.validateHymnNo(hymnType, hymnNo, isFu);
            if (nui != -1) {
                showContent(hymnType, nui);
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
     * Save the user selected hymn into the history table
     * Show the content of user selected hymnType and hymnNo
     *
     * @param hymnType lyrics content of the hymnType
     * @param hymnNo the content of hymnNo to display
     */
    private void showContent(String hymnType, int hymnNo)
    {
        HistoryRecord historyRecord = new HistoryRecord(hymnType, hymnNo, isFu);
        mDB.storeHymnHistory(historyRecord);

        Intent intent = new Intent(this, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, hymnType);
        bundle.putInt(ATTR_NUMBER, hymnNo);
        bundle.putBoolean(ATTR_AUTO_PLAY, false);

        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Show the TOC of the user selected hymn type
     *
     * @param hymnType Hymn Toc
     */
    private void showHymnToc(String hymnType)
    {
        // "英中对照" not implemented for HYMN_ER or HYMN_XB
        if (TOC_ENGLISH.equals(tocPage) && (HYMN_ER.equals(hymnType) || HYMN_XB.equals(hymnType))) {
            HymnsApp.showToastMessage(R.string.gui_in_development);
            return;
        }

        Intent intent = new Intent(this, HymnToc.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, hymnType);
        bundle.putString(ATTR_PAGE, tocPage);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * KeyEvent handler for KeyEvent.KEYCODE_BACK.
     * Remove the fragment view if triggers from a fragment else close app
     *
     * @param keyCode android keyCode
     * @param event KeyEvent
     * @return handler state from android super
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
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
     * @return true always
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // if (BuildConfig.DEBUG) {
        //     menu.findItem(R.id.sn_convert).setVisible(true);
        // }
        return true;
    }

    /**
     * Pop up the main menu if user long press on the main UI
     *
     * @param menu menu
     * @param v view
     * @param menuInfo info
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
    }

    /**
     * Handler for the Context item clicked; use the same handlers as Option Item clicked
     *
     * @param item Option Item
     * @return the handle state
     */
    public boolean onContextItemSelected(MenuItem item)
    {
        return onOptionsItemSelected(item);
    }

    /**
     * Handler for the option item clicked
     *
     * @param item menu Item
     * @return the handle state
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;

        switch (item.getItemId()) {
            // Set font size
            case R.id.small:
                fontSize = FONT_SIZE_DEFAULT - 5;
                setFontSize(fontSize, true);
                return true;

            case R.id.middle:
                fontSize = FONT_SIZE_DEFAULT;
                setFontSize(fontSize, true);
                return true;

            case R.id.lager:
                fontSize = FONT_SIZE_DEFAULT + 5;
                setFontSize(fontSize, true);
                return true;

            case R.id.xlager:
                fontSize = FONT_SIZE_DEFAULT + 10;
                setFontSize(fontSize, true);
                return true;

            case R.id.inc:
                fontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) + 2;
                setFontSize(fontSize, true);
                return true;

            case R.id.dec:
                fontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) - 2;
                setFontSize(fontSize, true);
                return true;

            // Set font color
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
                setFontColor(Color.BLACK, true);
                return true;

            // Set background color
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
            case R.id.stextcolor:
            default:
                return false;
        }
    }

    /**
     * Bind all the button to its resource id
     */
    private void initButton()
    {
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

        btn_er = findViewById(R.id.bs_er);
        btn_xb = findViewById(R.id.bs_xb);
        btn_bb = findViewById(R.id.bs_bb);
        btn_db = findViewById(R.id.bs_db);

        btn_search = findViewById(R.id.btn_search);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, hymnTocPage);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        tocSpinner = findViewById(R.id.spinner_toc);
        tocSpinner.setAdapter(mAdapter);

        // to avoid onSelectedItem get triggered on first init and only then init mTocSpinnerItem (else null)
        tocSpinner.setSelection(0, false);
        tocSpinner.setOnItemSelectedListener(this);

        mTocSpinnerItem = tocSpinner.findViewById(R.id.textItem);
        mTocSpinnerItem.setTypeface(null, Typeface.BOLD);
        mTocSpinnerItem.setGravity(Gravity.CENTER);
    }

    /**
     * Retrieve all the user preference settings and initialize the UI accordingly
     */
    private void initUserSettings()
    {
        setWallpaper();

        fontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT);
        setFontSize(fontSize, false);

        fontColor = mSharedPref.getInt(PREF_TEXT_COLOR, Color.BLACK);
        setFontColor(fontColor, false);
    }

    private void initHistoryList()
    {
        List<HistoryRecord> historyRecords = mDB.getHistoryRecords();
        ArrayAdapter<?> historyAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, historyRecords);
        historyAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mHistoryListView.setAdapter(historyAdapter);

        // short click to enter and show user selected lyrics
        mHistoryListView.setOnItemClickListener((adapterView, view, position, l) -> {
            mEntry.setHint(R.string.hint_hymn_number_enter);
            mHistoryListView.setVisibility(View.GONE);

            HistoryRecord sRecord = historyRecords.get(position);
            isFu = sRecord.isFu();
            sNumber = sRecord.getHymnNoFu();
            mEntry.setText(sNumber);
            showContent(sRecord.getHymnType(), sRecord.getHymnNo());
        });

        // Long click to delete hymn selection entry history on user confirmation
        mHistoryListView.setOnItemLongClickListener((adapterView, view, position, l) -> {
            HistoryRecord sRecord = historyRecords.get(position);

            DialogActivity.showConfirmDialog(this,
                    R.string.gui_delete,
                    R.string.gui_delete_history,
                    R.string.gui_delete, new DialogActivity.DialogListener()
                    {
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            int count = mDB.deleteHymnHistory(sRecord);
                            if (count == 1)
                                historyRecords.remove(position);
                            historyAdapter.notifyDataSetChanged();
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog)
                        {
                        }
                    }, sRecord.toString());
            return true;
        });
    }

    /**
     * Init the main UI wallpaper with one of the predefined image in drawable or user own if bgResId == -1
     */
    private void setWallpaper()
    {
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
     * @param size button lable font size
     * @param update true to update the preference settings
     */
    private void setFontSize(int size, boolean update)
    {
        if (update) {
            mEditor.putInt(PREF_TEXT_SIZE, size);
            mEditor.apply();
        }
        int fs_delta = size - 10;

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

        btn_fu.setTextSize(fs_delta);
        btn_del.setTextSize(fs_delta);
        btn_er.setTextSize(fs_delta);
        btn_xb.setTextSize(fs_delta);
        btn_bb.setTextSize(fs_delta);
        btn_db.setTextSize(fs_delta);

        btn_search.setTextSize(fs_delta);
        mTocSpinnerItem.setTextSize(fs_delta);
    }

    /**
     * Set the color of the buttons' labels
     *
     * @param color text color
     * @param update true to update the preference settings
     */
    private void setFontColor(int color, boolean update)
    {
        fontColor = color;
        if (update) {
            mEditor.putInt(PREF_TEXT_COLOR, color);
            mEditor.apply();
        }

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

        btn_er.setTextColor(color);
        btn_xb.setTextColor(color);
        btn_bb.setTextColor(color);
        btn_db.setTextColor(color);
        btn_search.setTextColor(color);

        mTocSpinnerItem.setTextColor(color);
    }

    /**
     * Set the main UI background wall paper
     *
     * @param bgMode the selected background wall paper
     * @param resid the android drawable resource Id for the selected wall paper
     */
    private void setBgColor(int bgMode, int resid)
    {
        mEditor.putInt(PREF_BACKGROUND, bgMode);
        mEditor.apply();
        background.setBackgroundResource(resid);
    }

    /**
     * standard ActivityResultContract#StartActivityForResult
     **/
    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();
            Uri uri = intent.getData();
            if (uri == null) {
                Timber.d("No image data selected: %s", intent);
            }
            else {
                setWallpaper();
            }
        }
    });
}
