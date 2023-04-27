package com.screenomics;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class CaptureService extends Service {
    private static final String TAG = "Screencapture";
    private static final String CHANNEL_ID = "screenomics_id";
    private static final DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private static Intent intent;
    private static int resultCode;
    private static int screenDensity;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ImageReader mImageReader;
    private KeyguardManager mKeyguardManager;
    private VirtualDisplay mVirtualDisplay;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private Runnable captureInterval;
    private Runnable insertStartImage;
    private Runnable insertPauseImage;
    private Handler mHandler = new Handler();
    public static byte[] key;
    private static ByteBuffer buffer;
    private static int pixelStride;
    private static int rowPadding;
    private static boolean capture = false;

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                buffer = planes[0].getBuffer();
                pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                rowPadding = rowStride - pixelStride * DISPLAY_WIDTH;
                image.close();
            }
        }
    }

    private void encryptImage(Bitmap bitmap, String descriptor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hash = prefs.getString("hash", "00000000").substring(0, 8);
        String keyRaw = prefs.getString("key", "");
        byte[] key = Converter.hexStringToByteArray(keyRaw);
        FileOutputStream fos = null;
        Date date = new Date();
        String dir = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        String screenshot = "/" + hash + "_" + sdf.format(date) + "_" + descriptor + ".png";

        try {
            fos = new FileOutputStream(dir + "/images" + screenshot);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            try {
                Encryptor.encryptFile(key, screenshot, dir + "/images" + screenshot, dir + "/encrypt" + screenshot);
                Log.i(TAG, "Encryption done");
            } catch (Exception e) {
                e.printStackTrace();
            }
            File f = new File(dir+"/images"+screenshot);
            if (f.delete()) Log.e(TAG, "file deleted: " + dir +"/images" + screenshot);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bitmap.recycle();
        }
    }

    // Called when Screen Cast is disabled
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e(TAG, "I'm stopped");
            try {
                stopCapturing();
                destroyImageReader();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();



        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        captureInterval = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
//                TODO what's this
                if (!capture) return;
                if (buffer != null && !mKeyguardManager.isKeyguardLocked()) {
                    Bitmap bitmap = Bitmap.createBitmap(DISPLAY_WIDTH + rowPadding / pixelStride, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    encryptImage(bitmap, "");
                    buffer.rewind();
                }
                mHandler.postDelayed(captureInterval, 5000);
            }
        };

        // To insert a 'start capture' image
        insertStartImage = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
//                if (!capture) return;
                InputStream is = getResources().openRawResource(R.raw.resumerecord);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                encryptImage(bitmap, "resume");

            }
        };

        insertPauseImage = new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
//                if (!capture) return;
                InputStream is = getResources().openRawResource(R.raw.pauserecord);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                encryptImage(bitmap, "pause");

            }
        };



    }


    @Override
    public int onStartCommand(Intent receivedIntent, int flags, int startId) {
        if (receivedIntent != null) {
            resultCode = receivedIntent.getIntExtra("resultCode", -1);
            intent = receivedIntent.getParcelableExtra("intentData");
            screenDensity = receivedIntent.getIntExtra("screenDensity", 0);
        }

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int intentflags;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            intentflags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }else{
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, intentflags);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.dna)
                .setContentTitle("ScreenLife Capture is currently enabled")
                .setContentText("If this notification disappears, please re-enable it from the application.")
                .setContentIntent(pendingIntent)
                .build();

        Log.i(TAG, "Starting foreground service");
        startForeground(1, notification);
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, intent);
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        createVirtualDisplay();

        startCapturing();
        return START_REDELIVER_INTENT;
    }

    private void startCapturing() {
        try {
            buffer = null;
            capture = true;
            // TODO: send a 'start capture' image
            mHandler.post(insertStartImage);
            mHandler.post(captureInterval);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createVirtualDisplay() {
        if (mMediaProjection != null) {
            mImageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, PixelFormat.RGBA_8888, 2);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, DISPLAY_WIDTH, DISPLAY_HEIGHT, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
            mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
        }
    }

    private void stopCapturing() {
        // TODO: send a 'stop capture' image to storage
        mHandler.post(insertPauseImage);
        capture = false;
        mHandler.removeCallbacksAndMessages(captureInterval);
        destroyImageReader();
        destroyVirtualDisplay();
        destroyMediaProjection();
    }

    // Called on intentionally stopping the screen capture
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapturing();
        Log.e(TAG, "I'm destroyed!");
    }

    private void destroyImageReader() {
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
        }
        Log.i(TAG, "ImageReader stopped");
    }

    private void destroyVirtualDisplay() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        Log.i(TAG, "VirtualDisplay stopped");
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection stopped");
        int intentflags;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            intentflags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }else{
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, intentflags);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ScreenLife Capture is NOT Running!")
                .setContentText("Please restart the application!")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        capture = false;
    }

    public class LocalBinder extends Binder {
        CaptureService getService() { return CaptureService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return new LocalBinder(); }

    public boolean isCapturing() { return capture; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screenomics Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
