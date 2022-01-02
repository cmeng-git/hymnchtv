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
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.R;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.utils.AndroidUtils;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Demonstration of package installation and uninstallation using the package installer Session API.
 *
 * see https://developer.android.com/reference/android/content/pm/PackageInstaller
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
                    AndroidUtils.showAlertDialog(this, R.string.gui_app_update_install, R.string.gui_apk_install_completed);
                    break;

                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    AndroidUtils.showAlertDialog(this, R.string.gui_app_update_install,
                            R.string.gui_apk_install_failed, status, message);
                    break;
                default:
                    Toast.makeText(this, "Unrecognized status received from installer: " + status,
                            Toast.LENGTH_SHORT).show();
            }
        }
    }
}
