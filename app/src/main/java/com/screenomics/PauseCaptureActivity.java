package com.screenomics;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.screenomics.services.capture.CaptureService;
import com.screenomics.services.capture.ResumeReceiver;

public class PauseCaptureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pause_capture);

        Button stopCaptureButton = findViewById(R.id.stopConfirmButton);
        Spinner spinner = findViewById(R.id.spinner);

        String[] items = new String[]{"1", "2", "three"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

        String reason = "";
        spinner.setOnItemSelectedListener((AdapterView parent, View view, int pos, long id) -> {
            reason = items[pos];
        });

        stopCaptureButton.setOnClickListener(v -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean("recordingState", false);
            editor.commit();

            Intent serviceIntent = new Intent(this, CaptureService.class);
            stopService(serviceIntent);

            PendingIntent mAlarmSender = PendingIntent.getBroadcast(this, 0, new Intent(this,
                    ResumeReceiver.class), PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 15 * 1000, mAlarmSender);

            // Make record of a pause of <x> minutes, for reason <reason>

            Toast.makeText(this, "Capture has been paused", Toast.LENGTH_SHORT).show();

            this.finish();
        });
    }
}