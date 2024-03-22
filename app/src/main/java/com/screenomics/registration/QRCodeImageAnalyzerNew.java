package com.screenomics.registration;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import android.media.Image;
import android.util.Log;

import java.util.List;


public class QRCodeImageAnalyzerNew implements ImageAnalysis.Analyzer {
    private final BarcodeScanner barcodeScanner = createBarcodeScanner();
    private final BarcodesListener barcodesListener;

    public QRCodeImageAnalyzerNew(BarcodesListener barcodesListener) {
        this.barcodesListener = barcodesListener;
    }
//
//    @FunctionalInterface
//    public interface BarcodesListener {
//        void invoke(List<Barcode> barcodes);
//    }


    @ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        InputImage inputImage = InputImage.fromMediaImage(image, rotationDegrees);
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(this::onQRCodeFound)
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onQRCodeFound(List<Barcode> barcodes) {
        if (barcodes != null && !barcodes.isEmpty()) {
            Log.d("QRCodeImageAnalyzer_res", String.valueOf(barcodes));
            barcodesListener.invoke(barcodes);
        }
    }

    private BarcodeScanner createBarcodeScanner() {
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
//                        .setBarcodeFormats(
//                                Barcode.FORMAT_QR_CODE,
//                                Barcode.FORMAT_AZTEC)
                        .build();
        return BarcodeScanning.getClient(options);
    }
}

