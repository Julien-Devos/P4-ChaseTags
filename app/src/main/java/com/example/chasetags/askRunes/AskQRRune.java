package com.example.chasetags.askRunes;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.example.chasetags.customCapture.CaptureActivityPortrait;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.R;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import es.dmoral.toasty.Toasty;

public class AskQRRune extends AskRuneGeneric {
    ActivityResultLauncher<ScanOptions> launcher;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(GlobalVariables.Rune_qr_object_type);

        initLauncher();
        scanCode();
    }

    private void initLauncher() {
        launcher = registerForActivityResult(new ScanContract(), res -> {
            if (res == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Error");
                builder.setMessage(getString(R.string.error_qr_rune_code));
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                }).show();
            }
            else {
                // Vibrate when QR is scanned
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));

                if(super.isRuneInputInRunesList(res.getContents())) {
                    if (super.hasRuneAlreadyBeenEntered(res.getContents())) {
                        Toasty.warning(AskQRRune.this, getString(R.string.toast_rune_already_entered), Toasty.LENGTH_SHORT, true).show();
                        finish();
                    }
                    else {
                        handleSuccess(res.getContents());
                    }
                }
                else {
                    Toasty.warning(AskQRRune.this, getString(R.string.toast_rune_enter_fail), Toasty.LENGTH_SHORT, true).show();
                    finish();
                }
            }
        });
    }

    private void scanCode() {
        ScanOptions scan_options = new ScanOptions();
        scan_options.setPrompt("Scan a QR Code");
        scan_options.setOrientationLocked(true);
        scan_options.setBeepEnabled(false);
        scan_options.setCaptureActivity(CaptureActivityPortrait.class);
        launcher.launch(scan_options);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }
}