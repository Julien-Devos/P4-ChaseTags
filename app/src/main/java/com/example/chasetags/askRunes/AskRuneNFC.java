package com.example.chasetags.askRunes;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.example.chasetags.R;
import com.example.chasetags.utils.GlobalVariables;

import es.dmoral.toasty.Toasty;

public class AskRuneNFC extends AskRuneGeneric implements NfcAdapter.ReaderCallback {

    private NfcAdapter mNfcAdapter;
    Context context;
    Ndef scannedTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(GlobalVariables.Rune_nfc_object_type);

        context = getApplicationContext();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mNfcAdapter!= null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            mNfcAdapter.enableReaderMode(this, this,
                    NfcAdapter.FLAG_READER_NFC_A |
                    NfcAdapter.FLAG_READER_NFC_B |
                    NfcAdapter.FLAG_READER_NFC_F |
                    NfcAdapter.FLAG_READER_NFC_V |
                    NfcAdapter.FLAG_READER_NFC_BARCODE |
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, options);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // Transform nfc id from bytes to string
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (byte b : src) {
            buffer[0] = Character.forDigit((b >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(b & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        scannedTag = Ndef.get(tag);

        // Check that it is an Ndef capable card
        if (scannedTag != null) {

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));

            String tagID = bytesToHexString(scannedTag.getTag().getId());

            runOnUiThread(() -> {
                if (super.isRuneInputInRunesList(tagID)) {
                    if (super.hasRuneAlreadyBeenEntered(tagID)) {
                        Toasty.warning(context, getString(R.string.toast_rune_already_entered), Toasty.LENGTH_SHORT, true).show();
                    } else {
                        handleSuccess(tagID);
                    }
                } else {
                    Toasty.warning(context, getString(R.string.toast_rune_enter_fail), Toasty.LENGTH_SHORT, true).show();
                }
            });
        }
    }
}