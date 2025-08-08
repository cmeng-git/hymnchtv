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
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_TYPE;
import static org.cog.hymnchtv.MainActivity.ATTR_MEDIA_URI;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MediaType.HYMN_BANZOU;
import static org.cog.hymnchtv.MediaType.HYMN_CHANGSHI;
import static org.cog.hymnchtv.MediaType.HYMN_JIAOCHANG;
import static org.cog.hymnchtv.MediaType.HYMN_MEDIA;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.BaseActivity;
import org.cog.hymnchtv.ContentHandler;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
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
import org.cog.hymnchtv.utils.ViewUtil;

import timber.log.Timber;

/**
 * The class allows user to add own media content, either via:
 * a. a youtube url link etc via android share sheet (handle in MainActivity)
 * b. video or audio media file
 * and link it to a specific hymnType and hymnNo.
 * <p>
 * It includes the capability to import url links from file, and export db url links to a file for sharing.
 * <p>
 * The format of the export record "," separated: hymnType, HymnNo, isFu, HymnMedia, urlLink, mediaUri
 * a. hymnType: HYMN_BB HYMN_DB, HYMN_ER, HYMN_XB, HYMN_XG, HYMN_YB
 * b. HymnNo: Hymn number
 * c. isFu: true if the hymnNo if Fu
 * d. HymnMedia: HYMN_MEDIA, HYMN_JIAOCHANG, HYMN_CHANGSHI, and HYMN_BANZOU,
 * e. urlLink: e.g. youtube or https url
 * f. mediaUri: Local media file uri (priority over urlLink if not null) (not use since v2.2.0)
 *
 * @author Eng Chong Meng
 */
