package com.screenomics.services.upload;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.screenomics.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Batch {

    private final MediaType PNG = MediaType.parse("image/*");
    private final List<File> files;
    private final OkHttpClient client;

    Batch(List<File> files, OkHttpClient client) {
        this.files = files;
        this.client = client;
    }

    public String sendFiles() {
        MultipartBody.Builder bodyPart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).isFile()) {
                bodyPart.addFormDataPart("file" + (i + 1), files.get(i).getName(), RequestBody.create(PNG, files.get(i)));
            }
        }

        RequestBody body = bodyPart.build();
        Request request = new Request.Builder()
                .addHeader("Content-Type", "multipart/form-data")
                .url(Constants.UPLOAD_ADDRESS)
                .post(body)
                .build();

        Response response = null;
        try {
            long startTime = System.nanoTime();
            response = client.newCall(request).execute();
            System.out.println("Upload of " + files.size() + " files took " + (System.nanoTime() - startTime)/1000000 + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
        int code = response != null ? response.code() : 999;
        if (code >= 400  && code < 500) {
            System.out.println(response.toString());
        }
        if (response != null) response.close();
        return String.valueOf(code);
    }

    public void deleteFiles() {
        files.forEach(File::delete);
    }

    public int size() {
        return files.size();
    }

}
