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
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Range;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.DialogActivity;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;

/**
 * MainActivity: The hymnchtv app main menu interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
@SuppressLint("NonConstantResourceId")
public class MainActivity extends FragmentActivity
{
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_SELECT = "sel";
    public static final String ATTR_NUMBER = "nu";
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
    private PopupWindow dbpop;  // toc db
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

    // 補充本 range parameters for page number
    public static final int BB_MAX = 1005;
    public static final int[] rangeLM = {38, 151, 259, 350, 471, 544, 630, 763, 881, 931};

    // invalid range for 補充本
    public static final List<Range<Integer>> rangeBb = new ArrayList<>();

    static {
        for (int i = 0; i < 10; i++) {
            rangeBb.add(Range.create(rangeLM[i], (i + 1) * 100));
        }
        // invalid range for 補充本
        // rangeBb.add(Range.create(38, 100));
        // rangeBb.add(Range.create(151, 200));
        // rangeBb.add(Range.create(259, 300));
        // rangeBb.add(Range.create(350, 400));
        // rangeBb.add(Range.create(471, 500));
        // rangeBb.add(Range.create(544, 600));
        // rangeBb.add(Range.create(630, 700));
        // rangeBb.add(Range.create(763, 800));
        // rangeBb.add(Range.create(881, 900));
        // rangeBb.add(Range.create(931, 1000));
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
        runOnUiThread(() -> new Handler().postDelayed(() -> {
            ChangeLog cl = new ChangeLog(this);
            if (cl.isFirstRun() && !isFinishing()) {
                cl.getLogDialog().show();
            }
        }, 15000));

        btn_toc.setOnClickListener(v -> {
            isToc = true;
            sNumber = "";
            mEntry.setText(R.string.hymn_toc);
        });

        // 儿童诗歌
        btn_er.setOnClickListener(v -> {
            isValid = false;
            if (isToc) {
                HymnsApp.showToastMessage(R.string.gui_in_development);

                // #TODO
                // View toc_er = getLayoutInflater().inflate(R.layout.toc_er, (ViewGroup) null);
                // TextView ermt1 = toc_er.findViewById(R.id.nbmt1);
                // erbmt1.setOnClickListener(view -> showContent(1, TOC_ER));

            }
            else {
                isValid = false;
                sNumber = mEntry.getText().toString();
                if (TextUtils.isEmpty(sNumber)) {
                    sNumber = "0";
                }
                HymnsApp.showToastMessage(R.string.gui_in_development);
                nui = Integer.parseInt(sNumber);
                // #TODO
            }

            if (isValid) {
                showContent(nui, HYMN_ER);
            }
        });

        // 新歌颂咏
        btn_nb.setOnClickListener(v -> {
            isValid = false;
            if (isToc) {
                // showContent(1, TOC_NB);
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

                nbmt1.setOnClickListener(view -> showContent(1, TOC_NB));
                nbmt2.setOnClickListener(view -> showContent(1, TOC_NB));
                nbmt3.setOnClickListener(view -> showContent(2, TOC_NB));
                nbmt4.setOnClickListener(view -> showContent(3, TOC_NB));
                nbmt5.setOnClickListener(view -> showContent(4, TOC_NB));
                nbmt6.setOnClickListener(view -> showContent(4, TOC_NB));
                nbmt7.setOnClickListener(view -> showContent(4, TOC_NB));

                btn_close_nb.setOnClickListener(view -> {
                    nbpop.dismiss();
                    nbpop = null;
                });
            }
            else {
                isValid = true;
                sNumber = mEntry.getText().toString();
                if (TextUtils.isEmpty(sNumber)) {
                    sNumber = "0";
                }

                nui = Integer.parseInt(sNumber);
                if (nui > 156) {
                    HymnsApp.showToastMessage(R.string.hymn_info_nb);
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
            }

            if (isValid) {
                showContent(nui, HYMN_NB);
            }
        });

        // 补充本
        btn_bb.setOnClickListener(v -> {
            isValid = false;
            if (isToc) {
                View toc_xb = getLayoutInflater().inflate(R.layout.toc_xb, (ViewGroup) null);
                bbpop = new PopupWindow(toc_xb, HymnsApp.screenWidth, HymnsApp.screenHeight);
                bbpop.showAtLocation(toc_xb, 17, 0, 0);

                TextView xbmt1 = toc_xb.findViewById(R.id.xbmt1);
                TextView xbmt2 = toc_xb.findViewById(R.id.xbmt2);
                TextView xbmt3 = toc_xb.findViewById(R.id.xbmt3);
                TextView xbmt4 = toc_xb.findViewById(R.id.xbmt4);
                TextView xbmt5 = toc_xb.findViewById(R.id.xbmt5);
                TextView xbmt6 = toc_xb.findViewById(R.id.xbmt6);
                TextView xbmt7 = toc_xb.findViewById(R.id.xbmt7);
                TextView xbmt8 = toc_xb.findViewById(R.id.xbmt8);
                TextView xbmt9 = toc_xb.findViewById(R.id.xbmt9);
                TextView xbmt10 = toc_xb.findViewById(R.id.xbmt10);
                TextView xbmt11 = toc_xb.findViewById(R.id.xbmt11);
                TextView btn_close_xb = toc_xb.findViewById(R.id.btn_close_xb);

                xbmt1.setOnClickListener(view -> showContent(1, TOC_BB));
                xbmt2.setOnClickListener(view -> showContent(2, TOC_BB));
                xbmt3.setOnClickListener(view -> showContent(4, TOC_BB));
                xbmt4.setOnClickListener(view -> showContent(6, TOC_BB));
                xbmt5.setOnClickListener(view -> showContent(8, TOC_BB));
                xbmt6.setOnClickListener(view -> showContent(10, TOC_BB));
                xbmt7.setOnClickListener(view -> showContent(12, TOC_BB));
                xbmt8.setOnClickListener(view -> showContent(13, TOC_BB));
                xbmt9.setOnClickListener(view -> showContent(15, TOC_BB));
                xbmt10.setOnClickListener(view -> showContent(18, TOC_BB));
                xbmt11.setOnClickListener(view -> showContent(19, TOC_BB));

                btn_close_xb.setOnClickListener(view -> {
                    bbpop.dismiss();
                    bbpop = null;
                });
            }
            else if (isFu) {
                HymnsApp.showToastMessage(R.string.hymn_info_xb);
                isValid = false;
            }
            else {
                isValid = true;
                sNumber = mEntry.getText().toString();
                if (TextUtils.isEmpty(sNumber)) {
                    sNumber = "0";
                }

                nui = Integer.parseInt(sNumber);
                if (nui < 1) {
                    HymnsApp.showToastMessage(R.string.gui_error_invalid);
                    sNumber = "";
                    mEntry.setText(sNumber);
                    isValid = false;
                }
                else if (nui > BB_MAX) {
                    HymnsApp.showToastMessage(R.string.gui_ereor_xb_ranger_over);
                    sNumber = "";
                    mEntry.setText(sNumber);
                    isValid = false;
                }
                else {
                    for (Range<Integer> rangeX : rangeBb) {
                        if (rangeX.contains(nui)) {
                            HymnsApp.showToastMessage(R.string.gui_error_xb_range, rangeX.getLower(), rangeX.getUpper());
                            isValid = false;
                            break;
                        }
                    }
                }
            }

            if (isValid) {
                showContent(nui, HYMN_BB);
            }
            else {
                sNumber = "";
                mEntry.setText(sNumber);
            }
        });

        // 大本诗歌
        btn_db.setOnClickListener(v -> {
            isValid = false;
            if (isToc) {
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

                dbmt1.setOnClickListener(view -> showContent(1, TOC_DB));
                dbmt2.setOnClickListener(view -> showContent(1, TOC_DB));
                dbmt3.setOnClickListener(view -> showContent(2, TOC_DB));
                dbmt4.setOnClickListener(view -> showContent(3, TOC_DB));
                dbmt5.setOnClickListener(view -> showContent(3, TOC_DB));
                dbmt6.setOnClickListener(view -> showContent(4, TOC_DB));
                dbmt7.setOnClickListener(view -> showContent(5, TOC_DB));
                dbmt8.setOnClickListener(view -> showContent(5, TOC_DB));
                dbmt9.setOnClickListener(view -> showContent(6, TOC_DB));
                dbmt10.setOnClickListener(view -> showContent(7, TOC_DB));
                dbmt11.setOnClickListener(view -> showContent(7, TOC_DB));
                dbmt12.setOnClickListener(view -> showContent(7, TOC_DB));
                dbmt13.setOnClickListener(view -> showContent(8, TOC_DB));
                dbmt14.setOnClickListener(view -> showContent(8, TOC_DB));
                dbmt15.setOnClickListener(view -> showContent(9, TOC_DB));
                dbmt16.setOnClickListener(view -> showContent(10, TOC_DB));
                dbmt17.setOnClickListener(view -> showContent(11, TOC_DB));
                dbmt18.setOnClickListener(view -> showContent(11, TOC_DB));
                dbmt19.setOnClickListener(view -> showContent(11, TOC_DB));
                dbmt20.setOnClickListener(view -> showContent(12, TOC_DB));
                dbmt21.setOnClickListener(view -> showContent(12, TOC_DB));
                dbmt22.setOnClickListener(view -> showContent(13, TOC_DB));
                dbmt23.setOnClickListener(view -> showContent(13, TOC_DB));
                dbmt24.setOnClickListener(view -> showContent(14, TOC_DB));
                dbmt25.setOnClickListener(view -> showContent(14, TOC_DB));
                dbmt26.setOnClickListener(view -> showContent(15, TOC_DB));
                dbmt27.setOnClickListener(view -> showContent(15, TOC_DB));
                dbmt28.setOnClickListener(view -> showContent(15, TOC_DB));
                dbmt29.setOnClickListener(view -> showContent(16, TOC_DB));

                btn_close_db.setOnClickListener(view -> {
                    dbpop.dismiss();
                    dbpop = null;
                });
            }
            else {
                isValid = true;
                sNumber = mEntry.getText().toString();
                if (sNumber.equals("")) {
                    sNumber = "0";
                }

                nui = Integer.parseInt(sNumber);
                if (isFu) {
                    if (nui < 1 || nui > 6) {
                        HymnsApp.showToastMessage(R.string.gui_ereor_db_ranger_fu);
                        sNumber = "";
                        mHint.setText(R.string.hymn_fu);
                        isValid = false;
                    }
                }
                else if (nui > 780) {
                    HymnsApp.showToastMessage(R.string.gui_ereor_db_ranger_over);
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
            }

            if (isValid) {
                if (isFu) {
                    nui += 780;
                }
                showContent(nui, HYMN_DB);
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

    public boolean onCreateOptionsMenu(Menu menu2)
    {
        super.onCreateOptionsMenu(menu2);
        getMenuInflater().inflate(R.menu.main_menu, menu2);
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

            case R.id.update_check:
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        new UpdateServiceImpl().checkForUpdates(true);
                    }
                }.start();
                return true;

            case R.id.about:
                DialogActivity.showDialog(this, R.string.gui_about, R.string.content_about);
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

    private void showContent(int number, String mode)
    {
        Intent intent = new Intent();
        intent.setClass(this, ContentHandler.class);

        Bundle bundle = new Bundle();
        bundle.putInt(ATTR_NUMBER, number);
        bundle.putString(ATTR_SELECT, mode);
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
}
