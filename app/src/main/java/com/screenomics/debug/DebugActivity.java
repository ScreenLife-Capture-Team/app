package com.screenomics.debug;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.screenomics.R;

import java.io.File;

public class DebugActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);

        String dir = getApplicationContext().getExternalFilesDir(null).getAbsolutePath() +
                "/images";
        File directory = new File(dir);
        File[] files = directory.listFiles();
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        if (chosenFile != null) {
            ImageView imageView = findViewById(R.id.imageView);
            Bitmap myBitmap = BitmapFactory.decodeFile(chosenFile.getAbsolutePath());

            imageView.setImageBitmap(myBitmap);
        }
    }
}
