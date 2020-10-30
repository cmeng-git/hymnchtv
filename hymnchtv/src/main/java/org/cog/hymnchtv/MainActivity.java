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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.*;
import android.text.TextUtils;
import android.util.Range;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.utils.HymnNo2IdxConvert;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;

import static org.cog.hymnchtv.HymnToc.hymnTocPage;

/**
 * MainActivity: The hymnchtv app main user interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
@SuppressLint("NonConstantResourceId")
public class MainActivity extends FragmentActivity implements AdapterView.OnItemSelectedListener
{
    public static final String ATTR_SELECT = "select";
    public static final String ATTR_NUMBER = "number";
    public static final String ATTR_SEARCH = "search";
    public static final String ATTR_PAGE = "page";

    public static final String HYMN_ER = "hymn_er";
    public static final String HYMN_XB = "hymn_xb";
    public static final String HYMN_BB = "hymn_bb";
    public static final String HYMN_DB = "hymn_db";

    public static final String PREF_MENU_SHOW = "MenuShow";
    public static final String PREF_SETTINGS = "Settings";
    public static final String PREF_TEXT_SIZE = "TextSize";
    public static final String PREF_TEXT_COLOR = "TextColor";
    public static final String PREF_BACKGROUND = "Background";

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
    private TextView mTocSpinnerItem;
    private TextView mEntry;

    private LinearLayout background;

    private static SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mEditor;

    private boolean isFu = false;
    private boolean isToc = false;
    private boolean isValid = true;

    private int fontSize = FONT_SIZE_DEFAULT;

    private int nui;
    private String sNumber = "";

    public static MainActivity mActivity;

    private String tocPage;

    /* Maximum HymnNo/HymnIndex: 大本诗歌 and start of its supplement */
    // The values and the similar must be updated if there are any new contents added
    public static final int HYMN_DB_NO_MAX = 780;
    public static final int HYMN_DBS_NO_MAX = 6;

    // FuGe pass-in index is HYMN_DB_NO_MAX + fu Number
    public static final int HYMN_DB_NO_TMAX = 786;
    public static final int HYMN_DB_INDEX_MAX = 786;

    /* Maximum HymnNo/HymnIndex (excluding multiPage i.e. a,b,c,d,e): 補充本 */
    public static final int HYMN_BB_NO_MAX = 1005;
    public static final int HYMN_BB_INDEX_MAX = 513;

    /* Maximum HymnNo/HymnIndex: 新歌颂咏 */
    public static final int HYMN_XB_NO_MAX = 169;
    public static final int HYMN_XB_INDEX_MAX = 169;

    /* Maximum HymnNo/HymnIndex: 儿童诗歌 */
    public static final int HYMN_ER_NO_MAX = 1232;
    public static final int HYMN_ER_INDEX_MAX = 330;


    // ======================================================== //
    // 補充本 range parameters for page number (i.e. less than in each 100 range)
    // Each value is hymnNo + 1 within each 100 range; it is used to generate rangeBbInvalid
    // The values must be updated if there are any new contents added
    public static final int[] rangeBbLimit = {38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006};

    // invalid range for 補充本
    public static final List<Range<Integer>> rangeBbInvalid = new ArrayList<>();

    // Auto generated invalid range for based 補充本 on rangeBbLimit; invalid range for 補充本:
    // (38, 100),(151, 200),(259, 300),(350, 400),(471, 500),(544, 600),(630, 700),(763, 800),(881, 900),(931, 1000)
    static {
        for (int i = 0; i < (rangeBbLimit.length - 1); i++) {
            rangeBbInvalid.add(Range.create(rangeBbLimit[i], 100 * (i + 1)));
        }
    }

    // ======================================================== //
    // 儿童诗歌 range parameters for page number (i.e. less than in each 100 range)
    // Each value is hymnNo + 1 within each 100 range; it is used to generate rangeErInvalid
    // The values must be updated if there are any new contents added
    public static final int[] rangeErLimit = {18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233};

    // invalid range for 儿童诗歌
    public static final List<Range<Integer>> rangeErInvalid = new ArrayList<>();

    // Auto generated invalid range for 儿童诗歌 based on rangeErLimit
    static {
        for (int i = 0; i < (rangeErLimit.length - 1); i++) {
            rangeErInvalid.add(Range.create(rangeErLimit[i], 100 * (i + 1)));
        }
    }

    // Available background wall papers
    public static int[] bgResId = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
            R.drawable.bg20, R.drawable.bg21, R.drawable.bg22, R.drawable.bg23, R.drawable.bg24, R.drawable.bg25};

    @SuppressLint("CommitPrefEdits")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        registerForContextMenu(findViewById(R.id.viewMain));

        mActivity = this;
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();

        initButton();
        setTitle(R.string.app_title_main);
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
        btn_er.setOnClickListener(v -> onErClicked());

        // 新歌颂咏
        btn_xb.setOnClickListener(v -> onXbClicked());

        // 补充本
        btn_bb.setOnClickListener(v -> onBbClicked());

        // 大本诗歌
        btn_db.setOnClickListener(v -> onDbClicked());

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
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    /**
     * Routine to handle Fu and all numeric buttons click
     * @param btnView fu and number buttons views
     */
    private void onNumberClick(View btnView)
    {
        sNumber = sNumber + ((Button) btnView).getText();
        mEntry.setText(sNumber);

        // Re-init TOC and Search Fields to default on hymn number entry
        if (sNumber.length() == 1) {
            tocSpinner.setSelection(0);
            tv_Search.setText("");
        }
    }

    /**
     * Handler for 儿童诗歌 button clicked
     */
    private void onErClicked()
    {
        if (!isToc) {
            if (isFu) {
                HymnsApp.showToastMessage(R.string.hymn_info_sp_none);
                return;
            }

            isValid = true;
            sNumber = mEntry.getText().toString();
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }

            nui = Integer.parseInt(sNumber);
            if (nui > HYMN_ER_NO_MAX) {
                HymnsApp.showToastMessage(R.string.hymn_info_er_max, HYMN_ER_NO_MAX);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }
            else if (nui < 1) {
                HymnsApp.showToastMessage(R.string.gui_error_invalid);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }
            else {
                for (Range<Integer> rangeX : rangeErInvalid) {
                    if (rangeX.contains(nui)) {
                        HymnsApp.showToastMessage(R.string.hymn_info_er_range_over, rangeX.getLower(), rangeX.getUpper());
                        isValid = false;
                        break;
                    }
                }
            }

            if (isValid) {
                showContent(HYMN_ER, nui);
            }
            else {
                sNumber = "";
                mEntry.setText(sNumber);
            }
        }
        else {
            showHymnToc(HYMN_ER);
        }
    }

    /**
     * Handler for 新歌颂咏 button clicked
     */
    private void onXbClicked()
    {
        if (!isToc) {
            if (isFu) {
                HymnsApp.showToastMessage(R.string.hymn_info_sp_none);
                return;
            }

            isValid = true;
            sNumber = mEntry.getText().toString();
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }

            nui = Integer.parseInt(sNumber);
            if (nui > HYMN_XB_NO_MAX) {
                HymnsApp.showToastMessage(R.string.hymn_info_xb_max, HYMN_XB_NO_MAX);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }
            else if (nui < 1) {
                HymnsApp.showToastMessage(R.string.gui_error_invalid);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }

            if (isValid) {
                showContent(HYMN_XB, nui);
            }
            else {
                sNumber = "";
                mEntry.setText(sNumber);
            }
        }
        else {
            showHymnToc(HYMN_XB);
        }
    }

    /**
     * Handler for 补充本 button clicked
     */
    private void onBbClicked()
    {
        if (!isToc) {
            if (isFu) {
                HymnsApp.showToastMessage(R.string.hymn_info_sp_none);
                return;
            }

            isValid = true;
            sNumber = mEntry.getText().toString();
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }

            nui = Integer.parseInt(sNumber);
            if (nui > HYMN_BB_NO_MAX) {
                HymnsApp.showToastMessage(R.string.hymn_info_bb_max, HYMN_BB_NO_MAX);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }
            else if (nui < 1) {
                HymnsApp.showToastMessage(R.string.gui_error_invalid);
                sNumber = "";
                mEntry.setText(sNumber);
                isValid = false;
            }
            else {
                for (Range<Integer> rangeX : rangeBbInvalid) {
                    if (rangeX.contains(nui)) {
                        HymnsApp.showToastMessage(R.string.hymn_info_bb_range_over, rangeX.getLower(), rangeX.getUpper());
                        isValid = false;
                        break;
                    }
                }
            }

            if (isValid) {
                showContent(HYMN_BB, nui);
            }
            else {
                sNumber = "";
                mEntry.setText(sNumber);
            }
        }
        else {
            showHymnToc(HYMN_BB);
        }
    }

    /**
     * Handler for 大本诗歌 button clicked
     */
    private void onDbClicked()
    {
        if (!isToc) {
            sNumber = mEntry.getText().toString();
            if (isFu) {
                sNumber = sNumber.substring(1);
            }
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }
            nui = Integer.parseInt(sNumber);

            isValid = true;
            if (isFu) {
                if (nui < 1 || nui > HYMN_DBS_NO_MAX) {
                    HymnsApp.showToastMessage(R.string.hymn_info_db_range_fu);
                    isFu = false;
                    isValid = false;
                }
                nui += HYMN_DB_NO_MAX;
            }
            else if (nui > HYMN_DB_NO_MAX) {
                HymnsApp.showToastMessage(R.string.hymn_info_db_max, HYMN_DB_NO_MAX);
                isValid = false;
            }
            else if (nui < 1) {
                HymnsApp.showToastMessage(R.string.gui_error_invalid);
                isValid = false;
            }

            if (isValid) {
                showContent(HYMN_DB, nui);
            }
            else {
                sNumber = "";
                mEntry.setText(sNumber);
            }
        }
        else {
            showHymnToc(HYMN_DB);
        }
    }

    /**
     * Show the content of user selected hymn type
     *
     * @param mode Toc or hymn lyrics
     * @param number the content of hymn number to display
     */
    public void showContent(String mode, int number)
    {
        Intent intent = new Intent(this, ContentHandler.class);

        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, mode);
        bundle.putInt(ATTR_NUMBER, number);

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
        Intent intent = new Intent(this, HymnToc.class);

        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, hymnType);
        bundle.putString(ATTR_PAGE, tocPage);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * KeyEvent handler for KeyEvent.KEYCODE_BACK
     *
     * @param keyCode android keyCode
     * @param event KeyEvent
     * @return handler state
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
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

