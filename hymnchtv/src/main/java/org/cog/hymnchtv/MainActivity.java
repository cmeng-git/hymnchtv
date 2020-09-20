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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Range;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.utils.DialogActivity;
import org.cog.hymnchtv.utils.HymnNo2IdxConvert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

import static org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl.APK_MIME_TYPE;
import static org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl.UNINSTALL_REQUEST_CODE;

/**
 * MainActivity: The hymnchtv app main user interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
@SuppressLint("NonConstantResourceId")
public class MainActivity extends FragmentActivity
{
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_SELECT = "select";
    public static final String ATTR_NUMBER = "number";
    public static final String ATTR_SEARCH = "search";

    public static final String HYMN_ER = "hymn_er";
    public static final String HYMN_NB = "hymn_nb";
    public static final String HYMN_BB = "hymn_bb";
    public static final String HYMN_DB = "hymn_db";

    public static final String TOC_ER = "toc_er";
    public static final String TOC_NB = "toc_nb";
    public static final String TOC_BB = "toc_bb";
    public static final String TOC_DB = "toc_db";

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

    private Button btn_toc;
    private Button btn_er;
    private Button btn_nb;
    private Button btn_bb;
    private Button btn_db;
    private Button btn_search;

    private EditText tv_Search;
    private TextView mEntry;
    private TextView mHint;

    private LinearLayout background;

    private PopupWindow pop;
    private PopupWindow dbpop; // toc db
    private PopupWindow bbpop; // toc bb
    private PopupWindow nbpop; // toc nb

    private static SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mEditor;

    private boolean isFu = false;
    private boolean isToc = false;
    private boolean isValid = true;

    private int fontColor = 0;
    private int fontSize = FONT_SIZE_DEFAULT;

    private int nui;
    private String sNumber = "";

    public static MainActivity mActivity;

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
    public static final int HYMN_NB_NO_MAX = 169;
    public static final int HYMN_NB_INDEX_MAX = 169;

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
    public static int[] bgRes = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
            R.drawable.bg20, R.drawable.bg21, R.drawable.bg22, R.drawable.bg23, R.drawable.bg24, R.drawable.bg25};

    @SuppressLint("CommitPrefEdits")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        registerForContextMenu(findViewById(R.id.viewMain));

        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();

        initButton();
        setTitle(R.string.app_title_main);
        initUserSettings();

        // PermissionUtils.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        PermissionUtils.requestPermission(this, 1001,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, false);

        // allow 15 seconds for first launch login to complete before showing history log
        // new ChangeLog(this).getLogDialog().show();
        runOnUiThread(() -> new Handler().postDelayed(() -> {
            ChangeLog cl = new ChangeLog(this);
            if (cl.isFirstRun() && !isFinishing()) {
                cl.getLogDialog().show();
            }
        }, 15000));

        mActivity = this;

        btn_toc.setOnClickListener(v -> {
            isToc = true;
            sNumber = "";
            mEntry.setText(R.string.hymn_toc);
        });

        // 儿童诗歌
        btn_er.setOnClickListener(v -> {
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
                HymnsApp.showToastMessage(R.string.gui_in_development);
            }
        });

        // 新歌颂咏
        btn_nb.setOnClickListener(v -> {
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
                if (nui > HYMN_NB_NO_MAX) {
                    HymnsApp.showToastMessage(R.string.hymn_info_nb_max, HYMN_NB_NO_MAX);
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
                    showContent(HYMN_NB, nui);
                }
                else {
                    sNumber = "";
                    mEntry.setText(sNumber);
                }
            }
            else {
                // showContent(TOC_NB, 1);
                View toc_nb = getLayoutInflater().inflate(R.layout.toc_nb, (ViewGroup) null);
                nbpop = new PopupWindow(toc_nb, HymnsApp.screenWidth, HymnsApp.screenHeight);
                nbpop.showAtLocation(toc_nb, 17, 0, 0);

                TextView nbmt1 = toc_nb.findViewById(R.id.nbmt1);
                TextView nbmt2 = toc_nb.findViewById(R.id.nbmt2);
                TextView nbmt3 = toc_nb.findViewById(R.id.nbmt3);
                TextView nbmt4 = toc_nb.findViewById(R.id.nbmt4);
                TextView nbmt5 = toc_nb.findViewById(R.id.nbmt5);
                TextView nbmt6 = toc_nb.findViewById(R.id.nbmt6);
                TextView nbmt7 = toc_nb.findViewById(R.id.nbmt7);
                TextView btn_close_nb = toc_nb.findViewById(R.id.btn_close_nb);

                nbmt1.setOnClickListener(view -> showContent(TOC_NB, 1));
                nbmt2.setOnClickListener(view -> showContent(TOC_NB, 1));
                nbmt3.setOnClickListener(view -> showContent(TOC_NB, 2));
                nbmt4.setOnClickListener(view -> showContent(TOC_NB, 3));
                nbmt5.setOnClickListener(view -> showContent(TOC_NB, 4));
                nbmt6.setOnClickListener(view -> showContent(TOC_NB, 4));
                nbmt7.setOnClickListener(view -> showContent(TOC_NB, 4));

                btn_close_nb.setOnClickListener(view -> {
                    nbpop.dismiss();
                    nbpop = null;
                });
            }
        });

        // 补充本
        btn_bb.setOnClickListener(v -> {
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
                View toc_xb = getLayoutInflater().inflate(R.layout.toc_bb, (ViewGroup) null);
                bbpop = new PopupWindow(toc_xb, HymnsApp.screenWidth, HymnsApp.screenHeight);
                bbpop.showAtLocation(toc_xb, 17, 0, 0);

                TextView bbmt1 = toc_xb.findViewById(R.id.bbmt1);
                TextView bbmt2 = toc_xb.findViewById(R.id.bbmt2);
                TextView bbmt3 = toc_xb.findViewById(R.id.bbmt3);
                TextView bbmt4 = toc_xb.findViewById(R.id.bbmt4);
                TextView bbmt5 = toc_xb.findViewById(R.id.bbmt5);
                TextView bbmt6 = toc_xb.findViewById(R.id.bbmt6);
                TextView bbmt7 = toc_xb.findViewById(R.id.bbmt7);
                TextView bbmt8 = toc_xb.findViewById(R.id.bbmt8);
                TextView bbmt9 = toc_xb.findViewById(R.id.bbmt9);
                TextView bbmt10 = toc_xb.findViewById(R.id.bbmt10);
                TextView bbmt11 = toc_xb.findViewById(R.id.bbmt11);
                TextView bbmt12 = toc_xb.findViewById(R.id.bbmt12);
                TextView bbmt13 = toc_xb.findViewById(R.id.bbmt13);
                TextView btn_close_xb = toc_xb.findViewById(R.id.btn_close_xb);

                bbmt1.setOnClickListener(view -> showContent(TOC_BB, 1));
                bbmt2.setOnClickListener(view -> showContent(TOC_BB, 2));
                bbmt3.setOnClickListener(view -> showContent(TOC_BB, 4));
                bbmt4.setOnClickListener(view -> showContent(TOC_BB, 6));
                bbmt5.setOnClickListener(view -> showContent(TOC_BB, 8));
                bbmt6.setOnClickListener(view -> showContent(TOC_BB, 10));
                bbmt7.setOnClickListener(view -> showContent(TOC_BB, 12));
                bbmt8.setOnClickListener(view -> showContent(TOC_BB, 13));
                bbmt9.setOnClickListener(view -> showContent(TOC_BB, 15));
                bbmt10.setOnClickListener(view -> showContent(TOC_BB, 18));
                bbmt11.setOnClickListener(view -> showContent(TOC_BB, 19));
                bbmt12.setOnClickListener(view -> showContent(TOC_BB, 20));
                bbmt13.setOnClickListener(view -> showContent(TOC_BB, 27));

                btn_close_xb.setOnClickListener(view -> {
                    bbpop.dismiss();
                    bbpop = null;
                });
            }
        });

        // 大本诗歌
        btn_db.setOnClickListener(v -> {
            if (!isToc) {
                isValid = true;
                sNumber = mEntry.getText().toString();
                if (sNumber.equals("")) {
                    sNumber = "0";
                }

                nui = Integer.parseInt(sNumber);
                if (isFu) {
                    if (nui < 1 || nui > HYMN_DBS_NO_MAX) {
                        HymnsApp.showToastMessage(R.string.hymn_info_db_range_fu);
                        sNumber = "";
                        mHint.setText(R.string.hymn_fu);
                        isValid = false;
                    }
                }
                else if (nui > HYMN_DB_NO_MAX) {
                    HymnsApp.showToastMessage(R.string.hymn_info_db_max, HYMN_DB_NO_MAX);
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
                    if (isFu) {
                        nui += HYMN_DB_NO_MAX;
                    }
                    showContent(HYMN_DB, nui);
                }
                else {
                    sNumber = "";
                    mEntry.setText(sNumber);
                }
            }
            else {
                View toc_db = getLayoutInflater().inflate(R.layout.toc_db, (ViewGroup) null);
                dbpop = new PopupWindow(toc_db, HymnsApp.screenWidth, HymnsApp.screenHeight);
                dbpop.showAtLocation(toc_db, 17, 0, 0);

                TextView dbmt1 = toc_db.findViewById(R.id.dbmt1);
                TextView dbmt2 = toc_db.findViewById(R.id.dbmt2);
                TextView dbmt3 = toc_db.findViewById(R.id.dbmt3);
                TextView dbmt4 = toc_db.findViewById(R.id.dbmt4);
                TextView dbmt5 = toc_db.findViewById(R.id.dbmt5);
                TextView dbmt6 = toc_db.findViewById(R.id.dbmt6);
                TextView dbmt7 = toc_db.findViewById(R.id.dbmt7);
                TextView dbmt8 = toc_db.findViewById(R.id.dbmt8);
                TextView dbmt9 = toc_db.findViewById(R.id.dbmt9);
                TextView dbmt10 = toc_db.findViewById(R.id.dbmt10);
                TextView dbmt11 = toc_db.findViewById(R.id.dbmt11);
                TextView dbmt12 = toc_db.findViewById(R.id.dbmt12);
                TextView dbmt13 = toc_db.findViewById(R.id.dbmt13);
                TextView dbmt14 = toc_db.findViewById(R.id.dbmt14);
                TextView dbmt15 = toc_db.findViewById(R.id.dbmt15);
                TextView dbmt16 = toc_db.findViewById(R.id.dbmt16);
                TextView dbmt17 = toc_db.findViewById(R.id.dbmt17);
                TextView dbmt18 = toc_db.findViewById(R.id.dbmt18);
                TextView dbmt19 = toc_db.findViewById(R.id.dbmt19);
                TextView dbmt20 = toc_db.findViewById(R.id.dbmt20);
                TextView dbmt21 = toc_db.findViewById(R.id.dbmt21);
                TextView dbmt22 = toc_db.findViewById(R.id.dbmt22);
                TextView dbmt23 = toc_db.findViewById(R.id.dbmt23);
                TextView dbmt24 = toc_db.findViewById(R.id.dbmt24);
                TextView dbmt25 = toc_db.findViewById(R.id.dbmt25);
                TextView dbmt26 = toc_db.findViewById(R.id.dbmt26);
                TextView dbmt27 = toc_db.findViewById(R.id.dbmt27);
                TextView dbmt28 = toc_db.findViewById(R.id.dbmt28);
                TextView dbmt29 = toc_db.findViewById(R.id.dbmt29);
                TextView btn_close_db = toc_db.findViewById(R.id.btn_close_db);

                dbmt1.setOnClickListener(view -> showContent(TOC_DB, 1));
                dbmt2.setOnClickListener(view -> showContent(TOC_DB, 1));
                dbmt3.setOnClickListener(view -> showContent(TOC_DB, 2));
                dbmt4.setOnClickListener(view -> showContent(TOC_DB, 3));
                dbmt5.setOnClickListener(view -> showContent(TOC_DB, 3));
                dbmt6.setOnClickListener(view -> showContent(TOC_DB, 4));
                dbmt7.setOnClickListener(view -> showContent(TOC_DB, 5));
                dbmt8.setOnClickListener(view -> showContent(TOC_DB, 5));
                dbmt9.setOnClickListener(view -> showContent(TOC_DB, 6));
                dbmt10.setOnClickListener(view -> showContent(TOC_DB, 7));
                dbmt11.setOnClickListener(view -> showContent(TOC_DB, 7));
                dbmt12.setOnClickListener(view -> showContent(TOC_DB, 7));
                dbmt13.setOnClickListener(view -> showContent(TOC_DB, 8));
                dbmt14.setOnClickListener(view -> showContent(TOC_DB, 8));
                dbmt15.setOnClickListener(view -> showContent(TOC_DB, 9));
                dbmt16.setOnClickListener(view -> showContent(TOC_DB, 10));
                dbmt17.setOnClickListener(view -> showContent(TOC_DB, 11));
                dbmt18.setOnClickListener(view -> showContent(TOC_DB, 11));
                dbmt19.setOnClickListener(view -> showContent(TOC_DB, 11));
                dbmt20.setOnClickListener(view -> showContent(TOC_DB, 12));
                dbmt21.setOnClickListener(view -> showContent(TOC_DB, 12));
                dbmt22.setOnClickListener(view -> showContent(TOC_DB, 13));
                dbmt23.setOnClickListener(view -> showContent(TOC_DB, 13));
                dbmt24.setOnClickListener(view -> showContent(TOC_DB, 14));
                dbmt25.setOnClickListener(view -> showContent(TOC_DB, 14));
                dbmt26.setOnClickListener(view -> showContent(TOC_DB, 15));
                dbmt27.setOnClickListener(view -> showContent(TOC_DB, 15));
                dbmt28.setOnClickListener(view -> showContent(TOC_DB, 15));
                dbmt29.setOnClickListener(view -> showContent(TOC_DB, 16));

                btn_close_db.setOnClickListener(view -> {
                    dbpop.dismiss();
                    dbpop = null;
                });
            }
        });

        btn_n0.setOnClickListener(v -> {
            sNumber = sNumber + "0";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n1.setOnClickListener(v -> {
            sNumber = sNumber + "1";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n2.setOnClickListener(v -> {
            sNumber = sNumber + "2";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n3.setOnClickListener(v -> {
            sNumber = sNumber + "3";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n4.setOnClickListener(v -> {
            sNumber = sNumber + "4";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n5.setOnClickListener(v -> {
            sNumber = sNumber + "5";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n6.setOnClickListener(v -> {
            sNumber = sNumber + "6";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n7.setOnClickListener(v -> {
            sNumber = sNumber + "7";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n8.setOnClickListener(v -> {
            sNumber = sNumber + "8";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_n9.setOnClickListener(v -> {
            sNumber = sNumber + "9";
            isToc = false;
            mEntry.setText(sNumber);
        });

        btn_fu.setOnClickListener(v -> {
            mHint.setText(R.string.hymn_fu);
            isToc = false;
            isFu = true;
        });

        btn_del.setOnClickListener(v -> {
            sNumber = "";
            mEntry.setText(sNumber);
            mHint.setText(R.string.hint_hymn_ui);
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
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == 82) {
            if (pop != null) {
                pop.dismiss();
                pop = null;
            }
        }
        else if (keyCode == 4) {
            if (pop != null) {
                pop.dismiss();
                pop = null;
            }
            else if (bbpop != null) {
                bbpop.dismiss();
                bbpop = null;
            }
            else if (dbpop != null) {
                dbpop.dismiss();
                dbpop = null;
            }
            else {
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.sn_convert).setVisible(true);
        }
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu2, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu2);
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        return onOptionsItemSelected(item);
    }

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
                fontColor = R.drawable.red;
                setFontColor(fontColor, true);
                return true;

            case R.id.blue:
                fontColor = R.drawable.blue;
                setFontColor(fontColor, true);
                return true;

            case R.id.white:
                fontColor = R.drawable.white;
                setFontColor(fontColor, true);
                return true;

            case R.id.grey:
                fontColor = R.drawable.grey;
                setFontColor(fontColor, true);
                return true;

            case R.id.brown:
                fontColor = R.drawable.coffee;
                setFontColor(fontColor, true);
                return true;

            case R.id.yellow:
                fontColor = R.drawable.yellow;
                setFontColor(fontColor, true);
                return true;

            case R.id.green:
                fontColor = R.drawable.green;
                setFontColor(fontColor, true);
                return true;

            case R.id.black:
                fontColor = R.drawable.black;
                setFontColor(fontColor, true);
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

            case R.id.about:
                Intent intent = new Intent(this, About.class);
                startActivity(intent);
                // DialogActivity.showDialog(this, R.string.gui_about, R.string.content_about);
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

    private void initButton()
    {
        background = findViewById(R.id.viewMain);
        mHint = findViewById(R.id.tv_hint);
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
        btn_nb = findViewById(R.id.bs_nb);
        btn_bb = findViewById(R.id.bs_xb);
        btn_db = findViewById(R.id.bs_db);

        btn_search = findViewById(R.id.btn_search);
        btn_toc = findViewById(R.id.bs_toc);
    }

    private void initUserSettings()
    {
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        int setbgs = mSharedPref.getInt(PREF_BACKGROUND, 0);
        fontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT);
        fontColor = mSharedPref.getInt(PREF_TEXT_COLOR, R.drawable.black);
        background.setBackgroundResource(bgRes[setbgs]);

        setFontSize(fontSize, false);
        setFontColor(fontColor, false);
    }

    private void showContent(String mode, int number)
    {
        Intent intent = new Intent();
        intent.setClass(this, ContentHandler.class);

        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, mode);
        bundle.putInt(ATTR_NUMBER, number);
        bundle.putString(ATTR_PAGE, ContentHandler.PAGE_MAIN);

        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

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
        mEntry.setTextSize(size);

        btn_fu.setTextSize(fs_delta);
        btn_del.setTextSize(fs_delta);
        btn_er.setTextSize(fs_delta);
        btn_nb.setTextSize(fs_delta);
        btn_bb.setTextSize(fs_delta);
        btn_db.setTextSize(fs_delta);

        btn_search.setTextSize(fs_delta);
        btn_toc.setTextSize(fs_delta);
    }

    private void setFontColor(int color, boolean update)
    {
        Resources res = getResources();
        if (update) {
            mEditor.putInt(PREF_TEXT_COLOR, color);
            mEditor.commit();
        }

        btn_n0.setTextColor(res.getColor(color));
        btn_n1.setTextColor(res.getColor(color));
        btn_n2.setTextColor(res.getColor(color));
        btn_n3.setTextColor(res.getColor(color));
        btn_n4.setTextColor(res.getColor(color));
        btn_n5.setTextColor(res.getColor(color));
        btn_n6.setTextColor(res.getColor(color));
        btn_n7.setTextColor(res.getColor(color));
        btn_n8.setTextColor(res.getColor(color));
        btn_n9.setTextColor(res.getColor(color));

        btn_fu.setTextColor(res.getColor(color));
        btn_del.setTextColor(res.getColor(color));

        btn_toc.setTextColor(res.getColor(color));
        btn_nb.setTextColor(res.getColor(color));
        btn_db.setTextColor(res.getColor(color));
        btn_bb.setTextColor(res.getColor(color));
        btn_search.setTextColor(res.getColor(color));
    }

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

    // ================= Handling of apk update =================
//    /**
//     * Asks the user to install downloaded .apk; e.g. due to version code conflict.
//     *
//     * @param fileUri download file uri of the apk to install.
//     */
//    public void askUninstallApk(final Uri fileUri)
//    {
//        String app_pkg_name = "org.cog.hymnchtv";
//        File apkFile = new File(FilePathHelper.getPath(HymnsApp.getGlobalContext(), fileUri));
//
//        DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
//                R.string.gui_download_completed,
//                R.string.gui_downloaded_uninstall,
//                R.string.gui_ok,
//                new DialogActivity.DialogListener()
//                {
//                    @Override
//                    public boolean onConfirmClicked(DialogActivity dialog)
//                    {
//                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
//                        Intent intent;
//                        intent = new Intent(Intent.ACTION_PACKAGE_REPLACED);
//                        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        intent.setDataAndType(fileUri, APK_MIME_TYPE);
//
//                        startActivity(intent);
//                        return true;
//                    }
//
//                    @Override
//                    public void onDialogCancelled(DialogActivity dialog)
//                    {
//                        askInstallDownloadedApk(fileUri);
//                    }
//                }, apkFile.getAbsolutePath());
//    }
//
//    /**
//     * Asks the user whether to install downloaded .apk.
//     *
//     * @param fileUri download file uri of the apk to install.
//     */
//    private void askInstallDownloadedApk(Uri fileUri)
//    {
//        DialogActivity.showConfirmDialog(HymnsApp.getGlobalContext(),
//                R.string.gui_download_completed,
//                R.string.gui_downloaded_install,
//                R.string.gui_update,
//                new DialogActivity.DialogListener()
//                {
//                    @Override
//                    public boolean onConfirmClicked(DialogActivity dialog)
//                    {
//                        // Need REQUEST_INSTALL_PACKAGES in manifest; Intent.ACTION_VIEW works for both
//                        Intent intent;
//                        intent = new Intent(Intent.ACTION_MY_PACKAGE_REPLACED);  // crash the apk
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                        intent.setDataAndType(fileUri, APK_MIME_TYPE);
//
//                        startActivity(intent);
//                        return true;
//                    }
//
//                    @Override
//                    public void onDialogCancelled(DialogActivity dialog)
//                    {
//                    }
//                });
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data)
//    {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == UNINSTALL_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                Timber.d("onActivityResult: user accepted the uninstall");
//            }
//            else if (resultCode == RESULT_CANCELED) {
//                Timber.d("onActivityResult: user canceled the uninstall");
//            }
//            else if (resultCode == RESULT_FIRST_USER) {
//                Timber.d("onActivityResult: failed to uninstall");
//            }
//        }
//    }
}
