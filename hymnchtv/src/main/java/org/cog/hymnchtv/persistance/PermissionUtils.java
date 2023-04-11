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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;

/**
 * Utility class for runtime permissions request.
 *
 * @author Eng Chong Meng
 */
public class PermissionUtils
{
    // android Permission Request Code
    public static final int PRC_WRITE_EXTERNAL_STORAGE = 2000;
    public static final int PRC_NOTIFICATIONS = 2001;

    private static FragmentActivity mActivity;
    private static String mPermissionRequest;
    private static String mPermissionType;

    // =========== Runtime permission handlers ==========
    /**
     * Check the WRITE_EXTERNAL_STORAGE state; proceed to request for permission if requestPermission == true.
     * Require to support WRITE_EXTERNAL_STORAGE pending aTalk installed API version.
     *
     * @param callBack the requester activity to receive onRequestPermissionsResult()
     * @param requestPermission Proceed to request for the permission if was denied; check only if false
     *
     * @return the current WRITE_EXTERNAL_STORAGE permission state
     */
    public static boolean hasWriteStoragePermission(FragmentActivity callBack, boolean requestPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return hasPermission(callBack, requestPermission, PRC_WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static boolean hasPermission(FragmentActivity callBack, boolean requestPermission, int requestCode, String permission) {
        // Timber.d(new Exception(),"Callback: %s => %s (%s)", callBack, permission, requestPermission);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(callBack, permission) != PackageManager.PERMISSION_GRANTED) {
                if (requestPermission) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(callBack, permission)) {
                        ActivityCompat.requestPermissions(callBack, new String[]{permission}, requestCode);
                    } else {
                        showHintMessage(requestCode, permission);
                    }
                }
                return false;
            }
        }
        return true;
    }

    public static void showHintMessage(int requestCode, String permission) {
        if (requestCode == PRC_WRITE_EXTERNAL_STORAGE) {
            HymnsApp.showToastMessage(R.string.permission_storage_required);
        }
        else if (requestCode == PRC_NOTIFICATIONS) {
            HymnsApp.showToastMessage(R.string.permission_notifications_required);
        } else {
            HymnsApp.showToastMessage(HymnsApp.getResString(R.string.permission_app_rational) + ": " + permission);
        }
    }

    /**
     * Requests the required permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    public static void requestPermission(FragmentActivity activity, int requestId, String permission, boolean endApp)
    {
        mActivity = activity;
        mPermissionRequest = permission;
        mPermissionType = permission.substring(permission.lastIndexOf(".") + 1);

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Display a dialog with rationale.
            RationaleDialog.newInstance(requestId, endApp).show(activity.getSupportFragmentManager(), "dialog");
        }
        else {
            // Required permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestId);
        }
    }

    /**
     * Checks if the result contains a {@link PackageManager#PERMISSION_GRANTED} result for a
     * permission from a runtime permissions request.
     *
     * @see ActivityCompat.OnRequestPermissionsResultCallback
     */
    public static boolean isPermissionGranted(String[] grantPermissions, int[] grantResults, String permission)
    {
        for (int i = 0; i < grantPermissions.length; i++) {
            if (permission.equals(grantPermissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    /**
     * A dialog that displays a permission denied message.
     */
    public static class PermissionDeniedDialog extends DialogFragment
    {
        private static final String ARGUMENT_FINISH_ACTIVITY = "finish";
        private boolean endApp = false;

        /**
         * Creates a new instance of this dialog and optionally finishes the calling Activity
         * when the 'Ok' button is clicked.
         */
        public static PermissionDeniedDialog newInstance(boolean endApp)
        {
            Bundle arguments = new Bundle();
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, endApp);

            PermissionDeniedDialog dialog = new PermissionDeniedDialog();
            dialog.setArguments(arguments);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            endApp = getArguments().getBoolean(ARGUMENT_FINISH_ACTIVITY);

            return new AlertDialog.Builder(mActivity)
                    .setMessage(getString(R.string.permission_app_rational, mPermissionType))
                    .setPositiveButton(R.string.gui_ok, null)
                    .create();
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog)
        {
            super.onDismiss(dialog);
            HymnsApp.showToastMessage(R.string.permission_storage_required);
            if (endApp) {
                mActivity.finish();
            }
        }
    }

    /**
     * A dialog that explains the use of the location permission and requests the necessary permission.
     *
     * The activity should implement {@link ActivityCompat.OnRequestPermissionsResultCallback}
     * to handle permit or denial of this permission request.
     */
    public static class RationaleDialog extends DialogFragment
    {
        private static final String ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode";
        private static final String ARGUMENT_FINISH_ACTIVITY = "finish";
        private boolean endApp = false;

        /**
         * Creates a new instance of a dialog displaying the rationale for the use of the permission.
         *
         * The permission is requested after clicking 'ok'.
         *
         * @param requestCode Id of the request that is used to request the permission. It is returned to the
         * {@link ActivityCompat.OnRequestPermissionsResultCallback}.
         * @param endApp Whether the calling Activity should be finished if the dialog is cancelled.
         */
        public static RationaleDialog newInstance(int requestCode, boolean endApp)
        {
            Bundle arguments = new Bundle();
            arguments.putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode);
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, endApp);
            RationaleDialog dialog = new RationaleDialog();
            dialog.setArguments(arguments);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            Bundle arguments = getArguments();
            final int requestCode = arguments.getInt(ARGUMENT_PERMISSION_REQUEST_CODE);
            endApp = arguments.getBoolean(ARGUMENT_FINISH_ACTIVITY);

            return new AlertDialog.Builder(mActivity)
                    .setMessage(getString(R.string.permission_app_rational, mPermissionType))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // After click on Ok, request the permission.
                        ActivityCompat.requestPermissions(mActivity, new String[]{mPermissionRequest}, requestCode);
                        // Do not finish the Activity while requesting permission.
                        endApp = false;
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog)
        {
            super.onDismiss(dialog);
            HymnsApp.showToastMessage(R.string.permission_storage_required);
            if (endApp) {
                mActivity.finish();
            }
        }
    }

    public static boolean isPermissionGranted(Context ctx, String permission)
    {
        mPermissionRequest = permission;
        mPermissionType = permission.substring(permission.lastIndexOf(".") + 1);

        boolean isGranted = ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
        if (!isGranted) {
            HymnsApp.showToastMessage(R.string.permission_storage_required);
        }
        return isGranted;
    }
}