public class MediaConfig extends BaseActivity
        implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemSelectedListener {
    // Online text and video playback help contents
    private static final String HYMNCHTV_FAQ_UDC_RECORD = "https://cmeng-git.github.io/hymnchtv/faq.html#hymnch_0070";
    private static final ArrayList<String> videoUrls = new ArrayList<String>() {{
        add("https:/cmeng-git.github.io/hymnchtv/video/mediaconfig_yt_search.mp4");
        add("https:/cmeng-git.github.io/hymnchtv/video/mediaconfig_url_export.mp4");
    }};

    public static final String HYMN_NO = "hymnNo";
    public static final String HYMN_FU = "isFu"; // set to 1 if fu else 0

    public static final String HYMN_TYPE = "hymnType";    // see HYMN_xxx: use for DB table name
    public static final String MEDIA_TYPE = "mediaType";  // see MEDIA_xxx
    public static final String MEDIA_URI = "mediaUri";    // set to null if none
    public static final String MEDIA_FILE_PATH = "mediaFilePath"; // set to null if none

    // The asset Url import file name
    public static final String ASSET_URL_IMPORT_FILE = "url_import.txt";
    // Import Url version preference parameter
    public static String PREF_VERSION_URL = "VersionUrlImport";

    /*
     * current default url_import version; always set the value to - 1.
     * This is to force a newly install apk will update the DB from asset file on first launch.
     * Increase the build.gradle versionImport value if there is url_import file released.
     */
    public static final int URL_IMPORT_VERSION = -1;

    // The default directory when import_export files are being saved
    public static final String DIR_IMPORT_EXPORT = "import_export/";

    /* Flag indicates if there were any uncommitted changes that should be saved on-exit */
    private boolean hasChanges = false;

    // Indicate this instance is trigger from share from
    private boolean mShare = false;

    // Flag indicates the content is auto filled and may be overwritten when user changes the hymnNo ect.
    private boolean isAutoFilled = true;
    private int mVisibleItem = 0;

    private Spinner hymnTypeSpinner;
    private Spinner mediaTypeSpinner;

    private CheckBox cbFu;
    private CheckBox cbOverwrite;
    public Button cmdAdd;
    public Button btnNQ;

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
        hymnTypeEntry.add("新詩歌本");
        hymnTypeEntry.add("青年诗歌 ");
        hymnTypeEntry.add("儿童诗歌");
    }

    public static List<String> hymnTypeValue = new ArrayList<>();

    static {
        hymnTypeValue.add(HYMN_DB);
        hymnTypeValue.add(HYMN_BB);
        hymnTypeValue.add(HYMN_XB);
        hymnTypeValue.add(HYMN_XG);
        hymnTypeValue.add(HYMN_YB);
        hymnTypeValue.add(HYMN_ER);
    }

    public static List<String> mediaTypeEntry = new ArrayList<>();

    static {
        mediaTypeEntry.add("媒体");
        mediaTypeEntry.add("教唱");
        mediaTypeEntry.add("唱诗");
        mediaTypeEntry.add("伴奏");
    }

    public static List<MediaType> mediaTypeValue = new ArrayList<>();

    static {
        mediaTypeValue.add(HYMN_MEDIA);
        mediaTypeValue.add(HYMN_JIAOCHANG);
        mediaTypeValue.add(HYMN_CHANGSHI);
        mediaTypeValue.add(HYMN_BANZOU);
    }

    public static Map<MediaType, String> mediaDir = new HashMap<>();

    static {
        mediaDir.put(HYMN_MEDIA, MEDIA_MEDIA);
        mediaDir.put(HYMN_JIAOCHANG, MEDIA_JIAOCHANG);
        mediaDir.put(HYMN_CHANGSHI, MEDIA_CHANGSHI);
        mediaDir.put(HYMN_BANZOU, MEDIA_BANZOU);
    }

    public enum Mode {
        QQ_LINK,
        NOTION_LINK,
        NOTION_RECORD
    }

    private String mHymnType = hymnTypeValue.get(0);
    private MediaType mMediaType = mediaTypeValue.get(0);
    private static final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_config);
        setTitle(R.string.media_config);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<?> hymnTypeAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_light, hymnTypeEntry);
        // Specify the layout to use when the list of choices appears
        hymnTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_radio);

        hymnTypeSpinner = findViewById(R.id.hymnType);
        hymnTypeSpinner.setAdapter(hymnTypeAdapter);

        // to avoid onSelectedItem get triggered on first init and only then init hymnTypeSpinner (else null)
        hymnTypeSpinner.setSelection(0, false);
        hymnTypeSpinner.setOnItemSelectedListener(this);

        // mHymnTypeSpinnerItem = hymnTypeSpinner.findViewById(R.id.textItem);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<?> mediaTypeAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_light, mediaTypeEntry);
        // Specify the layout to use when the list of choices appears
        mediaTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_radio);

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
                mShare = true;
                isAutoFilled = false;
                tvMediaUri.setText(mediaUri);
                if (mediaUri.contains("mp.weixin.qq.com") || mediaUri.contains(".notion.site")) {
                    mediaTypeSpinner.setSelection(1);
                }
                else if (mediaUri.contains("youtube.com")
                        || mediaUri.contains("hymnal.net")) {
                    mediaTypeSpinner.setSelection(0);
                }

                String hymnType = bundle.getString(ATTR_HYMN_TYPE);
                int hymnNo = bundle.getInt(ATTR_HYMN_NUMBER);
                cbFu.setChecked(MediaRecord.isFu(hymnType, hymnNo));
                setHymnTypeSpinner(hymnType);
                tvHymnNo.setText(String.valueOf(hymnNo));

                // Highlight if the new url link overwrites existing media record
                tvMediaUri.setTextColor(hasMediaRecord(createMediaRecord()) ? Color.RED : Color.DKGRAY);
            }
            else {
                mShare = false;
            }
        }

        // ======== database Media Record ==========
        mListView = findViewById(R.id.mrListView);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        tvImportFile = findViewById(R.id.importFile);
        cbOverwrite = findViewById(R.id.recordOverwrite);

        /* Use a file explorer to load in an import file */
        ActivityResultLauncher<String> mGetContent = getFileUri();
        findViewById(R.id.browseImportFile).setOnClickListener(view -> {
            mViewRequest = tvImportFile;
            mGetContent.launch("*/*");
        });

        btnNQ = findViewById(R.id.button_NQ);
        btnNQ.setOnClickListener(this);
        btnNQ.setOnLongClickListener(this);

        findViewById(R.id.editFile).setOnClickListener(this);
        findViewById(R.id.button_db_records).setOnClickListener(this);

        findViewById(R.id.button_import).setOnClickListener(this);
        findViewById(R.id.button_import).setOnLongClickListener(this);
        findViewById(R.id.button_export).setOnClickListener(this);

        mPlayerView = findViewById(R.id.player_container);
        mPlayerView.setVisibility(View.GONE);
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    /**
     * Android will automatically assign initial focus to the first EditText or focusable control in your Activity.
     * It naturally follows that the InputMethod (i.e. the soft keyboard) will respond to the focus event
     * by showing itself. Disable this effect, else post media deletion will popup the softKeyboard.
     */
    @Override
    protected void onResume() {
        super.onResume();
        btnNQ.setTextColor(btnNQ.isEnabled() ? Color.DKGRAY : Color.LTGRAY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onClick(View v) {
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
                checkExitAction(false);
                break;

            // use Rich Text Editor to modify or view the import file content
            case R.id.editFile:
                String filename = ViewUtil.toString(tvImportFile);
                if (filename != null)
                    editFile(filename);
                break;

            case R.id.button_NQ:
                downloadNQRecord(Mode.NOTION_RECORD);
                break;

            // Import the import file url links to the DB database
            case R.id.button_import:
                Timber.d("import Media Records");
                importMediaRecords(null);
                break;

            // Generate a text import file for all links start with "http(s)" from database for sharing
            case R.id.button_export:
                createExportLink();
                break;

            // Show the DB content for all user defined media link
            case R.id.button_db_records:
                showMediaRecords(-1);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.button_NQ:
                downloadNQRecord(Mode.QQ_LINK);
                return true;

            case R.id.button_import:
                Timber.d("import Media Records from: %s", ASSET_URL_IMPORT_FILE);
                importMediaRecords(ASSET_URL_IMPORT_FILE);
                return true;
        }
        return false;
    }

    /**
     * Start the user selected hymn for playback and show the lyrics content.
     */
    private void startPlayOrActionView() {
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
                MainActivity.showContent(this, mHymnType, nui, true);
            }
            else {
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(uri, mimeType);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                PackageManager manager = getPackageManager();
                List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
                if (info.isEmpty()) {
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
            HymnsApp.showToastMessage(R.string.error_playback, "url is null or not found!");
        }
    }

    /**
     * A contract specifying that an activity can be called with an input of type I
     * and produce an output of type O
     *
     * @return an instant of ActivityResultLauncher<String>
     *
     * @see ActivityResultCaller
     */
    private ActivityResultLauncher<String> getFileUri() {
        return registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) {
                HymnsApp.showToastMessage(R.string.file_does_not_exist);
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
     *
     * @return original uri path or newly copied uri path
     */
    private String copyToLocalFile(String uriPath) {
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

    public boolean isOverWrite() {
        return cbOverwrite.isChecked();
    }

    // ================= User entry events ================
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == hymnTypeSpinner) {
            mHymnType = hymnTypeValue.get(position);
            if (mListView.getVisibility() == View.VISIBLE) {
                showMediaRecords(-1);
            }
        }
        else if (parent == mediaTypeSpinner) {
            mMediaType = mediaTypeValue.get(position);
        }
        checkEntry();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private final View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
        if (hasFocus) {
            mFocusedView = v;
        }
        else {
            mFocusedView = null;
        }
    };

    private class MyTextWatcher implements TextWatcher {
        private final EditText mEditText;

        public MyTextWatcher(EditText editText) {
            mEditText = editText;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (tvMediaUri.equals(mEditText) && tvMediaUri.equals(mFocusedView)) {
                Timber.d("AutoFilled set to false");
                isAutoFilled = false;
            }
            else if (tvHymnNo.equals(mEditText) && tvHymnNo.equals(mFocusedView)) {
                checkEntry();
            }
        }

        public void afterTextChanged(Editable s) {
            hasChanges = true;
        }
    }

    // ================= Need user confirmation before exit if changes detected ================

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void checkUnsavedChanges() {
        if (hasChanges && !isAutoFilled) {
            DialogActivity.showConfirmDialog(this,
                    R.string.to_be_added,
                    R.string.unsaved_changes,
                    R.string.add, new DialogActivity.DialogListener() {
                        /**
                         * Fired when user clicks the dialog's the confirm button.
                         *
                         * @param dialog source <tt>DialogActivity</tt>.
                         */
                        public boolean onConfirmClicked(DialogActivity dialog) {
                            return cmdAdd.performClick();
                        }

                        /**
                         * Fired when user dismisses the dialog.
                         *
                         * @param dialog source <tt>DialogActivity</tt>
                         */
                        public void onDialogCancelled(DialogActivity dialog) {
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
    private boolean updateMediaRecord() {
        final MediaRecord mRecord = createMediaRecord();
        if (mRecord != null) {
            if (!checkMediaAvailable(mRecord)) {
                return false;
            }

            if (hasMediaRecord(mRecord)) {
                DialogActivity.showConfirmDialog(this,
                        R.string.to_be_added,
                        R.string.db_overwrite_media,
                        R.string.overwrite, new DialogActivity.DialogListener() {
                            /**
                             * Fired when user clicks the dialog's confirm button.
                             *
                             * @param dialog source <tt>DialogActivity</tt>.
                             */
                            public boolean onConfirmClicked(DialogActivity dialog) {
                                return saveMediaRecord(mRecord);
                            }

                            /**
                             * Fired when user dismisses the dialog.
                             *
                             * @param dialog source <tt>DialogActivity</tt>
                             */
                            public void onDialogCancelled(DialogActivity dialog) {
                            }
                        });
            }
            else {
                return saveMediaRecord(mRecord);
            }
        }
        return false;
    }

    private boolean checkMediaAvailable(MediaRecord mRecord) {
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
    private MediaRecord createMediaRecord() {
        MediaRecord mediaRecord;
        String uriPath = ViewUtil.toString(tvMediaUri);

        if ((mediaRecord = MediaRecord.toRecord(uriPath)) == null) {
            String hymnNo = ViewUtil.toString(tvHymnNo);
            boolean isUrlLink = URLUtil.isValidUrl(uriPath);
            if (!isUrlLink && (uriPath != null) && !new File(uriPath).exists()) {
                uriPath = null;
            }

            if ((hymnNo == null) || (uriPath == null)) {
                HymnsApp.showToastMessage(R.string.error_hymn_config);
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
    private boolean saveMediaRecord(MediaRecord mRecord) {
        String filePath = mRecord.getMediaFilePath();
        boolean isSuccess = true;

        if (filePath != null) {
            if (filePath.contains(FileBackend.TMP)) {
                File inFile = new File(filePath);

                File subDir = FileBackend.getHymnchtvStore(mHymnType + mediaDir.get(mMediaType), true);
                if (subDir == null) {
                    HymnsApp.showToastMessage(R.string.file_access_no_permission);
                    isSuccess = false;
                }
                File outFile = new File(subDir, inFile.getName());

                try {
                    // return false if inFile rename and copy failed i.e. outFile does not exist
                    if (!outFile.exists() && !inFile.renameTo(outFile)) {
                        FilePathHelper.copy(HymnsApp.getGlobalContext(), Uri.fromFile(inFile), outFile);
                        if (!outFile.exists()) {
                            HymnsApp.showToastMessage(R.string.add_to_db_failed);
                            isSuccess = false;
                        }
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
            HymnsApp.showToastMessage(R.string.add_to_db);
            if (mListView.getVisibility() == View.VISIBLE) {
                showMediaRecords(mVisibleItem);
            }
            isAutoFilled = true;
        }
        else {
            HymnsApp.showToastMessage(R.string.add_to_db_failed);
        }
        return isSuccess;
    }

    /**
     * Delete user selected media record on user confirmation.
     * User must manually deleted the old media record if user changes uri to point to another HymnType
     */
    private void deleteMediaRecord() {
        String hymnNo = ViewUtil.toString(tvHymnNo);
        if (hymnNo == null) {
            HymnsApp.showToastMessage(R.string.error_hymn_config);
            return;
        }

        boolean isFu = cbFu.isChecked();
        int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
        if (nui != -1) {
            Bundle args = new Bundle();
            args.putString(MediaRecordDeleteFragment.ARG_MESSAGE,
                    getString(R.string.delete_media, mHymnType, nui, mMediaType));
            String title = getString(R.string.delete);

            // Displays the media record and content delete dialog and waits for user confirmation
            DialogActivity.showCustomDialog(this, title, MediaRecordDeleteFragment.class.getName(),
                    args, getString(R.string.delete), new DialogActivity.DialogListener() {
                        @Override
                        public boolean onConfirmClicked(DialogActivity dialog) {
                            MediaRecord mRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType);
                            CheckBox cbMediaDelete = dialog.findViewById(R.id.cb_media_delete);

                            if (cbMediaDelete.isChecked()) {
                                mDB.getMediaRecord(mRecord, true);
                                String filePath = mRecord.getMediaFilePath();
                                if (filePath != null) {
                                    File mediaFile = new File(filePath);
                                    if (mediaFile.exists() && !mediaFile.delete()) {
                                        Timber.w(getString(R.string.delete_media_failed, mHymnType, nui));
                                    }
                                }
                            }
                            int row = mDB.deleteMediaRecord(mRecord);
                            HymnsApp.showToastMessage((row != 0)
                                    ? getString(R.string.delete_media_ok, mHymnType, nui)
                                    : getString(R.string.delete_media_failed, mHymnType, nui));

                            if (mListView.getVisibility() == View.VISIBLE) {
                                showMediaRecords(mVisibleItem);
                            }
                            isAutoFilled = true;
                            checkEntry();
                            return true;
                        }

                        @Override
                        public void onDialogCancelled(DialogActivity dialog) {
                        }
                    }, null);
        }
    }

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void shareMediaRecord() {
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
                    HymnsApp.showToastMessage(R.string.share_file_missing,
                            mediaFile.substring(mediaFile.indexOf("Download")));
                }
            }
            ShareWith.share(this, mRecord.toExportString(), imageUris);
        }
    }

    /**
     * Decode the url path so that it is user readable i.e. convert all %xx to actual text
     */
    private void uriDecode() {
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
    private void editFile(String fileName) {
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
    private void checkEntry() {
        if (!isAutoFilled) {
            Timber.d("AutoFilled is false");
            // Highlight if the new url link overwrites existing media record
            tvMediaUri.setTextColor(hasMediaRecord(createMediaRecord()) ? Color.RED : Color.DKGRAY);
            return;
        }

        String hymnNo = ViewUtil.toString(tvHymnNo);
        if (hymnNo == null)
            return;

        boolean isFu = cbFu.isChecked();
        int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
        if (nui == -1) {
            tvMediaUri.setText("");
            return;
        }

        String uriPath = null;
        MediaRecord mediaRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType);
        if (mDB.getMediaRecord(mediaRecord, true)) {
            uriPath = mediaRecord.getMediaFilePath();
            if (TextUtils.isEmpty(uriPath)) {
                uriPath = mediaRecord.getMediaUri();
            }
            tvMediaUri.setTextColor(ContentHandler.isFileExist(mediaRecord) ? Color.RED : Color.DKGRAY);
        }
        else {
            List<Uri> uriList = new ArrayList<>();
            String dir = mHymnType + mediaDir.get(mMediaType);
            if (ContentHandler.isFileExist(dir, Integer.parseInt(hymnNo), uriList)) {
                uriPath = uriList.get(0).getPath();
                tvMediaUri.setTextColor(Color.DKGRAY);
            }
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
        tvMediaUri.setText("");
    }

    // Check for any existing mediaRecord in DB or as saved local file
    public static boolean hasMediaRecord(MediaRecord mediaRecord) {
        if (mediaRecord == null)
            return false;

        if (mDB.getMediaRecord(mediaRecord, false)) {
            return true;
        }
        else {
            return ContentHandler.isFileExist(mediaRecord);
        }
    }

    // =============================== MediaRecord Database Handler ============================

    /**
     * Wait for user confirmation before proceed with QQ links download
     *
     * @param mode flag for either Notion or QQ site hymn link records fetch
     */
    private void downloadNQRecord(final Mode mode) {
        DialogActivity.showConfirmDialog(this,
                R.string.nq_download,
                R.string.nq_download_proceed,
                R.string.download, new DialogActivity.DialogListener() {
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        if (Mode.QQ_LINK == mode) {
                            QQRecord.fetchQQLinks(MediaConfig.this);
                        }
                        else if (Mode.NOTION_LINK == mode) {
                            NotionRecord.fetchNotionLinks(MediaConfig.this);
                        }
                        else if (Mode.NOTION_RECORD == mode) {
                            NotionRecord.fetchNotionRecords(MediaConfig.this);
                        }
                        btnNQ.setEnabled(false);
                        btnNQ.setTextColor(Color.DKGRAY);
                        return true;
                    }

                    public void onDialogCancelled(DialogActivity dialog) {
                    }
                }, (Mode.QQ_LINK == mode ? "QQ" : "Notion"), HymnsApp.getResString(isOverWrite() ? R.string.db_overwrite : R.string.db_no_overwrite));
    }

    /**
     * Import the media records into the database based on import file info
     */
    private void importMediaRecords(String assetFile) {
        String importFile = ViewUtil.toString(tvImportFile);
        if (importFile == null && assetFile == null) {
            HymnsApp.showToastMessage(R.string.error_hymn_config);
            return;
        }

        DialogActivity.showConfirmDialog(this,
                R.string.db_import,
                R.string.db_import_proceed,
                R.string.ok, new DialogActivity.DialogListener() {
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        try {
                            InputStream inputStream;
                            if (assetFile != null) {
                                inputStream = HymnsApp.getAppResources().getAssets().open(assetFile);
                            }
                            else {
                                inputStream = new FileInputStream(importFile);
                            }
                            boolean isOverWrite = cbOverwrite.isChecked();
                            importUrlRecords(inputStream, isOverWrite);
                            inputStream.close();
                        } catch (IOException e) {
                            Timber.w("Input file not accessible: %s", e.getMessage());
                        }
                        return true;
                    }

                    public void onDialogCancelled(DialogActivity dialog) {
                    }
                }, assetFile != null ? assetFile : importFile.substring(importFile.lastIndexOf("/") + 1),
                HymnsApp.getResString(isOverWrite() ? R.string.db_overwrite : R.string.db_no_overwrite));
    }

    /**
     * Import the url records into the database form the given inputStream
     */
    public static void importUrlRecords(InputStream inputStream, boolean isOverWrite) {
        // HymnsApp.showToastMessage(R.string.db_import_start); not required.
        int record = 0;
        int urlRecords = 0;

        try {
            byte[] buffer2 = new byte[inputStream.available()];
            if (inputStream.read(buffer2) == -1) {
                return;
            }

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");
            // Timber.d("No of Records: %s", mList.length);

            for (String mRecord : mList) {
                MediaRecord mediaRecord = MediaRecord.toRecord(mRecord);
                if (mediaRecord == null)
                    continue;

                boolean isFu = mediaRecord.isFu();
                int hymnNo = isFu ? (mediaRecord.getHymnNo() - HYMN_DB_NO_MAX) : mediaRecord.getHymnNo();
                int nui = HymnNoValidate.validateHymnNo(mediaRecord.getHymnType(), hymnNo, isFu);
                if ((nui != -1) && (isOverWrite || !hasMediaRecord(mediaRecord))) {
                    mDB.storeMediaRecord(mediaRecord);
                    record++;
                }
                urlRecords++;

                if (TimberLog.isFinestEnable)
                    Timber.d("Import media record: %s; %s(%s); %s", nui, hymnNo, record, mRecord);
            }
        } catch (IOException e) {
            Timber.w("Import file read error: %s", e.getMessage());
        }
        HymnsApp.showToastMessage(R.string.db_import_record, record, urlRecords);
    }

    /**
     * Import the media records into the database on hymnchtv installation from the asset file;
     * Update preference setting: PREF_VERSION_URL to this app IMPORT_URL_VERSION.
     */
    public static void importUrlAssetFile() {
        try {
            // opens input stream from the url_import asset file.
            InputStream inputStream = HymnsApp.getAppResources().getAssets().open(ASSET_URL_IMPORT_FILE);
            MediaConfig.importUrlRecords(inputStream, false);

            saveUrlImportFile(inputStream, URL_IMPORT_VERSION);
            inputStream.close();

        } catch (IOException e) {
            Timber.w("Asset file not available: %s", e.getMessage());
        }
    }

    /**
     * Save a copy of the given inputStream in hymn import_export directory for user later access.
     *
     * @param inputStream InputStream containing the media records.
     * @param version Version for the url import records.
     */
    public static File saveUrlImportFile(InputStream inputStream, int version) {
        String fileName = String.format("url_import-%s.txt",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));
        File file = MediaConfig.createFileIfNotExist(fileName, true);
        try {
            if (file != null) {
                FileOutputStream outputStream = new FileOutputStream(file);
                FileBackend.copy(inputStream, outputStream);
                outputStream.close();
            }
        } catch (IOException e) {
            Timber.e("%s", e.getMessage());
        }

        // Update urlImport version in pref.
        SharedPreferences mSharedPref = HymnsApp.getGlobalContext().getSharedPreferences(PREF_SETTINGS, 0);
        SharedPreferences.Editor mEditor = mSharedPref.edit();
        mEditor.putInt(PREF_VERSION_URL, version);
        mEditor.apply();

        return file;
    }

    /**
     * Export the media records in database for the current selected mHymnType and
     * filename tagged with timeStamp e.g hymn_db-20201212_092033
     */
    private void createExportLink() {
        String fileName = String.format("hymn_link-%s.txt",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File exportFile = createFileIfNotExist(fileName, true);
        if (exportFile != null) {
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
     * Created a file with the give fileName in the DIR_IMPORT_EXPORT if permitted, and file does not exist
     * if createNew is true; else just return the filePath
     *
     * @param fileName of the created file
     * @param createNew true to create if not exist; return null on failure
     *
     * @return File path of the new file or null if failed
     */
    public static File createFileIfNotExist(String fileName, boolean createNew) {
        File subDir = FileBackend.getHymnchtvStore(DIR_IMPORT_EXPORT, true);
        if (subDir == null) {
            HymnsApp.showToastMessage(R.string.file_access_no_permission);
            return null;
        }

        File filePath = TextUtils.isEmpty(fileName) ? subDir : new File(subDir, fileName);
        if (createNew) {
            try {
                if (filePath.exists() || filePath.createNewFile()) {
                    return filePath;
                }
                else {
                    return null;
                }
            } catch (IOException e) {
                Timber.e("Failed to create media export file: %s", e.getMessage());
            }
        }
        return filePath;
    }

    /**
     * Show all the DB media records in the DB based on user selected HymnType
     */
    @SuppressLint("ClickableViewAccessibility")
    private void showMediaRecords(int scrollPos) {
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

        // android.R.layout.simple_list_item_single_choice,
        /* Display the search result to the user */
        SimpleAdapter mediaAdapter = new SimpleAdapter(this, data,
                R.layout.media_records_list, new String[]{"match"}, new int[]{R.id.item_record});

        mListView.setAdapter(mediaAdapter);
        mListView.setVisibility(View.VISIBLE);
        if (scrollPos != -1) {
            mListView.setSelection(scrollPos);
        }

        // Update the media Record Editor info.
        mListView.setOnItemClickListener((adapterView, view, pos, id) -> {
            mVisibleItem = mListView.getFirstVisiblePosition();
            // mediaAdapter.setSelectItem(pos, view);

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
                    tvHymnNo.setText(String.valueOf(hymnNo));

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

        // Force entry update when longPress a list item
        mListView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            isAutoFilled = true;
            mListView.performItemClick(view, position, id);
            return true;
        });
    }

    private void setHymnTypeSpinner(String hymnType) {
        for (int i = 0; i < hymnTypeValue.size(); i++) {
            if (hymnTypeValue.get(i).equals(hymnType)) {
                hymnTypeSpinner.setSelection(i);
                break;
            }
        }
    }

    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (mPlayerView.getVisibility() == View.VISIBLE) {
                releasePlayer();
                mPlayerView.setVisibility(View.GONE);
            }
            else {
                checkExitAction(true);
            }
        }
    };

    private void checkExitAction(boolean ignoreList) {
        if (ignoreList || mListView.getVisibility() == View.GONE) {
            if (mShare) {
                String hymnNo = ViewUtil.toString(tvHymnNo);
                if (hymnNo != null) {
                    int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), cbFu.isChecked());
                    MainActivity.showContent(this, mHymnType, nui, false);
                    finish();
                }
            }
            else {
                checkUnsavedChanges();
            }
        }
        else {
            setTitle(R.string.media_config);
            mListView.setVisibility(View.GONE);
        }
    }

    /**
     * Use for playing the help video content only
     */
    private void playVideoHelp() {
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

    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.releasePlayer();
        }
    }
}
