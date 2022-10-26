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

import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.FragmentActivity;

import com.zqc.opencc.android.lib.ConversionType;

import org.cog.hymnchtv.ContentView;
import org.cog.hymnchtv.R;

/**
 * The class allows user to define the final S2T conversion type.
 *
 * @author Eng Chong Meng
 */
public class ChineseS2TSelection extends FragmentActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener
{
    private SharedPreferences mSharedPref;
    private ConversionType mConversionType;
    private boolean mHasChanges = false;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chinese_t2s_selection);

        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        String cType = mSharedPref.getString(ContentView.PREF_CONVERSION_TYPE, ConversionType.S2T.toString());
        // mConversionType = Enum.valueOf(ConversionType.class, cType);
        checkRadioButton(cType);

        // Only enable OnCheckedChangeListener only after checkRadioButton()
        RadioGroup radioGroup = findViewById(R.id.radioGroupVar);
        radioGroup.setOnCheckedChangeListener(this);

        findViewById(R.id.btnCancel).setOnClickListener(this);
        findViewById(R.id.btnOk).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.btnOk:
                updateS2TSelection(mHasChanges);
                break;

            case R.id.btnCancel:
                checkUnsavedChanges();
                break;
        }
    }

    /**
     * Select the radio button corresponding to the given cType
     *
     * @param cType Enum type as string for matching
     */
    private void checkRadioButton(String cType)
    {
        switch (cType) {
            case "S2T":
                ((RadioButton) findViewById(R.id.radioButtonS2T)).setChecked(true);
                break;

            case "S2HK":
                ((RadioButton) findViewById(R.id.radioButtonS2HK)).setChecked(true);
                break;

            case "S2TW":
                ((RadioButton) findViewById(R.id.radioButtonS2TW)).setChecked(true);
                break;

            case "S2TWP":
                ((RadioButton) findViewById(R.id.radioButtonS2TWP)).setChecked(true);
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        RadioButton rb = group.findViewById(checkedId);
        mHasChanges = true;

        if (null != rb) {
            switch (checkedId) {
                case R.id.radioButtonS2T:
                    mConversionType = ConversionType.S2T;
                    break;

                case R.id.radioButtonS2HK:
                    mConversionType = ConversionType.S2HK;
                    break;

                case R.id.radioButtonS2TW:
                    mConversionType = ConversionType.S2TW;
                    break;

                case R.id.radioButtonS2TWP:
                    mConversionType = ConversionType.S2TWP;
                    break;
            }
        }
    }

    /**
     * Save the user defined ConversionType setting.
     */
    private void updateS2TSelection(boolean hasChanges)
    {
        if (hasChanges) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(ContentView.PREF_CONVERSION_TYPE, mConversionType.toString());
            editor.apply();
        }

        Intent result = new Intent();
        result.putExtra(ContentView.EXTR_KEY_HAS_CHANGES, hasChanges);
        setResult(Activity.RESULT_OK, result);

        mHasChanges = false;
        finish();
    }

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void checkUnsavedChanges()
    {
        if (mHasChanges) {
            DialogActivity.showConfirmDialog(this,
                    R.string.gui_to_be_added,
                    R.string.gui_unsaved_changes,
                    R.string.gui_add_renew, new DialogActivity.DialogListener()
                    {
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            updateS2TSelection(true);
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog)
                        {
                            updateS2TSelection(false);
                            finish();
                        }
                    });
        }
        else {
            finish();
        }
    }
}
