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
package org.cog.hymnchtv.mediaconfig;

import static org.cog.hymnchtv.ContentHandler.MEDIA_BANZOU;
import static org.cog.hymnchtv.ContentHandler.MEDIA_CHANGSHI;
import static org.cog.hymnchtv.ContentHandler.MEDIA_JIAOCHANG;
import static org.cog.hymnchtv.ContentHandler.MEDIA_MEDIA;
import static org.cog.hymnchtv.MainActivity.ATTR_AUTO_PLAY;
import static org.cog.hymnchtv.MainActivity.ATTR_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_SELECT;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MediaType.HYMN_BANZOU;
import static org.cog.hymnchtv.MediaType.HYMN_CHANGSHI;
import static org.cog.hymnchtv.MediaType.HYMN_JIAOCHANG;
import static org.cog.hymnchtv.MediaType.HYMN_MEDIA;
import static org.cog.hymnchtv.MediaType.HYMN_URL;
import static org.cog.hymnchtv.mediaplayer.MediaExoPlayerFragment.ATTR_MEDIA_URL;
import static org.cog.hymnchtv.mediaplayer.MediaExoPlayerFragment.ATTR_MEDIA_URLS;
import static org.cog.hymnchtv.mediaplayer.YoutubePlayerFragment.URL_YOUTUBE;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.ContentHandler;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.RichTextEditor;
import org.cog.hymnchtv.mediaplayer.MediaExoPlayerFragment;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.utils.DialogActivity;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.cog.hymnchtv.utils.TimberLog;
import org.cog.hymnchtv.utils.TouchListener;
import org.cog.hymnchtv.utils.ViewUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * The class allows user to add own media content, either via:
 * a. a youtube url link etc via android share sheet (handle in MainActivity)
 * b. video or audio media file
 * and link it to a specific hymnType and hymnNo.
 *
 * It includes the capability to import, export or auto generate the export file
 * based on all the media files saved in a specified HymnType/MediaType sub-directory
 *
 * The format of the export record "," separated: hymnType, HymnNo, isFu, HymnMedia, urlLink, mediaUri
 * a. hymnType: HYMN_BB HYMN_DB, HYMN_ER, HYMN_XB
 * b. HymnNo: Hymn number
 * c. isFu: true if the hymnNo if Fu
 * d. HymnMedia: HYMN_BANZOU, HYMN_CHANGSHI, HYMN_JIAOCHANG, HYMN_MEDIA
 * e. urlLink: e.g. youtube or https url
 * f. mediaUri: Local media file uri (priority over urlLink if not null)
 *
 * @author Eng Chong Meng
 */
public class MediaConfig extends FragmentActivity
        implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemSelectedListener
{
    // Online text and video playback help contents
    private static final String HYMNCHTV_FAQ_UDC_RECORD = "https://cmeng-git.github.io/hymnchtv/faq.html#hymnch_0070";
    private static final ArrayList<String> videoUrls = new ArrayList<>();

    static {
        videoUrls.add("https:/cmeng-git.github.io/hymnchtv/video/mediaconfig_01.mp4");
        videoUrls.add("https:/cmeng-git.github.io/hymnchtv/video/mediaconfig_02.mp4");
    }

    public static final String HYMN_NO = "hymnNo";
    public static final String HYMN_FU = "isFu"; // set to 1 if fu else 0

    public static final String HYMN_TYPE = "hymnType";    // see HYMN_xxx: use for DB table name
    public static final String MEDIA_TYPE = "mediaType";  // see MEDIA_xxx
    public static final String MEDIA_URI = "mediaUri";    // set to null if none
    public static final String MEDIA_FILE_PATH = "mediaFilePath"; // set to null if none

    // The default directory when import_export files are being saved
    public static final String DIR_IMPORT_EXPORT = "import_export/";

    public static final String ATTR_MEDIA_URI = "attr_media_uri";

    /* Flag indicates if there were any uncommitted changes that should be saved on-exit */
    private boolean hasChanges = false;

    // Flag indicates the content is auto filled and may be overwritten when user changes the hymnNo ect.
    private boolean isAutoFilled = true;

    private Spinner hymnTypeSpinner;
    private Spinner mediaTypeSpinner;

    private CheckBox cbFu;
    private CheckBox cbOverwrite;
    public Button cmdAdd;

    private EditText tvUriDecode;
    private EditText tvHymnNo;
    private EditText tvMediaUri;
    private EditText tvImportFile;
    private View mFileView;

    // The EditText view to be filled onActivityResult upon user selection
    private TextView mViewRequest;

    // Focused view uses as indication to determine if the tvMediaUri is the auto filled or user entered
    private View mFocusedView = null;

    private MediaExoPlayerFragment mExoPlayer;
    private View mPlayerView;

    // DB based media record list view and last selected view
    private ListView mListView;

    public static List<String> hymnTypeEntry = new ArrayList<>();

    static {
        hymnTypeEntry.add("大本诗歌");
        hymnTypeEntry.add("补充本");
        hymnTypeEntry.add("新歌颂咏");
        hymnTypeEntry.add("儿童诗歌");
    }

