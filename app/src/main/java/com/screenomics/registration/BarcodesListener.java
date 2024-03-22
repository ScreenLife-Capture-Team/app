package com.screenomics.registration;

import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.List;

public interface BarcodesListener {
    void invoke(List<Barcode> barcodes);
    void qrCodeNotFound();
}