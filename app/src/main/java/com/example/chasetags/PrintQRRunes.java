package com.example.chasetags;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.chasetags.adapters.SelectRuneAdapter;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrintQRRunes extends AppCompatActivity {
    private ArrayList<Rune> runesList;
    private ListView listView;
    private SelectRuneAdapter adapter;
    TextView noGlobalRunes;
    Toolbar topAppBar;

    Context context;

    String noQRRunes;

    int pageHeight = 1120;
    int pageWidth = 792;
    private static final int PERMISSION_REQUEST_CODE = 999;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_qr_rune);

        noGlobalRunes = findViewById(R.id.no_qr_runes);
        noGlobalRunes.setVisibility(View.INVISIBLE);
        context = this.getBaseContext();

        topAppBar = (Toolbar) findViewById(R.id.topAppBar);

        listView = (ListView) findViewById(R.id.list);

        int fromSession = getIntent().getIntExtra("Session", -1);
        runesList = new ArrayList<>();
        List<Rune> runesListFrom;
        if(fromSession == 1) {
            runesListFrom = SessionMenuActivity.gameRunes;
            noQRRunes = getString(R.string.no_qr_runes_in_game);
        } else {
            runesListFrom = ManageProfileRunes.playerRunesList;
            noQRRunes = getString(R.string.no_qr_runes);
        }

        noGlobalRunes.setText(noQRRunes);

        for (Rune rune: runesListFrom) {
            if(rune.getType().equals(GlobalVariables.Rune_qr_object_type))
                runesList.add(rune);
        }

        adapter = new SelectRuneAdapter(runesList,PrintQRRunes.this);

        setupView();
        setupButtonListener();
        setupToolbarNavigationListener();
    }

    private void setupView() {
        if (runesList.isEmpty())
            noGlobalRunes.setVisibility(View.VISIBLE);
        else
            setupListAdapter();
    }

    private void setupListAdapter(){
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Rune rune = runesList.get(position);

                Snackbar.make(view, getString(R.string.name_of_the_rune) + ": " + rune.getName() +
                        "\n"+ getString(R.string.hint_of_the_rune) + ": " +rune.getLocalisationClue(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupButtonListener(){
        Button saveButton = findViewById(R.id.print_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                procedurePrintPdf();
                finish();
            }
        });
    }

    private void procedurePrintPdf() {
        if(!checkPermission()) requestPermission();

        PdfDocument pdf = createPdfDocument();

        savePdfIntoUserStorage(pdf);
    }

    private PdfDocument createPdfDocument() {
        PdfDocument pdf = new PdfDocument();

        Paint paint = new Paint();
        Paint title = new Paint();

        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextSize(20);

        Rune[] runesToPrint = initRunesToPrint();
        // 1, 4, 9, 16, ...
        int numberOfQRCodePerPage = 9;
        int numberOfQRCodePerLine = (int) Math.sqrt(numberOfQRCodePerPage);
        int numberOfPages = ((runesToPrint.length - 1) / numberOfQRCodePerPage) + 1;

        for (int page = 1; page <= numberOfPages ; page++) {
            PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.
                    Builder(pageWidth, pageHeight, page).create();
            PdfDocument.Page myPage = pdf.startPage(myPageInfo);

            Canvas canvas = myPage.getCanvas();

            int horizontal_padding = 15;

            int start_index = (numberOfQRCodePerPage * (page - 1));
            int finish_index = numberOfQRCodePerPage + (numberOfQRCodePerPage * (page-1));
            if(finish_index > runesToPrint.length) finish_index = runesToPrint.length;

            for (int index = start_index; index < finish_index; index++) {
                Rune rune = runesToPrint[index];

                Bitmap qr = build_qr_code(rune);

                int position = index % numberOfQRCodePerPage;
                int x;
                switch (position % numberOfQRCodePerLine) {
                    case 0: x = horizontal_padding;
                            break;
                    case 1: x = pageWidth/numberOfQRCodePerLine + horizontal_padding;
                            break;
                    case 2: x = 2* (pageWidth/numberOfQRCodePerLine) + horizontal_padding;
                            break;
                    default: x = 0;
                }
                int x_test = x + 30;

                int vertical_padding = (320 * (position/numberOfQRCodePerLine) );
                int y_text = 30 + vertical_padding;
                int y_bitmap = 40 + vertical_padding;

                canvas.drawText(rune.getName(), x_test, y_text, title);
                canvas.drawBitmap(qr, x, y_bitmap, paint);
            }

            pdf.finishPage(myPage);
        }

        return pdf;
    }

    private void savePdfIntoUserStorage(PdfDocument pdf) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(directory, "QRCodes.pdf");

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            pdf.writeTo(outputStream);
            outputStream.close();

            String pdf_saved = getString(R.string.pdf_saved_successfully) + " " + file.getAbsolutePath();
            Toast.makeText(this, pdf_saved, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pdf.close();

        // Open file when its downloaded
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri URI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        intent.setDataAndType(URI, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private boolean checkPermission() {
        int permissionWrite = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int permissionRead = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return (permissionWrite == PackageManager.PERMISSION_GRANTED) &&
               (permissionRead == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private Rune[] initRunesToPrint() {
        // Get number of runes we have to print
        int numberOfPrintedRunes = 0;
        for (Rune rune: runesList) {
            if(rune.isChecked()) {
                numberOfPrintedRunes++;
            }
        }

        // Copy the runes we have to print in an array
        Rune[] runesToPrint = new Rune[numberOfPrintedRunes];
        int indexArray = 0;
        for (Rune rune: runesList) {
            if(rune.isChecked()) {
                runesToPrint[indexArray] = rune;
                indexArray++;
            }
        }

        return runesToPrint;
    }

    private Bitmap build_qr_code(Rune runeQR) {
        MultiFormatWriter mWriter = new MultiFormatWriter();
        Bitmap mBitmap = null;
        try {
            BitMatrix mMatrix = mWriter.encode(runeQR.getRuneID(), BarcodeFormat.QR_CODE, 200,200);
            BarcodeEncoder mEncoder = new BarcodeEncoder();
            mBitmap = mEncoder.createBitmap(mMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return mBitmap;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }
}
