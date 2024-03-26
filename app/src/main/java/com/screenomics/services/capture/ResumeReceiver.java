package com.screenomics.services.capture;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.screenomics.R;

public class ResumeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, CaptureActivity.class);
        int intentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, serviceIntent,
                intentFlags);

        Notification notification =
                new Notification.Builder(context, "reminder-channel")
                        .setContentTitle("Time to resume capture!")
                        .setContentText("It has been 5 minutes since pausing the capture. Please " +
                                "resume capture ASAP!")
                        .setSmallIcon(R.drawable.dna)
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.notify(5, notification);
    }
}