    public static List<String> hymnTypeValue = new ArrayList<>();

    static {
        hymnTypeValue.add(HYMN_DB);
        hymnTypeValue.add(HYMN_BB);
        hymnTypeValue.add(HYMN_XB);
        hymnTypeValue.add(HYMN_ER);
    }

    public static List<String> mediaTypeEntry = new ArrayList<>();

    static {
        mediaTypeEntry.add("媒体");
        mediaTypeEntry.add("伴奏");
        mediaTypeEntry.add("教唱");
        mediaTypeEntry.add("唱诗");
        mediaTypeEntry.add("网页链接");
    }

    public static List<MediaType> mediaTypeValue = new ArrayList<>();

    static {
        mediaTypeValue.add(HYMN_MEDIA);
        mediaTypeValue.add(HYMN_BANZOU);
        mediaTypeValue.add(HYMN_JIAOCHANG);
        mediaTypeValue.add(HYMN_CHANGSHI);
        mediaTypeValue.add(HYMN_URL);
    }

    public static Map<MediaType, String> mediaDir = new HashMap<>();

    static {
        mediaDir.put(HYMN_MEDIA, MEDIA_MEDIA);
        mediaDir.put(HYMN_BANZOU, MEDIA_BANZOU);
        mediaDir.put(HYMN_JIAOCHANG, MEDIA_JIAOCHANG);
        mediaDir.put(HYMN_CHANGSHI, MEDIA_CHANGSHI);
    }

    private String mHymnType = hymnTypeValue.get(0);
    private MediaType mMediaType = mediaTypeValue.get(0);

