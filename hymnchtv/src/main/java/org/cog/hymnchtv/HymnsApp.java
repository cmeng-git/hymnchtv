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

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import org.cog.hymnchtv.impl.timberlog.TimberLogImpl;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.service.androidnotification.NotificationHelper;
import org.cog.hymnchtv.service.androidupdate.OnlineUpdateService;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.DialogActivity;

import java.util.List;

import timber.log.Timber;

/**
 * HymnsApp is the hymnchtv application class, the first to be lauched.
 * It is a global context and utility class for global actions (like EXIT broadcast).
 *
 * @author Eng Chong Meng
 */
public class HymnsApp extends Application implements LifecycleEventObserver
{
    /**
     * Indicate if hymnchtv is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    private static boolean isUpdateServerStarted = false;
    /**
     * Static instance holder.
     */
    private static HymnsApp mInstance;

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
    public void onCreate()
    {
        TimberLogImpl.init();
        // https://github.com/guardian/toolargetool
        // TooLargeTool.startLogging(this);

        // This helps to prevent WebView resets UI back to system default.
        // Must skip for < N else weired exceptions happen in Note-5
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            new WebView(this).destroy();
        }

        // Must initialize Notification channels before any notification is being issued.
        new NotificationHelper(this);

        // force delete in case system locked during testing
        // deleteDatabase(DatabaseBackend.DATABASE_NAME);

        // Trigger the hymnchtv database upgrade or creation if none exist
        DatabaseBackend.getInstance(this);
        // Recreate hymmQQ Database
        // MigrationTo3.createHymnQQTable(DatabaseBackend.getInstance(this).getWritableDatabase());

        // Do this after WebView(this).destroy(); Set up contextWrapper to use hymnchtv user selected Language
        mInstance = this;
        // String language = ConfigurationUtils.getProperty(getString(androidx.lifecycle.R.string.pref_key_locale), "");
        // LocaleHelper.setLocale(mInstance, language);

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

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
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
     * This method is for use in emulated process environments.  It will never be called on a production Android
     * device, where processes are removed by simply killing them; no user code (including this callback)
     * is executed when doing so.
     */
    @Override
    public void onTerminate()
    {
        mInstance = null;
        super.onTerminate();
    }

    // ========= LifecycleEventObserver implementations ======= //
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event)
    {
        if (Lifecycle.Event.ON_START == event) {
            isForeground = true;
            Timber.d("APP FOREGROUNDED");
        }
        else if (Lifecycle.Event.ON_STOP == event) {
            isForeground = false;
            Timber.d("APP BACKGROUNDED");
        }
    }

    /**
     * Start online service only when app is in the foreground, before going back to background upon detect
     * the device screen is locked. So need to handle with addition checks else Illegal exception.
     */
    private static void startUpdateService()
    {
        // Perform software version update check on first launch
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
     * All language setting changes must call via this so HymnsApp contextWrapper is updated
     *
     * @param language locale for the HymnsApp
     */
    public static void setLocale(String language)
    {
        // LocaleHelper.setLocale(mInstance, language);
    }

    /**
     * Retrieves <tt>AudioManager</tt> instance using application context.
     *
     * @return <tt>AudioManager</tt> service instance.
     */
    public static AudioManager getAudioManager()
    {
        return (AudioManager) getGlobalContext().getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Retrieves <tt>PowerManager</tt> instance using application context.
     *
     * @return <tt>PowerManager</tt> service instance.
     */
    public static PowerManager getPowerManager()
    {
        return (PowerManager) getGlobalContext().getSystemService(Context.POWER_SERVICE);
    }

    /**
     * Retrieves <tt>NotificationManager</tt> instance using application context.
     *
     * @return <tt>NotificationManager</tt> service instance.
     */
    public static NotificationManager getNotificationManager()
    {
        return (NotificationManager) getGlobalContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Retrieves <tt>DownloadManager</tt> instance using application context.
     *
     * @return <tt>DownloadManager</tt> service instance.
     */
    public static DownloadManager getDownloadManager()
    {
        return (DownloadManager) getGlobalContext().getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Get HymnsApp application instance
     *
     * @return HymnsApp mInstance
     */
    public static HymnsApp getInstance()
    {
        return mInstance;
    }

    /**
     * Returns global application context.
     *
     * @return Returns global application <tt>Context</tt>.
     */
    public static Context getGlobalContext()
    {
        return mInstance.getApplicationContext();
    }

    /**
     * Returns application <tt>Resources</tt> object.
     *
     * @return application <tt>Resources</tt> object.
     */
    public static Resources getAppResources()
    {
        return mInstance.getResources();
    }

    /**
     * Returns Android string resource of the user selected language for given <tt>id</tt>
     * and format arguments that will be used for substitution.
     *
     * @param id the string identifier.
     * @param arg the format arguments that will be used for substitution.
     * @return Android string resource for given <tt>id</tt> and format arguments.
     */
    public static String getResString(int id, Object... arg)
    {
        return mInstance.getString(id, arg);
    }

    /**
     * Returns Android v for a given defType and filename.
     *
     * @param resName the resource fileName.
     * @param defType the resource defined Type.
     * @return Android ResourceId for given defType and filename
     */

    public static int getFileResId(String resName, String defType)
    {
        String packageName = mInstance.getPackageName();
        return mInstance.getResources().getIdentifier(resName, defType, packageName);
    }

    public static Uri getRawUri(String filename)
    {
        int resId = HymnsApp.getFileResId(filename, "raw");
        return (resId != 0) ? Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + mInstance.getPackageName() + "/raw/" + resId) : null;
    }

    public static Uri getDrawableUri(String filename)
    {
        int resId = HymnsApp.getFileResId(filename, "drawable");
        return (resId != 0) ? Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + mInstance.getPackageName() + "/drawable/" + resId) : null;
    }

    /**
     * Returns Android string resource for given <tt>id</tt> and format arguments that will be used for substitution.
     *
     * @param aString the string identifier.
     * @return Android string resource for given <tt>id</tt> and format arguments.
     */
    public static String getResStringByName(String aString)
    {
        String packageName = mInstance.getPackageName();
        int resId = mInstance.getResources().getIdentifier(aString, "string", packageName);

        return (resId != 0) ? mInstance.getString(resId) : "";
    }

    /**
     * Toast show message in UI thread
     *
     * @param message the string message to display.
     */
    public static void showToastMessage(final String message)
    {
        new Handler(Looper.getMainLooper()).post(()
                -> Toast.makeText(getGlobalContext(), message, Toast.LENGTH_LONG).show());
    }

    public static void showToastMessage(int id)
    {
        showToastMessage(getResString(id));
    }

    public static void showToastMessage(int id, Object... arg)
    {
        showToastMessage(mInstance.getString(id, arg));
    }

    public static void showGenericError(final int id, final Object... arg)
    {
        new Handler(Looper.getMainLooper()).post(() -> {
            String msg = mInstance.getString(id, arg);
            DialogActivity.showDialog(mInstance, mInstance.getString(R.string.gui_error), msg);
        });
    }
}
