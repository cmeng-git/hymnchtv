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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.*;
import org.cog.hymnchtv.persistance.*;
import org.cog.hymnchtv.utils.*;

import java.io.*;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

import timber.log.Timber;

import static org.cog.hymnchtv.ContentHandler.MEDIA_BANZOU;
import static org.cog.hymnchtv.ContentHandler.MEDIA_CHANGSHI;
import static org.cog.hymnchtv.ContentHandler.MEDIA_JIAOCHANG;
import static org.cog.hymnchtv.ContentHandler.MEDIA_MEDIA;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MediaType.HYMN_BANZOU;
import static org.cog.hymnchtv.MediaType.HYMN_CHANGSHI;
import static org.cog.hymnchtv.MediaType.HYMN_JIAOCHANG;
import static org.cog.hymnchtv.MediaType.HYMN_MEDIA;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;

/**
 * The class allows user to add own media content, either via:
 * a. a youtube url link etc via android share sheet (handle in MainActivity)
 * b. video or audio media file
 * and link it to a specific hymnType and hymnNo.
 *
 * It includes the capability to import, export or auto generate the export file
 * based on all the media files saved in a specified HymnType/MeidaType sub-directory
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
        implements View.OnClickListener, AdapterView.OnItemSelectedListener
{
    public static String HYMNCHTV_FAQ_UDC_RECORD = "https://cmeng-git.github.io/hymnchtv/faq.html#hymnch_0070";
    public static String HYMNCHTV_FAQ__UDC_DB = "https://cmeng-git.github.io/hymnchtv/faq.html#hymnch_0080";

    // For testing only
    private static ArrayList<String> videoUrls = new ArrayList<>();
    static {
        videoUrls.add("https://cmeng-git.github.io/atalk/video/01.atalk_main.mp4");
        videoUrls.add("https://cmeng-git.github.io/atalk/video/02.atalk_conference.mp4");
        videoUrls.add("https://cmeng-git.github.io/atalk/video/03.atalk_file_transfer.mp4");
    }

    // public static final String TABLE_NAME = "hymn_db"; // HYMN_DB
    public static final String HYMN_NO = "hymnNo";
    public static final String HYMN_FU = "isFu";

    public static final String MEDIA_TYPE = "mediaType";  // see MEDIA_xxx
    public static final String MEDIA_URI = "mediaUri";
    public static final String MEDIA_FILE_PATH = "mediaFilePath";

    private static final int REQUEST_CODE_OPEN_FILE = 105;

    // The default directory when import_export files are being saved
    public static String DIR_MEDIA_RECORDS = "import_export/";

    public static final String ATTR_MEDIA_URI = "attr_media_uri";

    /* Flag indicates if there were any uncommitted changes that shall be applied on exit */
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

    // The EditText view to be filled onActivityResult upon user selection
    private TextView mViewRequest;

    // Focused view uses as indication to determine if the tvMediaUri is auto filled or user entered
    private View mFocusedView = null;

    // DB based media record list view
    private ListView mrListView;

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
    }

    public static List<MediaType> mediaTypeValue = new ArrayList<>();

    static {
        mediaTypeValue.add(HYMN_MEDIA);
        mediaTypeValue.add(HYMN_BANZOU);
        mediaTypeValue.add(HYMN_JIAOCHANG);
        mediaTypeValue.add(HYMN_CHANGSHI);
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

    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

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

        mViewRequest = tvMediaUri;
        cbFu = findViewById(R.id.cbFu);
        cbFu.setOnCheckedChangeListener((buttonView, isChecked) -> checkEntry());

        findViewById(R.id.browseMediaUri).setOnClickListener(this);
        findViewById(R.id.decodeUri).setOnClickListener(this);
        findViewById(R.id.decodeUri).setOnClickListener(this);

        cmdAdd = findViewById(R.id.button_add);
        cmdAdd.setOnClickListener(this);

        findViewById(R.id.button_delete).setOnClickListener(this);
        findViewById(R.id.button_Exit).setOnClickListener(this);

        /* The media uri may send in from mainActivity via android share */
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String mediaUri = bundle.getString(ATTR_MEDIA_URI);
            if (!TextUtils.isEmpty(mediaUri)) {
                isAutoFilled = false;
                tvMediaUri.setText(mediaUri);
            }
        }

        // ======== database Media Record ==========
        mrListView = findViewById(R.id.mrListView);

        tvImportFile = findViewById(R.id.importFile);
        cbOverwrite = findViewById(R.id.recordOverwrite);

        findViewById(R.id.browseImportFile).setOnClickListener(this);
        findViewById(R.id.editFile).setOnClickListener(this);

        findViewById(R.id.button_import).setOnClickListener(this);
        findViewById(R.id.button_export).setOnClickListener(this);
        findViewById(R.id.button_export_create).setOnClickListener(this);
        findViewById(R.id.button_db_records).setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            /* The media uri is selected by user via file explorer */
            case R.id.browseMediaUri:
                mViewRequest = tvMediaUri;
                browseFileStore();
                break;

            case R.id.help_text:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(HYMNCHTV_FAQ_UDC_RECORD));
                startActivity(intent);
                break;

            case R.id.help_video:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(HYMNCHTV_FAQ__UDC_DB));
                startActivity(intent);

