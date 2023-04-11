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
package org.cog.hymnchtv.service.androidnotification;

import android.annotation.TargetApi;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import org.cog.hymnchtv.R;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class to manage notification channels, and create notifications.
 *
 * @author Eng Chong Meng
 */
public class NotificationHelper extends ContextWrapper
{
    /**
     * Default group uses hymnchtv icon for notifications
     */
    public static final String DEFAULT_GROUP = "default";

    /**
     * Missed call event.
     */
    public static final String SILENT_GROUP = "silent";


    public static List<String> notificationIds = Arrays.asList( DEFAULT_GROUP, SILENT_GROUP);

    private NotificationManager notificationManager = null;

    private static final int LED_COLOR = 0xff00ff00;

    /**
     * Registers notification channels, which can be used later by individual notifications.
     *
     * @param ctx The application context
     */
    public NotificationHelper(Context ctx)
    {
        super(ctx);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Init the system service NotificationManager
            notificationManager = ctx.getSystemService(NotificationManager.class);

            // Delete any unused channel IDs or force to re-init all notification channels
            deleteObsoletedChannelIds(false);

            final NotificationChannel nDefault = new NotificationChannel(DEFAULT_GROUP,
                    getString(R.string.noti_channel_DEFAULT_GROUP), NotificationManager.IMPORTANCE_LOW);
            nDefault.setSound(null, null);
            nDefault.setShowBadge(false);
            // nDefault.setLightColor(Color.WHITE);
            nDefault.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(nDefault);

            final NotificationChannel nQuietHours = new NotificationChannel(SILENT_GROUP,
                    getString(R.string.noti_channel_SILENT_GROUP), NotificationManager.IMPORTANCE_LOW);
            nQuietHours.setSound(null, null);
            nQuietHours.setShowBadge(true);
            nQuietHours.setLightColor(LED_COLOR);
            nQuietHours.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(nQuietHours);
        }
    }

    /*
     * Send a notification.
     *
     * @param id The ID of the notification
     * @param notification The notification object
     */
    public void notify(int id, Notification.Builder notification)
    {
        notificationManager.notify(id, notification.build());
    }

    /**
     * Send Intent to load system Notification Settings for this app.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void goToNotificationSettings()
    {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }

    /**
     * Send intent to load system Notification Settings UI for a particular channel.
     *
     * @param channel Name of notification channel.
     */
    @TargetApi(Build.VERSION_CODES.O)
    public void goToNotificationSettings(String channel)
    {
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
        startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void deleteObsoletedChannelIds(boolean force)
    {
        List<NotificationChannel> channelGroups = notificationManager.getNotificationChannels();
        for (NotificationChannel nc : channelGroups) {
            if (force || !notificationIds.contains(nc.getId())) {
                notificationManager.deleteNotificationChannel(nc.getId());
            }
        }
    }

    /**
     * <a href="https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability">Behavior changes: Apps targeting Android 12</a>
     * Android 12 must specify the mutability of each PendingIntent object that your app creates.
     *
     * @return Pending Intent Flag based on API
     */
    public static int getPendingIntentFlag(boolean isMutable, boolean isUpdate)
    {
        int flag = isUpdate ? PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT;
        if (isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flag |= PendingIntent.FLAG_MUTABLE;
        }
        else if (!isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flag;
    }
}
