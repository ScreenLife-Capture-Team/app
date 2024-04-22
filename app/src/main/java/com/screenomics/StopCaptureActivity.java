package com.screenomics;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import com.screenomics.services.capture.CaptureService;

public class StopCaptureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_capture);

        Button stopCaptureButton = findViewById(R.id.stopConfirmButton);

        stopCaptureButton.setOnClickListener(v -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean("recordingState", false);
            editor.commit();

            Intent serviceIntent = new Intent(this, CaptureService.class);
            stopService(serviceIntent);

            this.finish();
        });
    }
}