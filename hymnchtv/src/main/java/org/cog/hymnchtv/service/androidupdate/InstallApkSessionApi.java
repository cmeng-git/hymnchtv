/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cog.hymnchtv.service.androidupdate;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.utils.AndroidUtils;
import org.cog.hymnchtv.utils.DialogActivity;

import java.io.*;
import java.util.*;

/**
 * Demonstration of package installation and uninstallation using the package installer Session API.
 *
 * see https://developer.android.com/reference/android/content/pm/PackageInstaller
 *
 * see https://www.py4u.net/discuss/617357
 * https://platinmods.com/threads/how-to-turn-a-split-apk-into-a-normal-non-split-apk.76683/
 * https://www.andnixsh.com/2020/06/sap-split-apks-packer-by-kirlif-windows.html
 */
public class InstallApkSessionApi extends FragmentActivity
{
    private static final String PACKAGE_INSTALLED_ACTION =
            "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String apkFile = bundle.getString(MediaConfig.ATTR_MEDIA_URI);
            String ext = apkFile.substring(apkFile.lastIndexOf(".") + 1);
            PackageInstaller.Session session = null;

            try {
                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params
                        = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int sessionId = packageInstaller.createSession(params);
                session = packageInstaller.openSession(sessionId);

                addApkToInstallSession(apkFile, session);

                // Create an install status receiver.
                Intent intent = new Intent(this, InstallApkSessionApi.class);
                intent.setAction(PACKAGE_INSTALLED_ACTION);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
                IntentSender statusReceiver = pendingIntent.getIntentSender();

                // Commit the session (this will start the installation workflow).
                session.commit(statusReceiver);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't install package", e);
            } catch (RuntimeException e) {
                if (session != null) {
                    session.abandon();
                }
                throw e;
            }
        }
    }

    private void addApkToInstallSession(String apkFile, PackageInstaller.Session session)
            throws IOException
    {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
             InputStream is = new FileInputStream(apkFile)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
            }
        }
    }

    // Note: this Activity must run in singleTop launchMode for it to be able to receive the intent in onNewIntent().
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);

            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    // This test app isn't privileged, so the user has to confirm the install.
                    Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                    startActivity(confirmIntent);
                    break;

                case PackageInstaller.STATUS_SUCCESS:
                    DialogActivity.showDialog(this, R.string.gui_app_update_install, R.string.gui_apk_install_completed);
                    break;

                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    DialogActivity.showDialog(this, R.string.gui_app_update_install,
                            R.string.gui_apk_install_failed, status, message);
                    break;
                default:
                    Toast.makeText(this, "Unrecognized status received from installer: " + status,
                            Toast.LENGTH_SHORT).show();
            }
        }
    }

    // If you have root, you can use this code.
    // installApk("/split-apks/");
    public void installApk(String apkFolderPath)
    {
        PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
        HashMap<String, Long> nameSizeMap = new HashMap<>();
        long totalSize = 0;

        File folder = new File(Environment.getExternalStorageDirectory().getPath() + apkFolderPath);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                nameSizeMap.put(listOfFiles[i].getName(), listOfFiles[i].length());
                totalSize += listOfFiles[i].length();
            }
        }
        String su = "/system/xbin/su";
        final String[] pm_install_create = new String[]{su, "-c", "pm", "install-create", "-S", Long.toString(totalSize)};
        execute(null, pm_install_create);

        List<PackageInstaller.SessionInfo> sessions = packageInstaller.getAllSessions();
        int sessId = sessions.get(0).getSessionId();
        String sessionId = Integer.toString(sessId);

        for (Map.Entry<String, Long> entry : nameSizeMap.entrySet()) {
            String[] pm_install_write = new String[]{su, "-c", "pm", "install-write", "-S", Long.toString(entry.getValue()), sessionId, entry.getKey(), Environment.getExternalStorageDirectory().getPath() + apkFolderPath + entry.getKey()};
            execute(null, pm_install_write);
        }
        String[] pm_install_commit = new String[]{su, "-c", "pm", "install-commit", sessionId};
        execute(null, pm_install_commit);

    }

    public String execute(Map<String, String> environmentVars, String[] cmd)
    {
        boolean DEBUG = true;
        if (DEBUG)
            Log.d("log", "command is " + Arrays.toString(cmd));
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (DEBUG)
                Log.d("log", "process is " + process);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (DEBUG)
                Log.d("log", "bufferreader is " + reader);

            if (DEBUG)
                Log.d("log", "readline " + reader.readLine());
            StringBuffer output = new StringBuffer();

            char[] buffer = new char[4096];
            int read;

            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            if (DEBUG)
                Log.d("log", output.toString());

            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
       Foo.installApk(context,fullPathToSplitApksFolder)

       AsyncTask.execute {
           Foo.installApk(this@MainActivity,"/storage/emulated/0/Download/split")
       }

    @WorkerThread
    @JvmStatic
    fun installApk(context:Context, apkFolderPath:String)
    {
        val packageInstaller = context.packageManager.packageInstaller
        val nameSizeMap = HashMap < File, Long>()
        var totalSize:Long = 0
        val folder = File(apkFolderPath)
        val listOfFiles = folder.listFiles().filter {
        it.isFile && it.name.endsWith(".apk")
    }
        for (file in listOfFiles) {
            Log.d("AppLog", "File " + file.name)
            nameSizeMap[file] = file.length()
            totalSize += file.length()
        }
        val su = "su"
        val pmInstallCreate = arrayOf(su, "-c", "pm", "install-create", "-S", totalSize.toString())
        execute(pmInstallCreate)
        val sessions = packageInstaller.allSessions
        val sessionId = Integer.toString(sessions[0].sessionId)
        for ((file, value) in nameSizeMap){
        val pmInstallWrite = arrayOf(su, "-c", "pm", "install-write", "-S", value.toString(), sessionId, file.name, file.absolutePath)
        execute(pmInstallWrite)
    }
        val pmInstallCommit = arrayOf(su, "-c", "pm", "install-commit", sessionId)
        execute(pmInstallCommit)
    }

    @WorkerThread
    @JvmStatic
    private fun execute(cmd:Array<String>):String?
    {
        Log.d("AppLog", "command is " + Arrays.toString(cmd))
        try {
            val process = Runtime.getRuntime().exec(cmd)
            Log.d("AppLog", "process is $process")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            Log.d("AppLog", "bufferreader is $reader")
            Log.d("AppLog", "readline " + reader.readLine())
            val output = StringBuilder()
            val buffer = CharArray(4096)
            var read:Int
            while (true) {
                read = reader.read(buffer)
                if (read <= 0)
                    break output.append(buffer, 0, read)
            }
            reader.close()
            process.waitFor()
            Log.d("AppLog", output.toString())
            return output.toString()
        } catch (e:Exception){
        e.printStackTrace()
    }
        return null
    }
    */
}
