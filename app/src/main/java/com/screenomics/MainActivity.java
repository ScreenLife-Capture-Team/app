package com.screenomics;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.screenomics.debug.DebugActivity;
import com.screenomics.registration.RegisterActivity;
import com.screenomics.services.capture.CaptureActivity;
import com.screenomics.services.capture.CaptureService;
import com.screenomics.services.capture.ResumeReceiver;
import com.screenomics.services.upload.SenderWorker;
import com.screenomics.services.upload.UploadScheduler;
import com.screenomics.services.upload.UploadService;
import com.screenomics.util.InternetConnection;
import com.screenomics.util.Logger;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1;
    private SwitchCompat switchCapture;
    private Timer numImageRefreshTimer;
    private TextView captureState;
    private Boolean recordingState;
    private TextView numImagesText;
    private Button uploadButton;
    private Button pauseButton;
    private final ServiceConnection captureServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CaptureService.LocalBinder localBinder = (CaptureService.LocalBinder) iBinder;
            CaptureService captureService = localBinder.getService();
            if (captureService.isCapturing()) {
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
                switchCapture.setEnabled(true);
                switchCapture.setChecked(true);
                pauseButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    private TextView numUploadText;
    private int infoOpenCount = 0;
    private UploadService uploadService;
    private final ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            UploadService.LocalBinder localBinder = (UploadService.LocalBinder) iBinder;
            uploadService = localBinder.getService();
            if (uploadService.status == UploadService.Status.SENDING) {
                numUploadText.setText("Uploading: " + uploadService.numUploaded + "/" + uploadService.numTotal);
            } else {
                numUploadText.setText(uploadService.status.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setSupportActionBar(findViewById(R.id.mainToolbar));

        WorkManager.getInstance(this).cancelAllWork();
        ListenableFuture<List<WorkInfo>> send_periodic1 =
                WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println("SENDPERIODIC: " + send_periodic1.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SenderWorker.class,
                1,
                TimeUnit.HOURS)
                .addTag("send_periodic")
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("send_periodic", ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest);

        ListenableFuture<List<WorkInfo>> send_periodic =
                WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println("SENDPERIODIC: " + send_periodic.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        int status = prefs.getInt("state", 0);
        recordingState = prefs.getBoolean("recordingState", false);
        if (status == 0) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        }

        captureState = findViewById(R.id.captureState);
        switchCapture = findViewById(R.id.switchCapture);
        numImagesText = findViewById(R.id.imageNumber);
        uploadButton = findViewById(R.id.uploadButton);
        pauseButton = findViewById(R.id.pauseButton);
        numUploadText = findViewById(R.id.uploadNumber);

        switchCapture.setChecked(recordingState);

        switchCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }
            if (isChecked) {
                Log.d("MainActivity", "pressed switch button!");
                editor.putBoolean("recordingState", true);
                editor.apply();
                startCapture();
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
                pauseButton.setVisibility(View.VISIBLE);
            } else {
                editor.putBoolean("recordingState", false);
                editor.commit();
                stopCapture();
                captureState.setText(getResources().getString(R.string.capture_state_off));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
                pauseButton.setVisibility(View.INVISIBLE);
            }
        });

        uploadButton.setOnClickListener(v -> {
            if (!InternetConnection.checkWiFiConnection(getApplicationContext())) {

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Upload image data while not on WiFi?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Upload",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                UploadScheduler.startUpload(getApplicationContext(), true);
                                Toast.makeText(getApplicationContext(), "Uploading...",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            } else {

                UploadScheduler.startUpload(getApplicationContext(), false);
                Toast.makeText(getApplicationContext(), "Uploading...", Toast.LENGTH_SHORT).show();
            }
        });

        pauseButton.setOnClickListener(v -> {
            editor.putBoolean("recordingState", false);
            editor.commit();
            stopCapture();
            switchCapture.setChecked(false);
            captureState.setText(getResources().getString(R.string.capture_state_off));
            captureState.setTextColor(getResources().getColor(R.color.light_sea_green));

            PendingIntent mAlarmSender = PendingIntent.getBroadcast(this, 0, new Intent(this,
                    ResumeReceiver.class), PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 15 * 1000, mAlarmSender);

            pauseButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Capture has been paused", Toast.LENGTH_SHORT).show();
        });


        File f_image =
                new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "images");
        File f_encrypt =
                new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt");
        if (!f_image.exists()) f_image.mkdir();
        if (!f_encrypt.exists()) f_encrypt.mkdir();
        Log.i(TAG, "f_image: " + f_image.getAbsolutePath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.aboutOption) {
            SharedPreferences prefs = getSharedPreferences(
                    getPackageName() + "_preferences"
                    , Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            infoOpenCount++;
            if (infoOpenCount >= 5) {
                editor.putBoolean("isDev", true);
                editor.apply();
                invalidateOptionsMenu();
            }
            DialogFragment informationDialog = new InfoDialog();
            informationDialog.show(getSupportFragmentManager(), "Information Dialog");
        }

        if (id == R.id.logsOption) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Logs");
            alertDialog.setMessage(Logger.getAll(this));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        if (id == R.id.debugOption) {
            Intent intent = new Intent(MainActivity.this, DebugActivity.class);
            MainActivity.this.startActivity(intent);
        }

        if (id == R.id.devOption) {
            Intent intent = new Intent(MainActivity.this, DevToolsActivity.class);
            MainActivity.this.startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = getSharedPreferences(
                getPackageName() + "_preferences"
                , Context.MODE_PRIVATE);
        boolean isDev = prefs.getBoolean("isDev", false);

        menu.findItem(R.id.devOption).setVisible(isDev);

        return super.onPrepareOptionsMenu(menu);
    }

    private void startCapture() {
        Intent serviceIntent = new Intent(this, CaptureActivity.class);
        startActivity(serviceIntent);
    }

    private void stopCapture() {
        Intent serviceIntent = new Intent(this, CaptureService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        infoOpenCount = 0;

        captureState.setText(getResources().getString(R.string.capture_state_off));
        switchCapture.setEnabled(true);
        switchCapture.setChecked(false);
        pauseButton.setVisibility(View.INVISIBLE);
        Intent screenCaptureIntent = new Intent(this, CaptureService.class);
        bindService(screenCaptureIntent, captureServiceConnection, 0);

        Intent intent = new Intent(this, UploadService.class);
        bindService(intent, uploadServiceConnection, 0);

        numImageRefreshTimer = new Timer();
        numImageRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        File outputDir =
                                new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt");
                        File[] files = outputDir.listFiles();
                        if (files == null) return;
                        int numImages = files.length;
                        float bytesTotal = Stream.of(files).mapToLong(File::length).sum();
                        numImagesText.setText(String.format("Number of images: %d (%sMB)",
                                numImages, String.format("%.2f", bytesTotal / 1024 / 1024)));
                        Log.i(TAG, "Image Number:" + numImages);
                        if (uploadService != null) {
                            if (uploadService.status == UploadService.Status.SENDING) {
                                numUploadText.setText("Uploading: " + uploadService.numUploaded + "/" + uploadService.numTotal);
                            } else if (uploadService.status == UploadService.Status.SUCCESS) {
                                numUploadText.setText("Successfully uploaded " + uploadService.numUploaded + " images at " + uploadService.lastActivityTime);
                            } else if (uploadService.status == UploadService.Status.FAILED) {
                                numUploadText.setText("Failed uploading " + uploadService.numToUpload + " images at " + uploadService.lastActivityTime + " with code " + uploadService.errorCode);
                            } else {
                                numUploadText.setText(uploadService.status.toString());
                            }
                        }
                    }
                });
            }
        }, 500, 5000);
    }

    // This needs to be here so that onResume is called at the correct time.
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        numImageRefreshTimer.cancel();
        unbindService(captureServiceConnection);
        unbindService(uploadServiceConnection);
    }
}

