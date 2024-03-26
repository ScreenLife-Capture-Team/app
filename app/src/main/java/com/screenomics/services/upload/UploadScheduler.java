package com.screenomics.services.upload;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.screenomics.util.InternetConnection;
import com.screenomics.util.Logger;

import java.io.File;
import java.util.Calendar;


public class UploadScheduler extends BroadcastReceiver {
    private AlarmManager alarm;
    private PendingIntent alarmIntent;
    private Context context;

    public UploadScheduler() {
    }
    // will be flagged as unused, but is called by the alarm intent below.

    public UploadScheduler(Context context) {
        this.alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UploadScheduler.class);
        int intentflags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentflags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        this.alarmIntent = PendingIntent.getBroadcast(context, 0, intent, intentflags);
        this.context = context;

        System.out.println("Resetting alarms!");
        alarm.cancel(this.alarmIntent);
    }

    public static void setAlarmInXSeconds(Context context, int seconds) {
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now + (seconds * 1000L));

        System.out.printf("SETTING ALARM TO RUN IN: %d%n",
                (cal.getTimeInMillis() - System.currentTimeMillis()) / 1000);
        Logger.i(context, "ALM" + (cal.getTimeInMillis() - System.currentTimeMillis()) / 1000);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UploadScheduler.class);
        int intentflags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentflags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, intentflags);
        alarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), alarmIntent);
    }

    public static void startUpload(Context context, boolean continueWithoutWifi) {
        File f_encrypt =
                new File(context.getExternalFilesDir(null).getAbsolutePath() + File.separator +
                        "encrypt");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String participant = prefs.getString("participant", "MISSING_PARTICIPANT_NAME");
        String key = prefs.getString("participantKey", "MISSING_KEY");
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra("dirPath", f_encrypt.getAbsolutePath());
        intent.putExtra("continueWithoutWifi", continueWithoutWifi);
        intent.putExtra("participant", participant);
        intent.putExtra("key", key);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        }
    }

    private void setAlarm(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        float diff = (cal.getTimeInMillis() - System.currentTimeMillis()) / 1000 / 60;
        if (diff < 0) {
            cal.add(Calendar.DATE, 1);
            diff = (cal.getTimeInMillis() - System.currentTimeMillis()) / 1000 / 60;
        }


        Logger.i(context, "ALM!" + diff);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (InternetConnection.checkWiFiConnection(context)) {
            startUpload(context, false);
        }
    }
}
