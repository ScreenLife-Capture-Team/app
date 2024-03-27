package com.screenomics.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.screenomics.MainActivity;
import com.screenomics.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CaptureNotifications {
    public static final int CAPTURE_NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "screenomics_id";
    private static final String CAPTURE_CHANNEL_ID = "capture-channel";
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private final Context context;

    public CaptureNotifications(Context context) {
        this.context = context;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // NotificationManager not supported
            return;
        }

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Screenomics Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(serviceChannel);

        NotificationChannel updateChannel = new NotificationChannel(
                CAPTURE_CHANNEL_ID,
                "Screenomics Updates Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(updateChannel);

        NotificationChannel reminderChannel = new NotificationChannel(
                "reminder-channel",
                "Screenomics Reminder Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(reminderChannel);

    }

    @SuppressLint("ObsoleteSdkInt")
    public void notifyImageCaptured() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // NotificationManager not supported
            return;
        }
        createChannels();

        Notification notification = new Notification.Builder(context, CAPTURE_CHANNEL_ID)
                .setSmallIcon(R.drawable.dna)
                .setContentTitle("ScreenLife Capture just took a capture!")
                .setContentText("At time: " + sdf.format(new Date()))
                .build();
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.notify(2, notification);
    }

    private Notification getCaptureStatusNotification(Service service, String title,
                                                      String subtitle) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            // NotificationManager not supported
            throw new Error("Invalid SDK version");
        }
        createChannels();

        Intent notificationIntent = new Intent(context, MainActivity.class);

        int intentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                intentFlags);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.dna)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        return notification;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void notifyCaptureStopped(Service service) {
        Notification notification = getCaptureStatusNotification(
                service,
                "ScreenLife Capture is NOT Running!",
                "Please restart the application!"
        );
        service.startForeground(CAPTURE_NOTIFICATION_ID, notification);
    }

    @SuppressLint("ObsoleteSdkInt")
    public Notification getCaptureStartedNotification(Service foregroundService) {
        Notification notification = getCaptureStatusNotification(
                foregroundService,
                "ScreenLife Capture is currently enabled",
                "If this notification " +
                        "disappears, please re-enable it from the application."
        );

        return notification;
    }
}
