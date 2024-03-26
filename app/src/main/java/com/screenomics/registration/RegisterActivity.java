package com.screenomics.registration;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.screenomics.MainActivity;
import com.screenomics.R;
import com.screenomics.util.Converter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RegisterActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_CAM_NOTIF = 1;

    private String key;
    private String hash;
    private PreviewView previewView;
    private Button continueButton;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        setSupportActionBar(findViewById(R.id.registerToolbar));

        previewView = findViewById(R.id.qrPreview);
        continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commitSharedPreferences();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                RegisterActivity.this.startActivity(intent);
                finish();
            }
        });
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        requestCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.registration_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.manualInputOption) {
            showManualInputDialog();
        }

        if (id == R.id.testModeOption) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("state", 1);
            editor.apply();

            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            RegisterActivity.this.startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showManualInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manual Input");
        builder.setMessage("Please scan the QR code using your phone's camera or third-party QR " +
                "code scanner, copy the code, and paste the result below.");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();
                setQR(m_Text);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            // provision exclusively for API 33
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    ActivityCompat.requestPermissions(RegisterActivity.this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.POST_NOTIFICATIONS},
                            PERMISSION_REQUEST_CAM_NOTIF);
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.POST_NOTIFICATIONS},
                            PERMISSION_REQUEST_CAM_NOTIF);
                }

            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    ActivityCompat.requestPermissions(RegisterActivity.this,
                            new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                }
            }

        }
    }

//    private void requestNotifications() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
//        PackageManager.PERMISSION_GRANTED) {
//            startCamera();
//        } else {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
//            .POST_NOTIFICATIONS)) {
//                ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{Manifest
//                .permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_NOTIFICATIONS);
//            } else {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
//                .POST_NOTIFICATIONS}, PERMISSION_REQUEST_NOTIFICATIONS);
//            }
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAM_NOTIF) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Please allow both camera and notifications permissions",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // below API 33
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Please allow camera permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // TODO: try messing with this
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

//        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeImageAnalyzer
//        (new QRCodeFoundListener() {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new QRCodeImageAnalyzerNew(new BarcodesListener() {
//            @Override
//            public void onQRCodeFound(String _qrCode) {
//                setQR(_qrCode);
//            }

            @Override
            public void invoke(List<Barcode> barcodes) {
                String scannedQRCodeText = barcodes.get(0).getRawValue();
                setQR(scannedQRCodeText);
            }


            @Override
            public void qrCodeNotFound() {
            }
        }));

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis,
                preview);
    }

    private void setQR(String _qrCode) {
        if (_qrCode.length() == 64) {
            key = _qrCode;
            try {
                System.out.println("getSHA " + getSHA(key));
                System.out.println("hash " + toHexString(getSHA(key)));
                hash = toHexString(getSHA(key));
                continueButton.setText("Verification code: " + hash.substring(0, 4));
                continueButton.setEnabled(true);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    private void commitSharedPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("state", 1);
        editor.putString("key", key);
        editor.putString("hash", hash);
        editor.apply();
    }


    private byte[] getSHA(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Converter.hexStringToByteArray(input));
        return md.digest();
    }

    private String toHexString(byte[] hash) {
        BigInteger num = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(num.toString(16));
        while (hexString.length() < 64) {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK)  //Override Keyback to do nothing in this case.
        {
            return true;
        }
        return super.onKeyDown(keyCode, event);  //-->All others key will work as usual
    }

}
