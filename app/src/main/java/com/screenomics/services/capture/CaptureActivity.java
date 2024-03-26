package com.screenomics.services.capture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class CaptureActivity extends Activity {
    private static final String TAG = "CaptureActivity";

    private static final int REQUEST_CODE_MEDIA = 1000;
    private static final int REQUEST_CODE_PHONE = 1001;

    public static int mScreenDensity;
    public MediaProjectionManager mProjectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        startMediaProjectionRequest();
        super.onCreate(savedInstanceState);
    }

    private void startMediaProjectionRequest() {
        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_MEDIA) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            Toast.makeText(getApplicationContext(), "Unknown request code: " + requestCode,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (resultCode != RESULT_OK) {
            // Mark not recording in UI
            Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        try {
            Intent screenCaptureIntent = new Intent(this, CaptureService.class);
            screenCaptureIntent.putExtra("resultCode", resultCode);
            screenCaptureIntent.putExtra("intentData", data);
            screenCaptureIntent.putExtra("screenDensity", mScreenDensity);
            startForegroundService(screenCaptureIntent);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Toast.makeText(this, "ScreenLife Capture is running!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
