package com.screenomics.services.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.screenomics.Constants;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SenderWorker extends Worker {
    public SenderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }
    enum Status {
        IDLE, SENDING, FAILED, SUCCESS
    }

    public int numToUpload = 0;
    public int numUploaded = 0;
    public int numTotal = 0;
    public boolean uploading = false;
    public String errorCode = "";
    public String lastActivityTime = "";
    public boolean continueWithoutWifi = false;

    private int numBatchesSending = 0;
    private int numBatchesToSend = 1;
    private List<Batch> batches = new ArrayList<>();

    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(Constants.REQ_TIMEOUT, TimeUnit.SECONDS).build();
    private LocalDateTime startDateTime;

    @Override
    public Result doWork() {

        final List<Number> hours = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        if (!hours.contains(DateTime.now().getHourOfDay())) {
            return Result.success();
        }
        System.out.println("Starting upload!");
        Context context = getApplicationContext();
        File f_encrypt = new File(context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "encrypt");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        startDateTime = LocalDateTime.now();
        File dir = new File(f_encrypt.getAbsolutePath());
        File[] files = dir.listFiles(onlyFilesBeforeStart);

        if (files == null || files.length == 0) {
            return Result.success();
        }

        LinkedList<File> fileList = new LinkedList<>(Arrays.asList(files));

        int batchSize = prefs.getInt("batchSize", Constants.BATCH_SIZE_DEFAULT);
        int maxToSend = prefs.getInt("maxSend", Constants.MAX_TO_SEND_DEFAULT);

        numToUpload = 0;

        // Split the files into batches.
        while (fileList.size() > 0 && (maxToSend == 0 || numToUpload < maxToSend)) {
            List<File> nextBatch = new LinkedList<>();
            for (int i = 0; i < batchSize; i++) {
                if (fileList.peek() == null) { break; }
                if (maxToSend != 0 && numToUpload == maxToSend) { break; }
                numToUpload ++;
                nextBatch.add(fileList.remove());
            }
            Batch batch = new Batch(nextBatch, client);
            batches.add(batch);
        }

        numTotal = numToUpload;
        System.out.println("GOT " + batches.size() + " BATCHES WITH " + numToUpload + "IMAGES TO UPLOAD" );
        System.out.println("TOTAL OF " + fileList.size() + " IMAGES THO");

        Log.d("SenderWorker", "sending batches: " + batches);

        for (Batch batch : batches) {
            String code = batch.sendFiles();
            if (code.equals("201")) {
                batch.deleteFiles();
            }
        }

        // ZonedDateTime dateTime = ZonedDateTime.now();
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        // lastActivityTime = dateTime.format(formatter);
        return Result.success();
    }

    private final FileFilter onlyFilesBeforeStart = new FileFilter() {
        @Override
        public boolean accept(File file) {
            List<String> parts = Arrays.asList(file.getName().replace(".png", "").split("_"));
            Integer[] dP = parts.subList(parts.size() - 7, parts.size() - 1).stream().map(Integer::valueOf).toArray(Integer[]::new);
            LocalDateTime imageCreateTime = LocalDateTime.of(dP[0], dP[1], dP[2], dP[3], dP[4], dP[5]);
            return imageCreateTime.isBefore(startDateTime);
        }
    };
}