//                Bundle bundle = new Bundle();
//                bundle.putString(ATTR_VIDEO_URL, null);
//                bundle.putStringArrayList(ATTR_VIDEO_URLS, videoUrls);
//
//                intent = new Intent(this, MediaExoPlayer.class);
//                intent.putExtras(bundle);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
                break;

            /* Decode the media uri so it is user readable instead of %xx */
            case R.id.decodeUri:
                uriDecode();
                break;

            case R.id.button_add:
                if (updateMediaRecord()) {
                    HymnsApp.showToastMessage(R.string.gui_add_to_db);
                    // getActivity().getSupportFragmentManager().popBackStack();
                }
                break;

            // Manual deletion must be performed by user if the user modifies the link to point to different HymnType
            case R.id.button_delete:
                deleteMediaRecord();
                break;

            case R.id.button_Exit:
                if (mrListView.getVisibility() == View.GONE) {
                    checkUnsavedChanges();
                }
                else {
                    setTitle(R.string.gui_media_config);
                    mrListView.setVisibility(View.GONE);
                }
                break;

            // User a file explorer to load in an import file
            case R.id.browseImportFile:
                mViewRequest = tvImportFile;
                browseFileStore();
                break;

            // use Rich Text Editor to modify or view the import file content
            case R.id.editFile:
                String filename = ViewUtil.toString(tvImportFile);
                if (filename != null)
                    editFile(filename);
                break;

            // Import to the DB database
            case R.id.button_import:
                importMediaRecords();
                break;

            // Export the database to a text file for sharing
            case R.id.button_export:
                generateExportFile();
                break;

            // Auto create the export file based on the sub-directory media files
            case R.id.button_export_create:
                createImportFile();
                break;

            // Show the DB content for all user defined media link
            case R.id.button_db_records:
                showMediaRecords();
                break;
        }
    }

    // ================= User entry events ================
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (parent == hymnTypeSpinner) {
            mHymnType = hymnTypeValue.get(position);
        }
        else if (parent == mediaTypeSpinner) {
            mMediaType = mediaTypeValue.get(position);
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
                         * Fired when user clicks the dialog's confirm button.
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

    /**
     * Update user entry to the DB media record, get user confirmation if there is an existing record present
     *
     * @return true if update is successful
     */
    private boolean updateMediaRecord()
    {
        String hymnNo = ViewUtil.toString(tvHymnNo);
        String uriPath = ViewUtil.toString(tvMediaUri);

        boolean isUrlLink = URLUtil.isValidUrl(uriPath);
        if (!isUrlLink && (uriPath != null) && !new File(uriPath).exists()) {
            uriPath = null;
        }

        if ((hymnNo == null) || (uriPath == null)) {
            HymnsApp.showToastMessage(R.string.gui_error_hymn_config);
            return false;
        }

        boolean isFu = cbFu.isChecked();
        int nui = HymnNoValidate.validateHymnNo(mHymnType, Integer.parseInt(hymnNo), isFu);
        if (nui != -1) {
            String filePath = null;
            if (!isUrlLink) {
                filePath = uriPath;
                uriPath = null;
            }

            MediaRecord mRecord = new MediaRecord(mHymnType, nui, isFu, mMediaType, uriPath, filePath);
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

    /**
     * Save the user entry to the DB media record on user confirmation
     *
     * @return true is successfully
     */
    private boolean saveMediaRecord(MediaRecord mRecord)
    {
        String filePath = mRecord.getMediaFilePath();
        if (filePath != null) {
            if (filePath.contains(FileBackend.TMP)) {
                File inFile = new File(filePath);
                String fileName = inFile.getName();
                String dir = mHymnType + mediaDir.get(mMediaType);
                File outFile = new File(FileBackend.getHymnchtvStore(dir, true), fileName);
                try {
                    if (!inFile.renameTo(outFile)) {
                        return false;
                    }
                    filePath = outFile.getAbsolutePath();

                    mRecord.setFilePath(filePath);
                    mRecord.setMediaUri(null);
                } catch (Exception e) {
                    HymnsApp.showToastMessage(e.getMessage());
                    return false;
                }
            }
        }
        mDB.storeMediaRecord(mRecord);
        return true;
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
            args.putString(MediaRecordDeleteFragment.ARG_MESSAGE, getString(R.string.gui_delete_media, nui));
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
                                    if (mediaFile.exists()) {
                                        mediaFile.delete();
                                    }
                                }
                            }

                            int row = mDB.deleteMediaRecord(mRecord);
                            HymnsApp.showToastMessage(R.string.gui_delete_media_ok, (row != 0) ? nui : 0);
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
     * Opens a FileChooserDialog to let the user pick a file for local store
     */
    private void browseFileStore()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        Intent chooseFile = Intent.createChooser(intent, "File Browser");
        startActivityForResult(chooseFile, REQUEST_CODE_OPEN_FILE);
    }

    /**
     * Update the specified TextView field after user has chosen the file
     *
     * @param requestCode Pre-defied requestCode
     * @param resultCode result of user selection
     * @param intent the return data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            if ((requestCode == REQUEST_CODE_OPEN_FILE) && (intent != null)) {
                Uri uri = intent.getData();
                File inFile = new File(FilePathHelper.getFilePath(this, uri));
                if (inFile.exists()) {
                    String filename = copyToLocalFile(inFile.getPath());
                    mViewRequest.setText(filename);
                    editFile(filename);
                }
                else
                    HymnsApp.showToastMessage(R.string.gui_file_DOES_NOT_EXIST);
            }
        }
    }

    /**
     * Copy the user selected file (content://) to the hymnchtv own directory
     *
     * @param uriPath the uri path returns by File(FilePathHelper.getFilePath(mContext, uri))
     * @return original uri path or newly copied uri path
     */
    private String copyToLocalFile(String uriPath)
    {
        if (uriPath.contains(FileBackend.TMP)) {
            File inFile = new File(uriPath);
            String fileName = inFile.getName();

            File outFile = new File(FileBackend.getHymnchtvStore(DIR_MEDIA_RECORDS, true), fileName);
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
     * Import the media records into the database based on import file info
     */
    private void importMediaRecords()
    {
        String importFile = ViewUtil.toString(tvImportFile);
        if (importFile == null) {
            HymnsApp.showToastMessage(R.string.gui_error_hymn_config);
            return;
        }

        try {
            InputStream in2 = new FileInputStream(importFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            boolean isOverWrite = cbOverwrite.isChecked();

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            for (String mRecord : mList) {
                MediaRecord mediaRecord = MediaRecord.toRecord(mRecord);

                boolean isFu = mediaRecord.isFu();
                int hymnNo = isFu ? (mediaRecord.getHymnNo() - HYMN_DB_NO_MAX) : mediaRecord.getHymnNo();
                int nui = HymnNoValidate.validateHymnNo(mediaRecord.getHymnType(), hymnNo, isFu);
                if ((nui != -1) && (isOverWrite || !mDB.getMediaRecord(mediaRecord, false))) {
                    mDB.storeMediaRecord(mediaRecord);
                }
            }
        } catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
        }
    }

    /**
     * Export the media records in database for the current selected mHymnType and
     * filename tagged with timeStamp e.g hymn_db-20201212_092033
     */
    private void generateExportFile()
    {
        String fileName = String.format("%s-%s.txt", mHymnType,
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File exportFile = new File(FileBackend.getHymnchtvStore(DIR_MEDIA_RECORDS, true), fileName);
        try {
            exportFile.createNewFile();
        } catch (IOException e) {
            Timber.w("Failed to create media export file!");
        }

        if (exportFile.exists()) {
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(exportFile.getAbsolutePath());

                List<MediaRecord> mediaRecords = mDB.getMediaRecords(mHymnType);
                if (!mediaRecords.isEmpty()) {
                    for (MediaRecord mediaRecord : mediaRecords) {
                        String mRecord = mediaRecord.getExportString();
                        if (mRecord != null)
                            fileWriter.write(mRecord);
                    }
                    HymnsApp.showToastMessage(R.string.hymn_match, mediaRecords.size());
                    tvImportFile.setText(exportFile.getPath());
                    editFile(exportFile.getPath());
                }
                else {
                    HymnsApp.showToastMessage(R.string.hymn_match_none);
                }
                fileWriter.close();
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
     *
     * Export filename tagged with timeStamp e.g hymn_db-20201212_092033
     */
    private void createImportFile()
    {
        String mediaRecord;
        String fileName = String.format("%s-%s.txt", mHymnType,
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));

        File exportFile = new File(FileBackend.getHymnchtvStore(DIR_MEDIA_RECORDS, true), fileName);
        try {
            exportFile.createNewFile();
        } catch (IOException e) {
            Timber.w("Failed to create media export file!");
        }

        if (exportFile.exists()) {
            File srcPath = FileBackend.getHymnchtvStore(mHymnType + MEDIA_MEDIA, false);
            if ((srcPath != null) && srcPath.isDirectory()) {
                File[] files = srcPath.listFiles();
                if ((files != null) && (files.length > 0)) {
                    try {
                        FileWriter fileWriter = new FileWriter(exportFile.getAbsolutePath());
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
                                    mHymnType, hymnNo, 0, HYMN_MEDIA, null, file.getAbsolutePath());
                            fileWriter.write(mediaRecord);
                        }
                        HymnsApp.showToastMessage(R.string.hymn_match, files.length);
                        fileWriter.close();

                        tvImportFile.setText(exportFile.getPath());
                        editFile(exportFile.getPath());
                    } catch (IOException e) {
                        Timber.e("Create Import File exception: %s", e.getMessage());
                    }
                }
                else {
                    HymnsApp.showToastMessage(R.string.hymn_match_none);
                }
            }
        }
    }

    /**
     * Show all the DB media records in the DB based on user selected HymnType and MediaType
     */
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
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.media_records_list, new String[]{"match"},
                new int[]{R.id.item_record});

        mrListView.setAdapter(adapter);
        mrListView.setVisibility(View.VISIBLE);

        // Update the media Record Editor info.
        mrListView.setOnItemClickListener((adapterView, view, pos, id) -> {
            if (isAutoFilled) {
                Map<String, String> item_db = data.get(pos);
                String mRecord = item_db.get("match");
                if (!TextUtils.isEmpty(mRecord)) {
                    String[] items = mRecord.split("#|:[^/]\\s*\n*");

                    for (int i = 0; i < hymnTypeValue.size(); i++) {
                        if (hymnTypeValue.get(i).equals(items[0])) {
                            hymnTypeSpinner.setSelection(i);
                        }
                    }
                    mediaTypeSpinner.setSelection(Enum.valueOf(MediaType.class, items[2]).getValue());

                    String hymnNo = items[1];
                    if (hymnNo.startsWith("附")) {
                        hymnNo = hymnNo.substring(1);
                        cbFu.setChecked(true);
                    }
                    tvHymnNo.setText(hymnNo);

                    // force focus to tvHymnNo so isAutoFilled cannot be accidentally cleared
                    tvHymnNo.requestFocus();

                    String mediaPath = items[4];
                    if (MediaRecord.FILEPATH.equals(items[3])) {
                        mediaPath = mediaPath.replace(MediaRecord.DOWNLOAD_DIR, MediaRecord.DOWNLOAD_FP);
                    }
                    tvMediaUri.setText(mediaPath);
                }
            }
        });
    }

//    private void showHelp()
//    {
//        WebViewFragment mWebFragment = (WebViewFragment) getSupportFragmentManager().findFragmentById(R.id.webView);
//        if (mWebFragment == null) {
//            mWebFragment = new WebViewFragment();
//        }
//        else {
//            mWebFragment.initWebView();
//        }
//
//        getSupportFragmentManager().beginTransaction().replace(R.id.webView, mWebFragment).commit();
//        mWebView.setVisibility(View.VISIBLE);
//    }
}
