package com.screenomics.services.upload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.screenomics.Constants;
import com.screenomics.R;
import com.screenomics.util.InternetConnection;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


public class UploadService extends Service {

    private final OkHttpClient client =
            new OkHttpClient.Builder().readTimeout(Constants.REQ_TIMEOUT, TimeUnit.SECONDS).build();
    public int numToUpload = 0;
    public int numUploaded = 0;
    public int numTotal = 0;
    public boolean uploading = false;
    public Status status = Status.IDLE;
    public String errorCode = "";
    public String lastActivityTime = "";
    public boolean continueWithoutWifi = false;

    private int numBatchesSending = 0;
    private int numBatchesToSend = 1;
    private final List<Batch> batches = new ArrayList<>();
    private LocalDateTime startDateTime;
    private final FileFilter onlyFilesBeforeStart = new FileFilter() {
        @Override
        public boolean accept(File file) {
            List<String> parts = Arrays.asList(file.getName().replace(".png", "").split("_"));
            // May 2nd fix: move 1 back in the list because of new descriptor component
            Integer[] dP =
                    parts.subList(parts.size() - 7, parts.size() - 1).stream().map(Integer::valueOf).toArray(Integer[]::new);
            LocalDateTime imageCreateTime = LocalDateTime.of(dP[0], dP[1], dP[2], dP[3], dP[4],
                    dP[5]);
            return imageCreateTime.isBefore(startDateTime);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void sendNextBatch() {
        if (batches.isEmpty()) return;
        if (numBatchesSending >= numBatchesToSend) return;
        if (!continueWithoutWifi && !InternetConnection.checkWiFiConnection(this))
            sendFailure("NOWIFI");
        Batch batch = batches.remove(0);
        numBatchesSending++;
        System.out.println("SENDING NEXT BATCH, numBatchesSending " + numBatchesSending + " out " +
                "of " + numBatchesToSend + " with " + batch.size() + " images");
        new Sender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, batch);
    }

    private void sendSuccessful(Batch batch) {
        numBatchesSending--;
        numUploaded += batch.size();
        numToUpload -= batch.size();

        if (numBatchesToSend < Constants.MAX_BATCHES_TO_SEND) numBatchesToSend++;
        for (int i = 0; i < numBatchesToSend; i++) {
            sendNextBatch();
        }
        if (numToUpload <= 0) {
            System.out.println("Sending Successful!");
            status = Status.SUCCESS;
            reset();
            stopForeground(true);
            stopSelf();
        }
    }

    private void sendFailure(String code) {
        status = Status.FAILED;
        errorCode = code;
        setNotification("Failure in Uploading", "Error code: " + errorCode);
        reset();
    }

    private void reset() {
        ZonedDateTime dateTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        lastActivityTime = dateTime.format(formatter);
        uploading = false;
        numBatchesToSend = 0;
        numTotal = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancelAll();

        createNotificationChannel();
        Notification notification = setNotification("Uploading..", "Preparing..");

        // Notification ID cannot be 0.
        startForeground(5, notification);

        if (intent == null || status == Status.SENDING) {
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        String dirPath = intent.getStringExtra("dirPath");
        continueWithoutWifi = intent.getBooleanExtra("continueWithoutWifi", false);

        startDateTime = LocalDateTime.now();
        File dir = new File(dirPath);
        File[] files = dir.listFiles(onlyFilesBeforeStart);


        if (files == null || files.length == 0) {
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        LinkedList<File> fileList = new LinkedList<>(Arrays.asList(files));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int batchSize = prefs.getInt("batchSize", Constants.BATCH_SIZE_DEFAULT);
        int maxToSend = prefs.getInt("maxSend", Constants.MAX_TO_SEND_DEFAULT);

        numToUpload = 0;

        System.out.println("INITIAL FILELIST SIZE " + fileList.size() + "");

        // Split the files into batches.
        while (fileList.size() > 0 && (maxToSend == 0 || numToUpload < maxToSend)) {
            List<File> nextBatch = new LinkedList<>();
            for (int i = 0; i < batchSize; i++) {
                if (fileList.peek() == null) {
                    break;
                }
                if (maxToSend != 0 && numToUpload == maxToSend) {
                    break;
                }
                numToUpload++;
                nextBatch.add(fileList.remove());
            }
            Batch batch = new Batch(nextBatch, client);
            batches.add(batch);
        }

        numTotal = numToUpload;
        numUploaded = 0;
        numBatchesToSend = 1;
        System.out.println("GOT " + batches.size() + " BATCHES WITH " + numToUpload + "IMAGES TO " +
                "UPLOAD");
        System.out.println("TOTAL OF " + fileList.size() + " IMAGES THO");

        status = Status.SENDING;
        // Send the first batch.F
        sendNextBatch();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "uploading-channel",
                    "Screenomics Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification setNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, UploadService.class);

        int intentflags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentflags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            intentflags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, intentflags);
        Notification notification =
                new Notification.Builder(this, "uploading-channel")
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(R.drawable.dna)
                        .setContentIntent(pendingIntent)
                        .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(5, notification);
        return notification;
    }

    public enum Status {
        IDLE, SENDING, FAILED, SUCCESS
    }

    public class Sender extends AsyncTask<Batch, Integer, Void> {
        @Override
        protected Void doInBackground(Batch... batches) {
            String code = batches[0].sendFiles();
            if (code.equals("201")) {
                batches[0].deleteFiles();
                sendSuccessful(batches[0]);
            } else {
                sendFailure(code);
            }
            return null;
        }
    }

    public class LocalBinder extends Binder {
        public UploadService getService() {
            return UploadService.this;
        }
    }
}