//        if (BuildConfig.DEBUG) {
//            menu.findItem(R.id.sn_convert).setVisible(true);
//        }
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
    }

    /**
     * Handler for the Context item clicked; use the same handlers as Option Item clicked
     *
     * @param item Option Item Item
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

            case R.id.sn_convert:
                // HymnIdx2NoConvert.validateIdx2NoConversion(HYMN_ER, HYMN_ER_INDEX_MAX);
                HymnNo2IdxConvert.validateNo2IdxConversion(HYMN_DB, HYMN_DB_NO_TMAX);
                // Hymn2SnConvert.startConvert(); use for old to new file name conversion for 1.1.0 only
                return true;

            case R.id.online_help:
                About.hymnUrlAccess(this, About.HYMNCHTV_HTTP_LINK);
                return true;

            case R.id.about:
                Intent intent = new Intent(this, About.class);
                startActivity(intent);
                return true;

            case R.id.exit:
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

        // Create an ArrayAdapter using the string array and aTalk default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, hymnTocPage);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        tocSpinner = findViewById(R.id.spinner_toc);
        tocSpinner.setAdapter(mAdapter);

        // to avoid onSelectedItem get triggered on first init and only then init mTocSpinnerItem (else null)
        tocSpinner.setSelection(0, false);
        tocSpinner.setOnItemSelectedListener(this);

        mTocSpinnerItem = tocSpinner.findViewById(R.id.textItem);
    }

    /**
     * Retrieve all the user preference settings and initialize the UI accordingly
     */
    private void initUserSettings()
    {
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        int bgResId = mSharedPref.getInt(PREF_BACKGROUND, 5);
        background.setBackgroundResource(MainActivity.bgResId[bgResId]);

        fontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT);
        setFontSize(fontSize, false);

        int fontColor = mSharedPref.getInt(PREF_TEXT_COLOR, Color.BLACK);
        setFontColor(fontColor, false);
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
            mEditor.commit();
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
        if (update) {
            mEditor.putInt(PREF_TEXT_COLOR, color);
            mEditor.commit();
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
        mEditor.commit();
        background.setBackgroundResource(resid);
    }

    public static SharedPreferences getSharedPref()
    {
        return mSharedPref;
    }
}
