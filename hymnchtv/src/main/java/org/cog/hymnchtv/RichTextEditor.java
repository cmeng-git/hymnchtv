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

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.utils.DialogActivity;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import jp.wasabeef.richeditor.RichEditor;
import timber.log.Timber;

/**
 * Rich text editor implementation based on jp.wasabeef:richeditor-android
 * for import_export file: view, make changes and save to apply
 *
 * @author Eng Chong Meng
 */
public class RichTextEditor extends FragmentActivity
        implements View.OnClickListener, DialogActivity.DialogListener
{
    // Tags for the onSaveInstanceState bundle.
    public static final String ATTR_FILE_URI = "attr_file_uUri";
    private static final String URI_CONTENT = "uri_content";

    /* filename under edit*/
    private String fileUri = null;

    /* Flag indicates if there were any uncommitted changes that shall be applied on exit */
    private static boolean hasChanges = false;

    private RichEditor mEditor;
    private TextView mPreview;
    private Button cmdSave;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rich_text_editor);
        mEditor = (RichEditor) findViewById(R.id.editor);
        mEditor.setEditorHeight(200);
        mEditor.setEditorFontSize(15);
        mEditor.setEditorFontColor(Color.DKGRAY);
        //mEditor.setEditorBackgroundColor(Color.BLUE);
        //mEditor.setBackgroundColor(Color.BLUE);
        //mEditor.setBackgroundResource(R.drawable.bg);
        //mEditor.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        mEditor.setPadding(10, 10, 10, 10);
        mEditor.setPlaceholder("Insert text here...");
        //mEditor.setInputEnabled(false);

        mPreview = (TextView) findViewById(R.id.preview);
        mEditor.setOnTextChangeListener(text -> {
            hasChanges = true;
            mPreview.setText(text);
        });

        findViewById(R.id.action_undo).setOnClickListener(v -> mEditor.undo());
        findViewById(R.id.action_redo).setOnClickListener(v -> mEditor.redo());

        findViewById(R.id.action_bold).setOnClickListener(v -> mEditor.setBold());
        findViewById(R.id.action_italic).setOnClickListener(v -> mEditor.setItalic());
        findViewById(R.id.action_subscript).setOnClickListener(v -> mEditor.setSubscript());
        findViewById(R.id.action_superscript).setOnClickListener(v -> mEditor.setSuperscript());
        findViewById(R.id.action_strikethrough).setOnClickListener(v -> mEditor.setStrikeThrough());
        findViewById(R.id.action_underline).setOnClickListener(v -> mEditor.setUnderline());

        findViewById(R.id.action_heading1).setOnClickListener(v -> mEditor.setHeading(1));
        findViewById(R.id.action_heading2).setOnClickListener(v -> mEditor.setHeading(2));
        findViewById(R.id.action_heading3).setOnClickListener(v -> mEditor.setHeading(3));
        findViewById(R.id.action_heading4).setOnClickListener(v -> mEditor.setHeading(4));
        findViewById(R.id.action_heading5).setOnClickListener(v -> mEditor.setHeading(5));
        findViewById(R.id.action_heading6).setOnClickListener(v -> mEditor.setHeading(6));

        findViewById(R.id.action_txt_color).setOnClickListener(new View.OnClickListener()
        {
            private boolean isChanged;

            @Override
            public void onClick(View v)
            {
                mEditor.setTextColor(isChanged ? Color.BLACK : Color.RED);
                isChanged = !isChanged;
            }
        });

        findViewById(R.id.action_bg_color).setOnClickListener(new View.OnClickListener()
        {
            private boolean isChanged;

            @Override
            public void onClick(View v)
            {
                mEditor.setTextBackgroundColor(isChanged ? Color.TRANSPARENT : Color.YELLOW);
                isChanged = !isChanged;
            }
        });

        findViewById(R.id.action_indent).setOnClickListener(v -> mEditor.setIndent());
        findViewById(R.id.action_outdent).setOnClickListener(v -> mEditor.setOutdent());

        findViewById(R.id.action_align_left).setOnClickListener(v -> mEditor.setAlignLeft());
        findViewById(R.id.action_align_center).setOnClickListener(v -> mEditor.setAlignCenter());
        findViewById(R.id.action_align_right).setOnClickListener(v -> mEditor.setAlignRight());

        findViewById(R.id.action_blockquote).setOnClickListener(v -> mEditor.setBlockquote());
        findViewById(R.id.action_insert_bullets).setOnClickListener(v -> mEditor.setBullets());
        findViewById(R.id.action_insert_numbers).setOnClickListener(v -> mEditor.setNumbers());

        findViewById(R.id.action_insert_image).setOnClickListener(
                v -> mEditor.insertImage("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg", "dachshund", 320));

        findViewById(R.id.action_insert_youtube).setOnClickListener(
                v -> mEditor.insertYoutubeVideo("https://www.youtube.com/embed/pS5peqApgUA"));

        findViewById(R.id.action_insert_audio).setOnClickListener(
                v -> mEditor.insertAudio("https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_5MG.mp3"));

        findViewById(R.id.action_insert_video).setOnClickListener(
                v -> mEditor.insertVideo("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4", 360));

        findViewById(R.id.action_insert_link).setOnClickListener(
                v -> mEditor.insertLink("https://github.com/wasabeef", "wasabeef"));
        findViewById(R.id.action_insert_checkbox).setOnClickListener(v -> mEditor.insertTodo());

        (cmdSave = findViewById(R.id.saveButton)).setOnClickListener(this);
        findViewById(R.id.endButton).setOnClickListener(this);

        /* updte the content text for edit either via savedInstanceState on screen rotate or given file uri on first entry */
        if (savedInstanceState != null) {
            fileUri = savedInstanceState.getString(ATTR_FILE_URI);
            mEditor.setHtml(savedInstanceState.getString(URI_CONTENT));
        }
        else {
            hasChanges = false;
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                fileUri = bundle.getString(ATTR_FILE_URI);
                if (!TextUtils.isEmpty(fileUri)) {
                    editFile(fileUri);
                }
            }
        }
    }

    /**
     * Save a copy of the current edited fileName and its content to the instance state bundle.
     *
     * @param outState Bundle
     */
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(ATTR_FILE_URI, fileUri);
        outState.putString(URI_CONTENT, mEditor.getHtml());
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.saveButton:
                if (hasChanges) {
                    saveFile();
                }
                finish();
                break;

            case R.id.endButton:
                checkUnsavedChanges();
                break;
        }
    }

    /**
     * check for any unsaved changes and alert user
     */
    private void checkUnsavedChanges()
    {
        if (hasChanges) {
            DialogActivity.showConfirmDialog(this,
                    R.string.gui_to_be_added,
                    R.string.gui_unsaved_changes,
                    R.string.gui_add, this);
        }
        else {
            finish();
        }
    }

    /**
     * Fired when user clicks the dialog's confirm button.
     *
     * @param dialog source <tt>DialogActivity</tt>.
     */
    public boolean onConfirmClicked(DialogActivity dialog)
    {
        return cmdSave.performClick();
    }

    /**
     * Fired when user dismisses the dialog.
     *
     * @param dialog source <tt>DialogActivity</tt>
     */
    public void onDialogCancelled(DialogActivity dialog)
    {
        finish();
    }

    /**
     * Extract the export file content for edit in RichText Editor
     * @param fileName The file for editing
     */
    private void editFile(String fileName)
    {
        try {
            InputStream in2 = new FileInputStream(fileName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            mEditor.setHtml(mResult);

        } catch (IOException e) {
            Timber.w("Content file not available: %s", e.getMessage());
            HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);
        }
    }

    /**
     * Save the changed file content to its original given fileName
     */
    private void saveFile()
    {
        try {
            File outFile = new File(fileUri);
            FileWriter fileWriter = new FileWriter(outFile.getAbsolutePath());
            fileWriter.write(mEditor.getHtml());
            fileWriter.close();
            HymnsApp.showToastMessage(R.string.gui_file_saved);
        } catch (IOException e) {
            Timber.w("Save file exception: %s", e.getMessage());
        }
    }
}