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
package org.cog.hymnchtv.persistance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.LinkedList;
import java.util.List;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;

/**
 * Utility class for runtime permissions request.
 *
 * @author Eng Chong Meng
 */
public class PermissionUtils {
    // android Permission Request Code
    public static final int PRC_PERMISSIONS_HYMN = 2000;
    public static final int PRC_WRITE_EXTERNAL_STORAGE = 2001;
    public static final int PRC_NOTIFICATIONS = 2002;

    protected static List<String> permissionList = new LinkedList<>();

    // Not all android devices follow this. Hymn allow app settings access via menu.
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
            permissionList.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissionList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionList.add(Manifest.permission.READ_MEDIA_VIDEO);
        }
        else {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            else {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private static final String[] permissions = permissionList.toArray(new String[0]);

    // =========== Runtime permission handlers ==========

    /**
     * Check the WRITE_EXTERNAL_STORAGE state; proceed to request for permission if requestPermission == true.
     * Require to support WRITE_EXTERNAL_STORAGE pending aTalk installed API version.
     *
     * @param activity the requester activity to receive onRequestPermissionsResult()
     *
     * @return the current WRITE_EXTERNAL_STORAGE permission state
     */
    public static boolean checkWriteStoragePermission(FragmentActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, PRC_WRITE_EXTERNAL_STORAGE);
            }
            else {
                HymnsApp.showToastMessage(R.string.permission_storage_required);
            }
            return false;
        }
        return true;
    }

    public static void checkHymnPermissionAndRequest(FragmentActivity activity) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(activity, permissions, PRC_PERMISSIONS_HYMN);
        }
    }
}
