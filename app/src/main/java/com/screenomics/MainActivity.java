package com.screenomics;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public MediaProjectionManager mProjectionManager;
    public static int mScreenDensity;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1;
    private static final int REQUEST_CODE_MEDIA = 1000;
    private static final int REQUEST_CODE_PHONE = 1001;
    private SwitchCompat switchCapture;
    private Timer numImageRefreshTimer;
    private Button infoButton;
    private Button logButton;
    private Button devButton;
    private TextView captureState;
    private Boolean recordingState;
    private TextView numImagesText;
    private Button uploadButton;
    private TextView numUploadText;
    private int infoOpenCount = 0;
    private UploadService uploadService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WorkManager.getInstance(this).cancelAllWork();
        ListenableFuture<List<WorkInfo>> send_periodic1 = WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println(new StringBuilder().append("SENDPERIODIC: ").append(send_periodic1.get()).toString());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SenderWorker.class,
                1,
                TimeUnit.HOURS )
                .addTag("send_periodic")
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("send_periodic", ExistingPeriodicWorkPolicy.REPLACE, workRequest);

        ListenableFuture<List<WorkInfo>> send_periodic = WorkManager.getInstance(this).getWorkInfosByTag("send_periodic");
        try {
            System.out.println(new StringBuilder().append("SENDPERIODIC: ").append(send_periodic.get()).toString());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        String key = prefs.getString("key", "");
        recordingState = prefs.getBoolean("recordingState", false);
        if (key.equals("")) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        captureState = findViewById(R.id.captureState);
        switchCapture = findViewById(R.id.switchCapture);
        infoButton = findViewById(R.id.infoButton);
        logButton = findViewById(R.id.logButton);
        devButton = findViewById(R.id.devButton);
        numImagesText = findViewById(R.id.imageNumber);
        uploadButton = findViewById(R.id.uploadButton);
        numUploadText = findViewById(R.id.uploadNumber);

        switchCapture.setChecked(recordingState);

        devButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DevToolsActivity.class);
            MainActivity.this.startActivity(intent);
        });

        switchCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) { return; }
            if (isChecked) {
                Log.d("MainActivity", "pressed switch button!");
                editor.putBoolean("recordingState", true);
                editor.apply();
                startMediaProjectionRequest();
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
            } else {
                editor.putBoolean("recordingState", false);
                editor.commit();
                stopService();
                captureState.setText(getResources().getString(R.string.capture_state_off));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
            }
        });

        infoButton.setOnClickListener(v -> {
            infoOpenCount++;
            if (infoOpenCount == 5) {
                editor.putBoolean("isDev", true);
                editor.apply();
                devButton.setVisibility(View.VISIBLE);
            }
            DialogFragment informationDialog = new InfoDialog();
            informationDialog.show(getSupportFragmentManager(), "Information Dialog");
        });

        logButton.setOnClickListener(v -> {

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
                                Toast.makeText(getApplicationContext(), "Uploading...", Toast.LENGTH_SHORT).show();
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


        File f_image = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "images");
        File f_encrypt = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt");
        if (!f_image.exists()) f_image.mkdir();
        if (!f_encrypt.exists()) f_encrypt.mkdir();
        Log.i(TAG, "f_image: " + f_image.getAbsolutePath());
    }

    private void startMediaProjectionRequest() {
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA);
    }

    private void requestNotifications() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_NOTIFICATIONS);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_MEDIA) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            switchCapture.setChecked(false);
            Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent screenCaptureIntent = new Intent(this, CaptureService.class);
            screenCaptureIntent.putExtra("resultCode", resultCode);
            screenCaptureIntent.putExtra("intentData", data);
            screenCaptureIntent.putExtra("screenDensity", mScreenDensity);
            startForegroundService(screenCaptureIntent);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Toast.makeText(this, "ScreenLife Capture is running!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // for debugging purposes
    public static String intentToString(Intent intent) {
        if (intent == null)
            return "";

        StringBuilder stringBuilder = new StringBuilder("action: ")
                .append(intent.getAction())
                .append(" data: ")
                .append(intent.getDataString())
                .append(" extras: ")
                ;
        for (String key : intent.getExtras().keySet())
            stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");

        return stringBuilder.toString();

    }

    private void createAlarm(){
        final UploadScheduler alarm = new UploadScheduler(this);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, CaptureService.class);
        stopService(serviceIntent);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CaptureService.LocalBinder localBinder = (CaptureService.LocalBinder) iBinder;
            CaptureService captureService = localBinder.getService();
            if (captureService.isCapturing()) {
                captureState.setText(getResources().getString(R.string.capture_state_on));
                captureState.setTextColor(getResources().getColor(R.color.light_sea_green));
                switchCapture.setEnabled(true);
                switchCapture.setChecked(true);
            }}

        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    };

    private final ServiceConnection uploaderServiceConnection = new ServiceConnection() {
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
        public void onServiceDisconnected(ComponentName componentName) { }
    };

    @Override
    protected void onResume() {
        super.onResume();
        infoOpenCount = 0;

        captureState.setText(getResources().getString(R.string.capture_state_off));
        switchCapture.setEnabled(true);
        switchCapture.setChecked(false);
        Intent screenCaptureIntent = new Intent(this, CaptureService.class);
        bindService(screenCaptureIntent, serviceConnection, 0);

        Intent intent = new Intent(this, UploadService.class);
        bindService(intent, uploaderServiceConnection, 0);

        numImageRefreshTimer = new Timer();
        numImageRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        File outputDir = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+File.separator+"encrypt");
                        File[] files = outputDir.listFiles();
                        if (files == null) return;
                        int numImages = files.length;
                        float bytesTotal = Stream.of(files).mapToLong(File::length).sum();
                        numImagesText.setText(String.format("Number of images: %d (%sMB)", numImages, String.format("%.2f", bytesTotal / 1024 / 1024)));
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDev = prefs.getBoolean("isDev", false);
        if (!isDev) devButton.setVisibility(View.GONE);
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
        unbindService(serviceConnection);
        unbindService(uploaderServiceConnection);
    }
}

