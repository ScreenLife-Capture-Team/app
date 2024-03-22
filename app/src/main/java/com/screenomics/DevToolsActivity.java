package com.screenomics;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.screenomics.services.upload.UploadScheduler;

import java.io.File;

public class DevToolsActivity extends Activity {

    String imagesPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dev_tools);
        imagesPath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt";
        Button resetButton = findViewById(R.id.clearPrefs);
        Button clearImagesButton = findViewById(R.id.clearImages);
        Button attemptUploadButton = findViewById(R.id.attemptUpload);
        Button attemptUploadButton2 = findViewById(R.id.attemptUpload2);
        Button setBatchSizeButton = findViewById(R.id.setBatchSizeButton);
        EditText batchSizeInput = findViewById(R.id.batchSizeInput);
        Button setMaxSendButton = findViewById(R.id.setMaxSendButton);
        EditText maxSendInput = findViewById(R.id.maxSendInput);
        Button disableButton = findViewById(R.id.disableButton);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        batchSizeInput.setText(String.valueOf(prefs.getInt("batchSize", Constants.BATCH_SIZE_DEFAULT)));
        maxSendInput.setText(String.valueOf(prefs.getInt("maxSend", Constants.MAX_TO_SEND_DEFAULT)));

        clearImagesButton.setOnClickListener(v -> {
            File dir = new File(imagesPath);
            if (!dir.exists()) dir.mkdir();
            File[] files = dir.listFiles();
            for (File file : files) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        resetButton.setOnClickListener(v -> {
            File outputDir = new File(imagesPath);
            int numImages = outputDir.listFiles().length;
            if (numImages != 0) {
                Toast.makeText(getApplicationContext(), "Cannot reset if images still exist on the device", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear().apply();
            Toast.makeText(getApplicationContext(), "Reset successful, restarting app", Toast.LENGTH_SHORT).show();
            finishAffinity();
        });

        attemptUploadButton.setOnClickListener(v -> {
            UploadScheduler.setAlarmInXSeconds(getApplicationContext(), 10);
            Toast.makeText(getApplicationContext(), "Setting upload in 10s...", Toast.LENGTH_SHORT).show();
        });

        attemptUploadButton2.setOnClickListener(v -> {
            UploadScheduler.setAlarmInXSeconds(getApplicationContext(), 300);
            Toast.makeText(getApplicationContext(), "Setting upload in 5m...", Toast.LENGTH_SHORT).show();
        });

        batchSizeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable edt) {
                if (edt.length() == 1 && edt.toString().equals("0"))
                    batchSizeInput.setText("");
            }
        });

        setBatchSizeButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("batchSize", Integer.parseInt(batchSizeInput.getText().toString()));
            editor.apply();
            Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();
        });

        setMaxSendButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("maxSend", Integer.parseInt(maxSendInput.getText().toString()));
            editor.apply();
            Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();
        });

        disableButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isDev", false);
            editor.apply();
            finish();
        });
    }
}