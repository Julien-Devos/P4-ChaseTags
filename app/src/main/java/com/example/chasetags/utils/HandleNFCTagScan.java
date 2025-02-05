package com.example.chasetags.utils;

import android.nfc.NfcAdapter;
import android.os.Bundle;


public abstract class HandleNFCTagScan {

    public static void ReadNFCTags(android.app.Activity activity) {
        NfcAdapter deviceNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (deviceNfcAdapter != null) {
            // Handle nfc to not open default window if nfc scanned
            deviceNfcAdapter.enableReaderMode(activity, tag -> {},
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, new Bundle());
        }
    }


}
