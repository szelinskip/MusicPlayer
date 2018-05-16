package pl.pisquared.musicplayer;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MusicApp extends Application
{
    public static final String NOTIFICATION_CHANNEL_ID = "MusicForegroundServiceNotification";
    public static final String NOTIFICATION_CHANNEL_NAME = "MusicForegroundService Notification Channel";

    @Override
    public void onCreate()
    {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if(manager != null)
                manager.createNotificationChannel(channel);
        }
    }
}
