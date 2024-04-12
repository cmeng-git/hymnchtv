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

import static org.cog.hymnchtv.MainActivity.PREF_LOCALE;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;

import android.app.ActivityManager;
import android.app.Application;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.util.List;

import org.cog.hymnchtv.impl.timberlog.TimberLogImpl;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.service.androidnotification.NotificationHelper;
import org.cog.hymnchtv.service.androidupdate.OnlineUpdateService;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.LocaleHelper;

import timber.log.Timber;

/**
 * HymnsApp is the hymnchtv application class, the first to be launched.
 * It is a global context and utility class for global actions (like EXIT broadcast).
 *
 * @author Eng Chong Meng
 */
public class HymnsApp extends Application implements LifecycleEventObserver {
    /**
     * Indicate if hymnchtv is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    private static boolean isUpdateServerStarted = false;

    // Use the clear current toast state so new one can be shown immediately
    private static Toast toast = null;

    /**
     * Static instance holder.
     */
    private static Context mInstance;

    /**
     * Must have only one instance of the MediaDownloadHandler for properly UI display
     */
    public static MediaDownloadHandler mMediaDownloadHandler;

    public static boolean isPortrait = true;

    public static int screenWidth;
    public static int screenHeight;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        TimberLogImpl.init();
        // https://github.com/guardian/toolargetool
        // TooLargeTool.startLogging(this);

        // This helps to prevent WebView resets UI back to system default.
        // Must skip for < N else weired exceptions happen in Note-5
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                new WebView(this).destroy();
            } catch (Exception e) {
                Timber.e("WebView init exception: %s", e.getMessage());
            }
        }

        // Must initialize Notification channels before any notification is being issued.
        new NotificationHelper(this);

        // force delete in case system locked during testing
        // deleteDatabase(DatabaseBackend.DATABASE_NAME);

        // Trigger the hymnchtv database upgrade or creation if none exist
        DatabaseBackend.getInstance(this);

        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        mMediaDownloadHandler = new MediaDownloadHandler();

        // Get android device screen display size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Point size = new Point();
            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }
        else {
            // UnsupportedOperationException: in Xiaomi Mi 11 Android 11 (SDK 30)
            // mInstance.getDisplay().getSize(size);
            Rect mBounds = ((WindowManager) getSystemService(WINDOW_SERVICE)).getCurrentWindowMetrics().getBounds();
            screenWidth = Math.abs(mBounds.width());
            screenHeight = Math.abs(mBounds.height());
        }
        // Purge all the previously old downloaded apk
        UpdateServiceImpl.getInstance().removeOldDownloads();
    }

    /**
     * setLocale for Application class to work properly with PBContext class.
     */
    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences sharePref = base.getSharedPreferences(PREF_SETTINGS, 0);
        String language = sharePref.getString(PREF_LOCALE, LocaleHelper.LocaleChinese);
        mInstance = LocaleHelper.setLocale(base, language);
        super.attachBaseContext(mInstance);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = true;
        }
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isPortrait = false;
        }
    }

    /**
     * This method is for use in emulated process environments. It will never be called on a production Android
     * device, where processes are removed by simply killing them; no user code (including this callback)
     * is executed when doing so.
     */
    @Override
    public void onTerminate() {
        mInstance = null;
        super.onTerminate();
    }

    // ========= LifecycleEventObserver implementations ======= //
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (Lifecycle.Event.ON_START == event) {
            isForeground = true;
            startUpdateService();
        }
        else if (Lifecycle.Event.ON_STOP == event) {
            isForeground = false;
        }
    }

    /**
     * Start online service only when app is in the foreground, before going back to background upon detect
     * the device screen is locked. So need to handle with addition checks else Illegal exception.
     */
    private static void startUpdateService() {
        // Perform software version update check on first launch for both release and debug version
        if (BuildConfig.DEBUG && !isUpdateServerStarted) {
            ActivityManager manager = (ActivityManager) mInstance.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
            if (runningAppProcesses != null) {
                int importance = runningAppProcesses.get(0).importance;
                // higher importance has lower number
                if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Intent dailyCheckupIntent = new Intent(mInstance, OnlineUpdateService.class);
                    dailyCheckupIntent.setAction(OnlineUpdateService.ACTION_AUTO_UPDATE_START);
                    mInstance.startService(dailyCheckupIntent);
                    isUpdateServerStarted = true;
                    Timber.d("### Online hymnchtv app update service started!");
                }
            }
        }
    }

    /**
     * Retrieves <tt>DownloadManager</tt> instance using application context.
     *
     * @return <tt>DownloadManager</tt> service instance.
     */
    public static DownloadManager getDownloadManager() {
        return (DownloadManager) getGlobalContext().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Get HymnsApp application instance
     *
     * @return HymnsApp mInstance
     */
    public static Context getInstance() {
        return mInstance;
    }

    /**
     * Returns global application context.
     *
     * @return Returns global application <tt>Context</tt>.
     */
    public static Context getGlobalContext() {
        return mInstance.getApplicationContext();
    }

    /**
     * Returns application <tt>Resources</tt> object.
     *
     * @return application <tt>Resources</tt> object.
     */
    public static Resources getAppResources() {
        return mInstance.getResources();
    }

    /**
     * Returns Android string resource of the user selected language for given <tt>id</tt>
     * and format arguments that will be used for substitution.
     *
     * @param id the string identifier.
     * @param arg the format arguments that will be used for substitution.
     *
     * @return Android string resource for given <tt>id</tt> and format arguments.
     */
    public static String getResString(int id, Object... arg) {
        return mInstance.getString(id, arg);
    }

    /**
     * Returns Android v for a given defType and filename.
     *
     * @param resName the resource fileName.
     * @param defType the resource defined Type.
     *
     * @return Android ResourceId for given defType and filename
     */

    public static int getFileResId(String resName, String defType) {
        String packageName = mInstance.getPackageName();
        return mInstance.getResources().getIdentifier(resName, defType, packageName);
    }

    public static Uri getRawUri(String filename) {
        int resId = HymnsApp.getFileResId(filename, "raw");
        return (resId != 0) ? Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + mInstance.getPackageName() + "/raw/" + resId) : null;
    }

    public static Uri getDrawableUri(String filename) {
        int resId = HymnsApp.getFileResId(filename, "drawable");
        return (resId != 0) ? Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + mInstance.getPackageName() + "/drawable/" + resId) : null;
    }

    /**
     * Toast show message in UI thread
     *
     * @param message the string message to display.
     */
    public static void showToastMessage(final String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (toast != null && toast.getView() != null) {
                toast.cancel();
            }
            toast = Toast.makeText(getGlobalContext(), message, Toast.LENGTH_LONG);
            toast.show();
        });
    }

    public static void showToastMessage(int id) {
        showToastMessage(getResString(id));
    }

    public static void showToastMessage(int id, Object... arg) {
        showToastMessage(mInstance.getString(id, arg));
    }
}
