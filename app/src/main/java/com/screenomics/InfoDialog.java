package com.screenomics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;


import androidx.fragment.app.DialogFragment;

public class InfoDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("SUTD ScreenLife Capture")
                .setMessage("Developed by researchers from the Singapore University of Technology and Design, SUTD ScreenLife Capture is a smartphone application that captures high frequency screenshots of your smartphone use. When in use, the data is securely encrypted and sent over to the research team at SUTD.\n\n" +
                        "This application is meant for research purposes only, and if you are not a consenting participant of a research study conducted by Singapore University of Technology and Design researchers, please delete this application immediately.")
                .setNeutralButton("OK", null);
        return builder.create();
    }
}
