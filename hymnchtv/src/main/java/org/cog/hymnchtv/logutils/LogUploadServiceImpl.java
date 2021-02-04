/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.cog.hymnchtv.logutils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.service.androidupdate.VersionServiceImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Send/upload logs, to specified destination.
 *
 * @author Damian Minkov
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class LogUploadServiceImpl
{
    /**
     * List of log files created for sending logs purpose. There is no easy way of waiting until
     * email is sent and deleting temp log file, so they are cached and removed on OSGI service stop action.
     */
    private final List<File> storedLogFiles = new ArrayList<>();

    /**
     * Retrieve logcat file from Android and Send the log files.
     *
     * @param destinations array of destination addresses
     * @param subject the subject if available
     * @param title the title for the action, used any intermediate dialogs that need to be shown, like "Choose action:".
     */
    public void sendLogs(String[] destinations, String subject, String title)
    {
        /* The path pointing to directory used to store temporary log archives. */
        File logStorageDir = FileBackend.getHymnchtvStore("logs", true);
        if (logStorageDir != null) {
            File externalStorageFile;
            try {
                debugPrintInfo();
                File logcatDir = new File(HymnsApp.getGlobalContext().getFilesDir(), "/log");
                if (!logcatDir.exists()) {
                    logcatDir.mkdirs();
                }
                File logcatFile = new File(logcatDir, "hymnchtv-logcat.txt");

                Runtime.getRuntime().exec("logcat -v time -f " + logcatFile.getAbsolutePath());
                // just wait for 100ms before collect logs - note: process redirect to file, and does not exit
                Thread.sleep(100);
                externalStorageFile = LogsCollector.collectLogs(logStorageDir, null);
            } catch (Exception ex) {
                HymnsApp.showToastMessage("Error creating logs file archive: " + ex.getMessage());
                return;
            }

            // Stores file name to remove it on service shutdown
            storedLogFiles.add(externalStorageFile);
            Context ctx = HymnsApp.getGlobalContext();
            Uri logsUri = FileBackend.getUriForFile(ctx, externalStorageFile);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, destinations);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.setType("application/zip");
            sendIntent.putExtra(Intent.EXTRA_STREAM, logsUri);
            sendIntent.putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.gui_SEND_LOGS_INFO));

            Intent chooserIntent = Intent.createChooser(sendIntent, title);
            ctx.grantUriPermission("android", logsUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // chooserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); not working; need above statement

            // Starting this activity from context that is not from the current activity; this flag is needed in this situation
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooserIntent);
        }
        else {
            Timber.e("Error sending debug log files");
        }
    }

    /**
     * Extract debug info for android device OS and hymnchtv installed version
     */
    private void debugPrintInfo()
    {
        String property = "http.agent";
        try {
            String sysProperty = System.getProperty(property);
            Timber.i("%s = %s", property, sysProperty);
        } catch (Exception e) {
            Timber.w(e, "An exception occurred while writing debug info");
        }

        VersionServiceImpl versionService = VersionServiceImpl.getInstance();

        Timber.i("Device installed with aTalk-android version: %s, version code: %s",
                versionService.getCurrentVersionName(), versionService.getCurrentVersionCode());
    }

    /**
     * Frees resources allocated by this service.
     * Purge all files log directory and log sent.
     */
    public void dispose()
    {
        System.err.println("DISPOSE!!!!!!!!!!!!!");
        for (File logFile : storedLogFiles) {
            logFile.delete();
        }
        storedLogFiles.clear();
        // clean debug log directory after log sent
        purgeDebugLog();
    }

    /**
     * Process to purge all debug log files in case it gets too large to handle Static access from other module
     */
    public static void purgeDebugLog()
    {
        File logDir = new File(HymnsApp.getGlobalContext().getFilesDir() + "/log");
        if (logDir.exists()) {
            final File[] files = logDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete())
                        Timber.w("Couldn't delete log file: %s", file.getName());
                }
            }
        }
    }
}
