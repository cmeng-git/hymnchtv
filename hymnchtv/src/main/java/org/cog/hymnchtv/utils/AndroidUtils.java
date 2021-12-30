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
package org.cog.hymnchtv.utils;

import android.app.*;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.core.app.NotificationCompat;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.service.androidnotification.NotificationHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import timber.log.Timber;

/**
 * The <tt>AndroidUtils</tt> class provides a set of utility methods allowing an easy way to show
 * an alert dialog on android, show a general notification, etc.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AndroidUtils
{
    /**
     * Api level constant. Change it here to simulate lower api on new devices.
     *
     * All API level decisions should be done based on {@link #hasAPI(int)} call result.
     */
    private static final int API_LEVEL = Build.VERSION.SDK_INT;

    /**
     * Var used to track last hymnchtv icon notification text in order to prevent from posting
     * updates that make no sense. This will happen when providers registration state changes
     * and global status is still the same(online or offline).
     */
    private static String lastNotificationText = null;

    /**
     * Shows an alert dialog for the given context and a title given by <tt>titleId</tt> and
     * message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param titleId the title identifier in the resources
     * @param messageId the message identifier in the resources
     */
    public static void showAlertDialog(Context context, final int titleId, final int messageId, final Object... arg)
    {
        String title = context.getResources().getString(titleId);
        String msg = context.getResources().getString(messageId, arg);
        showAlertDialog(context, title, msg);
    }

    /**
     * Shows an alert dialog for the given context and a title given by <tt>titleId</tt> and
     * message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param title the title identifier in the resources
     * @param message the message identifier in the resources
     * @param button the confirm button string identifier
     * @param listener the <tt>DialogInterface.DialogListener</tt> to attach to the confirm button
     */
    public static void showAlertConfirmDialog(Context context, final String title,
            final String message, final String button, final DialogActivity.DialogListener listener)
    {
        DialogActivity.showConfirmDialog(context, title, message, button, listener);
    }

    /**
     * Shows an alert dialog for the given context and a title given by <tt>titleId</tt> and
     * message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param title the title of the message
     * @param message the message
     */
    public static void showAlertDialog(final Context context, final String title, final String message)
    {
        DialogActivity.showDialog(context, title, message);
    }

    /**
     * Clears the general notification.
     *
     * @param appContext the <tt>Context</tt> that will be used to create new activity from notification
     * <tt>Intent</tt>.
     */
    public static void clearGeneralNotification(Context appContext)
    {
//        int id = Service.getGeneralNotificationId();
//        if (id < 0) {
//            Timber.log(TimberLog.FINER, "There's no global notification icon found");
//            return;
//        }

        AndroidUtils.generalNotificationInvalidated();
        // AndroidGUIActivator.getLoginRenderer().updatehymnchtvIconNotification();
    }

    /**
     * Shows an alert dialog for the given context and a title given by <tt>titleId</tt> and
     * message given by <tt>messageId</tt>.
     *
     * @param context the android <tt>Context</tt>
     * @param notificationID the identifier of the notification to update
     * @param title the title of the message
     * @param message the message
     * @param date the date on which the event corresponding to the notification happened
     */
    public static void updateGeneralNotification(Context context, int notificationID, String title,
            String message, long date)
    {
        // Filter out the same subsequent notifications
        if (lastNotificationText != null && lastNotificationText.equals(message)) {
            return;
        }

        NotificationCompat.Builder nBuilder;
        nBuilder = new NotificationCompat.Builder(context, NotificationHelper.DEFAULT_GROUP);

//        nBuilder.setContentTitle(title)
//                .setContentText(message)
//                .setWhen(date)
//                .setSmallIcon(R.drawable.ic_notification);
//
//        nBuilder.setContentIntent(HymnsApp.gethymnchtvIconIntent());
        NotificationManager mNotificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = nBuilder.build();
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
                & Notification.FLAG_FOREGROUND_SERVICE & Notification.FLAG_NO_CLEAR;

        // mId allows you to update the notification later on.
        mNotificationManager.notify(notificationID, notification);
        lastNotificationText = message;
    }

    /**
     * This method should be called when general notification is changed from the outside(like in
     * call notification for example).
     */
    public static void generalNotificationInvalidated()
    {
        lastNotificationText = null;
    }

    /**
     * Indicates if the service given by <tt>activityClass</tt> is currently running.
     *
     * @param context the Android context
     * @param activityClass the activity class to check
     * @return <tt>true</tt> if the activity given by the class is running, <tt>false</tt> - otherwise
     */
    public static boolean isActivityRunning(Context context, Class<?> activityClass)
    {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);

        boolean isServiceFound = false;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).topActivity.getClassName().equals(activityClass.getName())) {
                isServiceFound = true;
            }
        }
        return isServiceFound;
    }

    public static void setOnTouchBackgroundEffect(View view)
    {
        view.setOnTouchListener(new OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                if (!(v.getBackground() instanceof TransitionDrawable))
                    return false;

                TransitionDrawable transition = (TransitionDrawable) v.getBackground();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        transition.startTransition(500);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        transition.reverseTransition(500);
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Returns <tt>true</tt> if we are currently running on tablet device.
     *
     * @return <tt>true</tt> if we are currently running on tablet device.
     */
    public static boolean isTablet()
    {
        Context context = HymnsApp.getGlobalContext();

        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Returns <tt>true</tt> if this device supports at least given API level.
     *
     * @param minApiLevel API level value to check
     * @return <tt>true</tt> if this device supports at least given API level.
     */
    public static boolean hasAPI(int minApiLevel)
    {
        return API_LEVEL >= minApiLevel;
    }

    /**
     * Returns <tt>true</tt> if current <tt>Thread</tt> is UI thread.
     *
     * @return <tt>true</tt> if current <tt>Thread</tt> is UI thread.
     */
    public static boolean isUIThread()
    {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /**
     * Converts pixels to density independent pixels.
     *
     * @param px pixels value to convert.
     * @return density independent pixels value for given pixels value.
     */
    public static int pxToDp(int px)
    {
        return (int) (((float) px) * HymnsApp.getAppResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Number of milliseconds in a second.
     */
    public static final long MILLIS_PER_SECOND = 1000;

    /**
     * Number of milliseconds in a standard minute.
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    /**
     * Number of milliseconds in a standard hour.
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * Number of milliseconds in a standard day.
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    /**
     * Formats the given long to X hour, Y min, Z sec.
     *
     * @param millis the time in milliseconds to format
     * @return the formatted seconds
     */
    public static String formatSeconds(long millis)
    {
        long[] values = new long[4];
        values[0] = millis / MILLIS_PER_DAY;
        values[1] = (millis / MILLIS_PER_HOUR) % 24;
        values[2] = (millis / MILLIS_PER_MINUTE) % 60;
        values[3] = (millis / MILLIS_PER_SECOND) % 60;

        String[] fields = {" d ", " h ", " min ", " sec"};

        StringBuffer buf = new StringBuffer(64);
        boolean valueOutput = false;

        for (int i = 0; i < 4; i++) {
            long value = values[i];

            if (value == 0) {
                if (valueOutput)
                    buf.append('0').append(fields[i]);
            }
            else {
                valueOutput = true;
                buf.append(value).append(fields[i]);
            }
        }
        return buf.toString().trim();
    }

    public static String UrlEncode(String url) throws UnsupportedEncodingException
    {
        // Need to encode chinese link for safe access; revert all "%3A" and "%2F" to ":" and "/" etc
        String encDnLnk = URLEncoder.encode(url, "UTF-8")
                .replace("%23", "#")
                .replace("%26", "&")
                .replace("%2F", "/")
                .replace("%3A", ":")
                .replace("%3B", ";")
                .replace("%3D", "=")
                .replace("%3F", "?");
        // Timber.d("Download URL link encoded: %s", encDnLnk);
        return encDnLnk;
    }
}