    private String sHymnType = hymnTypeEntry.get(0);
    private String sMediaType = mediaTypeEntry.get(0);

    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_config);
        setTitle(R.string.gui_media_config);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<?> hymnTypeAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, hymnTypeEntry);
        // Specify the layout to use when the list of choices appears
        hymnTypeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        hymnTypeSpinner = findViewById(R.id.hymnType);
        hymnTypeSpinner.setAdapter(hymnTypeAdapter);

        // to avoid onSelectedItem get triggered on first init and only then init hymnTypeSpinner (else null)
        hymnTypeSpinner.setSelection(0, false);
        hymnTypeSpinner.setOnItemSelectedListener(this);

        // mHymnTypeSpinnerItem = hymnTypeSpinner.findViewById(R.id.textItem);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<?> mediaTypeAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, mediaTypeEntry);
        // Specify the layout to use when the list of choices appears
        mediaTypeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        mediaTypeSpinner = findViewById(R.id.mediaType);
        mediaTypeSpinner.setAdapter(mediaTypeAdapter);

        // to avoid onSelectedItem get triggered on first init and only then init mediaTypeSpinner (else null)
        mediaTypeSpinner.setSelection(0, false);
        mediaTypeSpinner.setOnItemSelectedListener(this);

        tvHymnNo = findViewById(R.id.hymnNo);
        tvHymnNo.addTextChangedListener(new MyTextWatcher(tvHymnNo));
        tvHymnNo.setOnFocusChangeListener(focusListener);

        findViewById(R.id.help_text).setOnClickListener(this);
        findViewById(R.id.help_video).setOnClickListener(this);
        tvUriDecode = findViewById(R.id.linkDecoded);
        tvMediaUri = findViewById(R.id.mediaUri);
        tvMediaUri.addTextChangedListener(new MyTextWatcher(tvMediaUri));
        tvMediaUri.setOnFocusChangeListener(focusListener);

        mFileView = findViewById(R.id.mediaMissing);
        mFileView.setVisibility(View.GONE);

        mViewRequest = tvMediaUri;
        cbFu = findViewById(R.id.cbFu);
        cbFu.setOnCheckedChangeListener((buttonView, isChecked) -> checkEntry());

        ActivityResultLauncher<String> mGetContent = getFileUri();
        /* The media uri is selected by user via file explorer */
        findViewById(R.id.browseMediaUri).setOnClickListener(view -> {
            mViewRequest = tvMediaUri;
            mGetContent.launch("*/*");
        });

        findViewById(R.id.shareMediaUri).setOnClickListener(this);
        findViewById(R.id.decodeUri).setOnClickListener(this);

        cmdAdd = findViewById(R.id.button_add);
        cmdAdd.setOnClickListener(this);

        findViewById(R.id.button_play).setOnClickListener(this);
        findViewById(R.id.button_delete).setOnClickListener(this);
        findViewById(R.id.button_Exit).setOnClickListener(this);

        /* The media uri may send in from mainActivity via android share */
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String mediaUri = bundle.getString(ATTR_MEDIA_URI);
            if (!TextUtils.isEmpty(mediaUri)) {
                isAutoFilled = false;
                tvMediaUri.setText(mediaUri);
                if (mediaUri.contains(".notion.site") || mediaUri.contains("mp.weixin.qq.com")) {
                    mediaTypeSpinner.setSelection(4);
                }
                else if (mediaUri.contains("youtube.com")
                        || mediaUri.contains("hymnal.net")) {
                    mediaTypeSpinner.setSelection(0);
                }
            }
        }

        // ======== database Media Record ==========
        mListView = findViewById(R.id.mrListView);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        tvImportFile = findViewById(R.id.importFile);
        cbOverwrite = findViewById(R.id.recordOverwrite);

        /* Use a file explorer to load in an import file */
        findViewById(R.id.browseImportFile).setOnClickListener(view -> {
            mViewRequest = tvImportFile;
            mGetContent.launch("*/*");
        });

        findViewById(R.id.editFile).setOnClickListener(this);
        findViewById(R.id.button_import).setOnClickListener(this);
        findViewById(R.id.button_import).setOnLongClickListener(this);

        Button btnNQ = findViewById(R.id.button_NQ);
        // btnNQ.setOnClickListener(this);
        // btnNQ.setOnLongClickListener(this);
        btnNQ.setOnTouchListener(touchListener);

        Button btnExport = findViewById(R.id.button_export);
        btnExport.setOnClickListener(this);
        btnExport.setOnLongClickListener(this);

        findViewById(R.id.button_import_create).setOnClickListener(this);
        findViewById(R.id.button_db_records).setOnClickListener(this);

        mPlayerView = findViewById(R.id.player_container);
        mPlayerView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.help_text:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(HYMNCHTV_FAQ_UDC_RECORD));
                startActivity(intent);
                break;

            case R.id.help_video:
                playVideoHelp();
                break;

            case R.id.shareMediaUri:
                shareMediaRecord();
                break;

            /* Decode the media uri, so it is user readable instead of %xx */
            case R.id.decodeUri:
                uriDecode();
                break;

            case R.id.button_add:
                if (updateMediaRecord()) {
                    Timber.d("Record saved successful: %s", tvMediaUri.getText());
                }
                break;

            // Manual deletion must be performed by user if the user modifies the link to point to different HymnType
            case R.id.button_delete:
                deleteMediaRecord();
                break;

            case R.id.button_play:
                startPlayOrActionView();
                break;

            case R.id.button_Exit:
                if (mListView.getVisibility() == View.GONE) {
                    checkUnsavedChanges();
                }
                else {
                    setTitle(R.string.gui_media_config);
                    mListView.setVisibility(View.GONE);
                }
                break;

            // use Rich Text Editor to modify or view the import file content
            case R.id.editFile:
                String filename = ViewUtil.toString(tvImportFile);
                if (filename != null)
                    editFile(filename);
                break;

            // Import to the DB database
            case R.id.button_import:
                Timber.d("import Media Records");
                importMediaRecords(null);
                break;

            case R.id.button_NQ:
                downloadNQRecord(1);
                break;

            // Export the database to a text file for sharing
            case R.id.button_export:
                createExportFile();
                break;

            // Auto creates an export file based on the sub-directory media files
            case R.id.button_import_create:
                createAutoImportFile();
                break;

            // Show the DB content for all user defined media link
            case R.id.button_db_records:
                showMediaRecords();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        // Export the database to a text file for sharing
        switch (v.getId()) {
            // Import to the DB database
            case R.id.button_import:
                Timber.d("import Media Records");
                importMediaRecords("notion_url_import.txt");
                return true;

            case R.id.button_NQ:
                downloadNQRecord(0);
                return true;

            case R.id.button_db_records:
                createExportLink();
                return true;
        }
        return false;
    }


    /**
     * TouchListener to support singleTap, doubleTap and longPress for the view for site visit
     */
    TouchListener touchListener = new TouchListener(HymnsApp.getGlobalContext())
    {
        @Override
        public boolean onSingleTap(View v, int idx)
        {
            if (v.getId() == R.id.button_NQ) {
                downloadNQRecord(1);
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(View v, int idx)
        {
            if (v.getId() == R.id.button_NQ) {
                downloadNQRecord(0);
            }
        }

        @Override
        public boolean onDoubleTap(View v, int idx)
        {
            if (v.getId() == R.id.button_NQ) {
                downloadNQRecord(2);
                return true;
            }
            return false;
        }
    };

    /**
     * This is activated by user; or automatic from mediaController when the downloaded uri is completed
     */
    private void startPlayOrActionView()
    {
        String mediaUrl = ViewUtil.toString(tvMediaUri);

        if ((mediaUrl != null) && (URLUtil.isValidUrl(mediaUrl) || new File(mediaUrl).exists())) {
            Uri uri = Uri.parse(mediaUrl);
            String mimeType = FileBackend.getMimeType(this, uri);
            boolean isMedia = !TextUtils.isEmpty(mimeType) && (mimeType.contains("video") || mimeType.contains("audio"));

            String hymnNo = ViewUtil.toString(tvHymnNo);
            boolean isFu = cbFu.isChecked();
            int nui = (hymnNo == null) ? -1 : HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);

            if (((nui != -1) && isMedia) || mediaUrl.matches(URL_YOUTUBE)) {
                // Get the user selected mediaType for playback
                SharedPreferences mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
                SharedPreferences.Editor mEditor = mSharedPref.edit();
                mEditor.putInt(PREF_MEDIA_HYMN, mMediaType.getValue());
                mEditor.apply();
                showContent(mHymnType, nui);
            }
            else {
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(uri, mimeType);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                PackageManager manager = getPackageManager();
                List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
                if (info.size() == 0) {
                    openIntent.setDataAndType(uri, "*/*");
                }
                try {
                    startActivity(openIntent);
                } catch (ActivityNotFoundException e) {
                    // showToastMessage(R.string.service_gui_FILE_OPEN_NO_APPLICATION);
                }
            }
        }
        else {
            HymnsApp.showToastMessage(R.string.gui_error_playback, "url is null or not found!");
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
        Intent intent = new Intent(this, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_SELECT, hymnType);
        bundle.putInt(ATTR_NUMBER, hymnNo);
        bundle.putBoolean(ATTR_AUTO_PLAY, true);

        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * A contract specifying that an activity can be called with an input of type I
     * and produce an output of type O
     *
     * @return an instant of ActivityResultLauncher<String>
     * @see ActivityResultCaller
     */
    private ActivityResultLauncher<String> getFileUri()
    {
        return registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) {
                HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);
            }
            else {
                File inFile = new File(FilePathHelper.getFilePath(this, uri));
                if (inFile.exists()) {
                    String filename = inFile.getPath();
                    if (mViewRequest == tvImportFile) {
                        filename = copyToLocalFile(filename);
                        editFile(filename);
                    }
                    else {
                        isAutoFilled = false;
                    }
                    mViewRequest.setText(filename);
                }
            }
        });
    }

    /**
     * Copy the user selected import_export file (content://) to the hymnchtv own import_export directory;
     * So it will be properly updated with user edited content.
     * The mediaUri will only moved to its final media directory when user performs add command
     *
     * @param uriPath the uri path returns by File(FilePathHelper.getFilePath(mContext, uri))
     * @return original uri path or newly copied uri path
     */
    private String copyToLocalFile(String uriPath)
    {
        if (uriPath.contains(FileBackend.TMP)) {
            File inFile = new File(uriPath);

            File outFile;
            if ((outFile = createFileIfNotExist(inFile.getName(), false)) == null) {
                return null;
            }

            try {
                if (inFile.renameTo(outFile)) {
                    uriPath = outFile.getAbsolutePath();
                }
            } catch (Exception e) {
                HymnsApp.showToastMessage(e.getMessage());
            }
        }
        return uriPath;
    }

    // ================= User entry events ================
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (parent == hymnTypeSpinner) {
            mHymnType = hymnTypeValue.get(position);
            sHymnType = hymnTypeEntry.get(position);
            if (mListView.getVisibility() == View.VISIBLE) {
                showMediaRecords();
            }
        }
        else if (parent == mediaTypeSpinner) {
            mMediaType = mediaTypeValue.get(position);
            sMediaType = mediaTypeEntry.get(position);
        }
        checkEntry();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }

    private final View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
        if (hasFocus) {
            mFocusedView = v;
        }
        else {
            mFocusedView = null;
        }
    };

    private class MyTextWatcher implements TextWatcher
    {
        private final EditText mEditText;

        public MyTextWatcher(EditText editText)
        {
            mEditText = editText;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
            if (tvMediaUri.equals(mEditText) && tvMediaUri.equals(mFocusedView)) {
                Timber.d("AutoFilled set to false");
                isAutoFilled = false;
            }
            else if (tvHymnNo.equals(mEditText) && tvHymnNo.equals(mFocusedView)) {
                checkEntry();
            }
        }

        public void afterTextChanged(Editable s)
        {
            hasChanges = true;
        }
    }

    // ================= Need user confirmation before exit if changes detected ================

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void checkUnsavedChanges()
    {
        if (hasChanges && !isAutoFilled) {
            DialogActivity.showConfirmDialog(this,
                    R.string.gui_to_be_added,
                    R.string.gui_unsaved_changes,
                    R.string.gui_add, new DialogActivity.DialogListener()
                    {
                        /**
                         * Fired when user clicks the dialog's the confirm button.
                         *
                         * @param dialog source <tt>DialogActivity</tt>.
                         */
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            return cmdAdd.performClick();
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
                    });
        }
        else {
            finish();
        }
    }

    // ================= Media Record Handlers ================

    /**
     * Update user entry to the DB media record, get user confirmation if there is an existing record present
     *
     * @return true if update is successful
     */
    private boolean updateMediaRecord()
    {
        final MediaRecord mRecord = createMediaRecord();
        if (mRecord != null) {
            if (!checkMediaAvailable(mRecord)) {
                return false;
            }

            if (mDB.getMediaRecord(mRecord, false)) {
                DialogActivity.showConfirmDialog(this,
                        R.string.gui_to_be_added,
                        R.string.gui_db_overwrite_media,
                        R.string.gui_overwrite, new DialogActivity.DialogListener()
                        {
                            /**
                             * Fired when user clicks the dialog's confirm button.
                             *
                             * @param dialog source <tt>DialogActivity</tt>.
                             */
                            public boolean onConfirmClicked(DialogActivity dialog)
                            {
                                return saveMediaRecord(mRecord);
                            }

                            /**
                             * Fired when user dismisses the dialog.
                             *
                             * @param dialog source <tt>DialogActivity</tt>
                             */
                            public void onDialogCancelled(DialogActivity dialog)
                            {
                            }
                        });
            }
            else {
                return saveMediaRecord(mRecord);
            }
        }
        return false;
    }

    private boolean checkMediaAvailable(MediaRecord mRecord)
    {
        String mediaFile;
        if (mRecord != null) {
            if (!TextUtils.isEmpty(mediaFile = mRecord.getMediaFilePath())) {
                File mFile = new File(mediaFile);
                if (!mFile.exists()) {
                    mFileView.setVisibility(View.VISIBLE);
                    return false;
                }
            }
        }
        mFileView.setVisibility(View.GONE);
        return true;
    }

    /**
     * create the MediaRecord based on user input parameters
     *
     * @return return the newly created mediaRecord, null otherwise
     */
    private MediaRecord createMediaRecord()
    {
        MediaRecord mediaRecord;
        String uriPath = ViewUtil.toString(tvMediaUri);

        if ((mediaRecord = MediaRecord.toRecord(uriPath)) == null) {
            String hymnNo = ViewUtil.toString(tvHymnNo);
            boolean isUrlLink = URLUtil.isValidUrl(uriPath);
            if (!isUrlLink && (uriPath != null) && !new File(uriPath).exists()) {
                uriPath = null;
            }

            if ((hymnNo == null) || (uriPath == null)) {
                HymnsApp.showToastMessage(R.string.gui_error_hymn_config);
                return null;
            }
            boolean isFu = cbFu.isChecked();
            int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
            if (nui != -1) {
                String filePath = null;
                if (!isUrlLink) {
                    filePath = uriPath;
                    uriPath = null;
                }
                mediaRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType, uriPath, filePath);
            }
        }
        return mediaRecord;
    }

    /**
     * Save the user entry to the DB media record on user confirmation
     *
     * @return true is successfully
     */
    private boolean saveMediaRecord(MediaRecord mRecord)
    {
        String filePath = mRecord.getMediaFilePath();
        boolean isSuccess = true;

        if (filePath != null) {
            if (filePath.contains(FileBackend.TMP)) {
                File inFile = new File(filePath);

                File subDir = FileBackend.getHymnchtvStore(mHymnType + mediaDir.get(mMediaType), true);
                if (subDir == null) {
                    HymnsApp.showToastMessage(R.string.gui_file_ACCESS_NO_PERMISSION);
                    isSuccess = false;
                }
                File outFile = new File(subDir, inFile.getName());

                try {
                    // return false if inFile rename failed && outFile does not exist
                    if (!inFile.renameTo(outFile) && !outFile.exists()) {
                        HymnsApp.showToastMessage(R.string.gui_add_to_db_failed);
                        isSuccess = false;
                    }

                    filePath = outFile.getAbsolutePath();
                    mRecord.setFilePath(filePath);
                    mRecord.setMediaUri(null);
                } catch (Exception e) {
                    HymnsApp.showToastMessage(e.getMessage());
                    isSuccess = false;
                }
            }
        }

        if (isSuccess) {
            mDB.storeMediaRecord(mRecord);
            HymnsApp.showToastMessage(R.string.gui_add_to_db);
        }
        else {
            HymnsApp.showToastMessage(R.string.gui_add_to_db_failed);
        }
        return isSuccess;
    }

    /**
     * Delete user selected media record on user confirmation.
     * User must manually deleted the old media record if user changes uri to point to another HymnType
     */
    private void deleteMediaRecord()
    {
        String hymnNo = ViewUtil.toString(tvHymnNo);
        if (hymnNo == null) {
            HymnsApp.showToastMessage(R.string.gui_error_hymn_config);
            return;
        }

        boolean isFu = cbFu.isChecked();
        int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
        if (nui != -1) {
            Bundle args = new Bundle();
            args.putString(MediaRecordDeleteFragment.ARG_MESSAGE,
                    getString(R.string.gui_delete_media, mHymnType, nui, mMediaType));
            String title = getString(R.string.gui_delete);

            // Displays the media record and content delete dialog and waits for user confirmation
            DialogActivity.showCustomDialog(this, title, MediaRecordDeleteFragment.class.getName(),
                    args, getString(R.string.gui_delete), new DialogActivity.DialogListener()
                    {
                        @Override
                        public boolean onConfirmClicked(DialogActivity dialog)
                        {
                            MediaRecord mRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType);
                            CheckBox cbMediaDelete = dialog.findViewById(R.id.cb_media_delete);

                            if (cbMediaDelete.isChecked()) {
                                mDB.getMediaRecord(mRecord, true);
                                String filePath = mRecord.getMediaFilePath();
                                if (filePath != null) {
                                    File mediaFile = new File(filePath);
                                    if (mediaFile.exists() && !mediaFile.delete()) {
                                        Timber.w(getString(R.string.gui_delete_media_failed,
                                                mHymnType, nui));
                                    }
                                }
                            }
                            int row = mDB.deleteMediaRecord(mRecord);
                            HymnsApp.showToastMessage(R.string.gui_delete_media_ok, (row != 0) ? nui : 0);
                            checkEntry();
                            return true;
                        }

                        @Override
                        public void onDialogCancelled(DialogActivity dialog)
                        {
                        }
                    }, null);
        }
    }

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void shareMediaRecord()
    {
        ArrayList<Uri> imageUris = new ArrayList<>();
        String importFile = ViewUtil.toString(tvImportFile);

        if (importFile != null) {
            imageUris.add(FileBackend.getUriForFile(this, new File(importFile)));
            ShareWith.share(this, null, imageUris);
            return;
        }

        final MediaRecord mRecord = createMediaRecord();
        if (mRecord != null) {
            String mediaFile = mRecord.getMediaFilePath();
            if (!TextUtils.isEmpty(mediaFile)) {
                File mFile = new File(mediaFile);
                if (mFile.exists()) {
                    imageUris.add(FileBackend.getUriForFile(this, new File(mediaFile)));
                }
                else {
                    HymnsApp.showToastMessage(R.string.gui_share_file_missing,
                            mediaFile.substring(mediaFile.indexOf("Download")));
                }
            }
            ShareWith.share(this, mRecord.toExportString(), imageUris);
        }
    }

    /**
     * Decode the url path so that it is user readable i.e. convert all %xx to actual text
     */
    private void uriDecode()
    {
        if (tvMediaUri.getVisibility() == View.GONE) {
            tvMediaUri.setVisibility(View.VISIBLE);
            tvUriDecode.setVisibility(View.GONE);
        }
        else {
            String mediaUri = ViewUtil.toString(tvMediaUri);
            if (mediaUri != null) {
                tvMediaUri.setVisibility(View.GONE);
                tvUriDecode.setVisibility(View.VISIBLE);
                try {
                    tvUriDecode.setText(URLDecoder.decode(mediaUri, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    Timber.w("URI decode exception: %s", e.getMessage());
                }
            }
        }
    }

    /**
     * Launch the Rich Text Editor activity for user to view, changes and saves
     *
     * @param fileName the import_export file name
     */
    private void editFile(String fileName)
    {
        if (fileName != null) {
            Intent intent = new Intent(this, RichTextEditor.class);
            Bundle bundle = new Bundle();
            bundle.putString(RichTextEditor.ATTR_FILE_URI, fileName);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    /**
     * Check any saved entry in DB based on user input. Show the DB content if found
     */
    private void checkEntry()
    {
        if (!isAutoFilled) {
            Timber.d("AutoFilled is false");
            return;
        }

        String hymnNo = ViewUtil.toString(tvHymnNo);
        if (hymnNo == null)
            return;

        boolean isFu = cbFu.isChecked();
        int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
        if (nui == -1)
            return;

        MediaRecord mediaRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType);
        if (mDB.getMediaRecord(mediaRecord, true)) {
            String uriPath = mediaRecord.getMediaFilePath();
            if (TextUtils.isEmpty(uriPath)) {
                uriPath = mediaRecord.getMediaUri();
            }

            if (!TextUtils.isEmpty(uriPath)) {
                // show the mediaUri EditText view
                if (tvMediaUri.getVisibility() == View.GONE) {
                    tvMediaUri.setVisibility(View.VISIBLE);
                    tvUriDecode.setVisibility(View.GONE);
                }

                // force focus to tvHymnNo so isAutoFilled cannot be accidentally cleared
                tvHymnNo.requestFocus();
                tvMediaUri.setText(uriPath);
                return;
            }
        }
        tvMediaUri.setText("");
    }

    // =============================== MediaRecord Database Handler ============================

    /**
     * Wait for user confirmation before proceed with QQ links download
     *
     * @param mode flag for either Notion (1 / 2) or QQ (0) site hymn link records fetch
     */
    private void downloadNQRecord(final int mode)
    {
        DialogActivity.showConfirmDialog(this,
                R.string.gui_nq_download,
                R.string.gui_nq_download_proceed,
                R.string.gui_download, new DialogActivity.DialogListener()
                {
                    public boolean onConfirmClicked(DialogActivity dialog)
                    {
                        if (mode == 0) {
                            QQRecord.fetchQQLinks(MediaConfig.this);
                        }
                        else if (mode == 1) {
                            NotionRecord.fetchNotionLinks(MediaConfig.this);
                        }
                        else if (mode == 2) {
                            NotionRecordScrape.fetchNotionLinks(MediaConfig.this);
                        }
                        return true;
                    }

                    public void onDialogCancelled(DialogActivity dialog)
                    {
                    }
                }, (mode != 0) ? "Notion" : "QQ");
    }

    /**
     * Import the media records into the database based on import file info
     */
    private void importMediaRecords(String assetFile)
    {
        String importFile = ViewUtil.toString(tvImportFile);
        if (importFile == null && assetFile == null) {
            HymnsApp.showToastMessage(R.string.gui_error_hymn_config);
            return;
        }

        HymnsApp.showToastMessage(R.string.gui_db_import_start);
        int record = 0;
        InputStream in2;
        try {
            if (importFile != null) {
                in2 = new FileInputStream(importFile);
            }
            else {
                in2 = HymnsApp.getGlobalContext().getResources().getAssets().open(assetFile);
            }

            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            boolean isOverWrite = cbOverwrite.isChecked(); //|| (assetFile != null);
            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            for (String mRecord : mList) {
                MediaRecord mediaRecord = MediaRecord.toRecord(mRecord);
                if (mediaRecord == null)
                    continue;

                boolean isFu = mediaRecord.isFu();
                int hymnNo = isFu ? (mediaRecord.getHymnNo() - HYMN_DB_NO_MAX) : mediaRecord.getHymnNo();
                int nui = HymnNoValidate.validateHymnNo(mediaRecord.getHymnType(), hymnNo, isFu);
                if ((nui != -1) && (isOverWrite || !mDB.getMediaRecord(mediaRecord, false))) {
                    mDB.storeMediaRecord(mediaRecord);
                    record++;
                }
                if (TimberLog.isFinestEnable)
                    Timber.d("Import media record: %s; %s(%s); %s", nui, hymnNo, record, mRecord);
            }
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
        }
        HymnsApp.showToastMessage(R.string.gui_db_import_record, record);
    }

    /**
     * Export the media records in database for all the HymnType and
     * filename tagged with timeStamp e.g hymnsAll-20201212_092033
     */
    private void createExportFile()
    {
        String fileName = String.format("hymnsAll-%s.txt",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File exportFile = createFileIfNotExist(fileName, true);
        if (exportFile != null && exportFile.exists()) {
            int recordSize = 0;
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(exportFile.getAbsolutePath());
                for (String hymnType : hymnTypeValue) {
                    List<MediaRecord> mediaRecords = mDB.getMediaRecords(hymnType);
                    if (!mediaRecords.isEmpty()) {
                        recordSize += mediaRecords.size();
                        for (MediaRecord mediaRecord : mediaRecords) {
                            String mRecord = mediaRecord.toExportString();
                            if (mRecord != null)
                                fileWriter.write(mRecord);
                        }
                    }
                }
                fileWriter.close();

                if (recordSize != 0) {
                    HymnsApp.showToastMessage(R.string.hymn_match, recordSize);
                    tvImportFile.setText(exportFile.getPath());
                    editFile(exportFile.getPath());
                }
                else {
                    HymnsApp.showToastMessage(R.string.hymn_match_none);
                }
            } catch (IOException e) {
                Timber.e("Export media record exception: %s", e.getMessage());
            }
        }
    }

    /**
     * Export the media records in database for the current selected mHymnType and
     * filename tagged with timeStamp e.g hymn_db-20201212_092033
     */
    private void createExportLink()
    {
        String fileName = String.format("hymnsLink-%s.txt",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File exportFile = createFileIfNotExist(fileName, true);
        if (exportFile != null && exportFile.exists()) {
            int recordSize = 0;
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(exportFile.getAbsolutePath());
                for (String hymnType : hymnTypeValue) {
                    List<MediaRecord> mediaRecords = mDB.getMediaLinks(hymnType);
                    if (!mediaRecords.isEmpty()) {
                        recordSize += mediaRecords.size();
                        for (MediaRecord mediaRecord : mediaRecords) {
                            String mRecord = mediaRecord.toExportString();
                            if (mRecord != null)
                                fileWriter.write(mRecord);
                        }
                    }
                }
                fileWriter.close();

                if (recordSize != 0) {
                    HymnsApp.showToastMessage(R.string.hymn_match, recordSize);
                    tvImportFile.setText(exportFile.getPath());
                    editFile(exportFile.getPath());
                }
                else {
                    HymnsApp.showToastMessage(R.string.hymn_match_none);
                }
            } catch (IOException e) {
                Timber.e("Export media record exception: %s", e.getMessage());
            }
        }
    }

    /**
     * Auto creates the export file basing on all the media files reside in the MEDIA_MEDIA directory:
     * a. mHymnType selected is used as the DB Table
     * b. The only medias supported are in sub-dir: mHymnType + MEDIA_MEDIA
     * c. The hymnNo is extracted from the fileName (without the ext), default to "0000" if none
     * d. User must review and edit the generated text file to update Fu hymn
     * <p>
     * Export filename tagged with timeStamp e.g hymn_db-20201212_092033
     */
    private void createAutoImportFile()
    {
        String mediaRecord;
        String fileName = String.format("%s-%s.txt", mHymnType,
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File importFile = createFileIfNotExist(fileName, true);
        if (importFile != null && importFile.exists()) {
            File srcPath = FileBackend.getHymnchtvStore(mHymnType + mediaDir.get(mMediaType), false);
            if ((srcPath != null) && srcPath.isDirectory()) {
                File[] files = srcPath.listFiles();
                if ((files != null) && (files.length > 0)) {
                    // Sort filename in ascending order i.e. so the export file has its hymnNo in ascending order
                    Arrays.sort(files);
                    try {
                        FileWriter fileWriter = new FileWriter(importFile.getAbsolutePath());
                        for (File file : files) {

                            // Extract the hymnNo from the fileName with ext stripped
                            String mFileName = file.getName();
                            int idx = mFileName.lastIndexOf(".");
                            if (idx != -1)
                                mFileName = mFileName.substring(0, idx);
                            String hymnNo = mFileName.replaceAll("\\D*", "");

                            if (TextUtils.isEmpty(hymnNo)) {
                                hymnNo = "0000";
                            }
                            mediaRecord = String.format(Locale.CHINA, "%s,%s,%d,%s,%s,%s\r\n",
                                    mHymnType, hymnNo, 0, mMediaType, null, file.getAbsolutePath());
                            fileWriter.write(mediaRecord);
                        }
                        fileWriter.close();
                        HymnsApp.showToastMessage(R.string.hymn_match_auto_create, sHymnType, sMediaType, files.length);

                        tvImportFile.setText(importFile.getPath());
                        editFile(importFile.getPath());
                    } catch (IOException e) {
                        Timber.e("Create Import File exception: %s", e.getMessage());
                    }
                }
                else {
                    HymnsApp.showToastMessage(R.string.hymn_match_none);
                }
            }
            else {
                HymnsApp.showToastMessage(R.string.hymn_match_none);
            }
        }
    }

    /**
     * Created a file with the give fileName in the DIR_IMPORT_EXPORT if permitted, and file does not exist
     * if createNew is true; else just return the filePath
     *
     * @param fileName of the created file
     * @return File path of the new file or null if failed
     */
    private File createFileIfNotExist(String fileName, boolean createNew)
    {
        File subDir = FileBackend.getHymnchtvStore(DIR_IMPORT_EXPORT, true);
        if (subDir == null) {
            HymnsApp.showToastMessage(R.string.gui_file_ACCESS_NO_PERMISSION);
            return null;
        }

        File filePath = TextUtils.isEmpty(fileName) ? subDir : new File(subDir, fileName);
        try {
            if (createNew && (filePath.exists() || filePath.createNewFile())) {
                return filePath;
            }
        } catch (IOException e) {
            Timber.e("Failed to create media export file: %s", e.getMessage());
        }
        return filePath;
    }

    /**
     * Show all the DB media records in the DB based on user selected HymnType
     */
    @SuppressLint("ClickableViewAccessibility")
    private void showMediaRecords()
    {
        List<MediaRecord> mediaRecords = mDB.getMediaRecords(mHymnType);
        if (mediaRecords.isEmpty()) {
            HymnsApp.showToastMessage(R.string.hymn_match_none);
            return;
        }
        setTitle(getString(R.string.hymn_match, mediaRecords.size()));

        final List<Map<String, String>> data = new ArrayList<>();
        for (MediaRecord mediaRecord : mediaRecords) {
            Map<String, String> item_db = new HashMap<>();
            item_db.put("match", mediaRecord.toString());
            data.add(item_db);
        }

        /* Display the search result to the user */
        SimpleListAdapter mediaAdapter = new SimpleListAdapter(this, data, R.layout.media_records_list,
                new String[]{"match"}, new int[]{R.id.item_record});

        mListView.setAdapter(mediaAdapter);
        mListView.setVisibility(View.VISIBLE);

        // Update the media Record Editor info.
        mListView.setOnItemClickListener((adapterView, view, pos, id) -> {
            mediaAdapter.setSelectItem(pos, view);

            if (isAutoFilled) {
                String qqEntry = data.get(pos).get("match");
                MediaRecord mRecord = MediaRecord.toRecord(qqEntry);
                if (mRecord != null) {
                    String hymnType = mRecord.getHymnType();
                    for (int i = 0; i < hymnTypeValue.size(); i++) {
                        if (hymnTypeValue.get(i).equals(hymnType)) {
                            hymnTypeSpinner.setSelection(i);
                            break;
                        }
                    }
                    mediaTypeSpinner.setSelection(mRecord.getMediaType().getValue());
                    cbFu.setChecked(mRecord.isFu());

                    int hymnNo = mRecord.getHymnNo();
                    tvHymnNo.setText(Integer.toString(hymnNo));

                    // force focus to tvHymnNo so isAutoFilled cannot be accidentally cleared
                    tvHymnNo.requestFocus();

                    String mediaPath = mRecord.getMediaUri();
                    if (!TextUtils.isEmpty(mRecord.getMediaFilePath())) {
                        mediaPath = mRecord.getMediaFilePath().replace(MediaRecord.DOWNLOAD_DIR, MediaRecord.DOWNLOAD_FP);
                    }
                    tvMediaUri.setText(mediaPath);
                }
            }
        });
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mPlayerView.getVisibility() == View.VISIBLE) {
                releasePlayer();
                mPlayerView.setVisibility(View.GONE);
            }
            else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use for playing the help video content only
     */
    private void playVideoHelp()
    {
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_MEDIA_URL, null);
        bundle.putStringArrayList(ATTR_MEDIA_URLS, videoUrls);

        mPlayerView.setVisibility(View.VISIBLE);
        mExoPlayer = MediaExoPlayerFragment.getInstance(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.player_container, mExoPlayer)
                .addToBackStack(null)
                .commit();
    }

    private void releasePlayer()
    {
        if (mExoPlayer != null) {
            mExoPlayer.releasePlayer();
        }
    }
}
