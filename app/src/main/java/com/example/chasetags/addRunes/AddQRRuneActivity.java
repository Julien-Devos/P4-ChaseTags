package com.example.chasetags.addRunes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chasetags.dialogs.DeleteRuneDialog;
import com.example.chasetags.dialogs.EditGameRuneDialog;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.R;
import com.example.chasetags.SessionMenuActivity;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.example.chasetags.utils.OnEditGameRuneDialogListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class AddQRRuneActivity extends AppCompatActivity {

    // FOR GLOBAL RUNES USAGE
    private String runeID;
    private String ownerUID = "";
    Boolean updatingRune = false;
    FirebaseFirestore dbFirestore;

    // LAYOUT ITEMS (UI)
    Button confirmButton;
    FloatingActionButton deleteRune;
    EditText nameText;
    EditText locText;
    EditText scoreText;
    ImageView qr_image;
    Toolbar topAppBar;
    NavigationBarView bottomNavBar;
    TextInputLayout nameInputLayout;
    TextInputLayout clueInputLayout;

    Integer fromActivity;
    String intentRuneCode;

    SharedPreferences dialogPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_qr_rune);

        // Change navigation bar color since it's different in this activity
        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        // UI items
        confirmButton = findViewById(R.id.create_rune);
        deleteRune = findViewById(R.id.delete_rune);
        nameText = findViewById(R.id.new_rune_name);
        locText = findViewById(R.id.loc_clue);
        scoreText = findViewById(R.id.rune_score);
        qr_image = findViewById(R.id.qr_image);
        topAppBar = findViewById(R.id.toolbar);
        bottomNavBar = findViewById(R.id.bottom_navigation);
        nameInputLayout = findViewById(R.id.input_layout);
        clueInputLayout = findViewById(R.id.input_layout2);
        // TODO handle name, code and clue not completed with the InputLayout

        runeID = getIntent().getStringExtra(GlobalVariables.EXN_Rune_ID);
        if (runeID != null) {
            bottomNavBar.setVisibility(View.GONE);
        }

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";}}
            return null;
        };
        scoreText.setFilters(new InputFilter[]{filter});

        String runeName = getIntent().getStringExtra(GlobalVariables.Rune_name);
        intentRuneCode = getIntent().getStringExtra(GlobalVariables.Rune_code);
        String runeHint = getIntent().getStringExtra(GlobalVariables.Rune_hint);
        String runeScore = getIntent().getStringExtra(GlobalVariables.Rune_score);

        if (!runeName.isEmpty()) {
            nameText.setText(runeName);
        } if (!runeHint.isEmpty()) {
            locText.setText(runeHint);
        } if (!runeScore.isEmpty()){
            scoreText.setText(runeScore);
        }

        // Get DB instance
        dbFirestore = FirebaseFirestore.getInstance();

        // By default, the "delete" button is hidden. It can be toggled back on in the 'resume' methods
        deleteRune.setVisibility(View.GONE);

        int selectedNavItem = getIntent().getIntExtra(GlobalVariables.EXN_rune_nav_ID, -1);
        if (selectedNavItem != -1) {
            bottomNavBar.setSelectedItemId(selectedNavItem);
        }

        fromActivity = getIntent().getIntExtra(GlobalVariables.EXN_FROM, -1);

        dialogPreference = getSharedPreferences(GlobalVariables.PREFS_preference_dialogs,0);

        setupButtonsListeners();

        ownerUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setupToolbarNavigationListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (runeID != null) {
            deleteRune.setVisibility(View.VISIBLE);
            if (fromActivity == GlobalVariables.EXV_FROM_GAME){
                deleteRune.setImageResource(R.drawable.baseline_close_24);
            }
            updatingRune = true;

            DocumentReference runeRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID);
            runeRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Rune rune = document.toObject(Rune.class);

                            build_and_show_qr_code(qr_image, rune.getRuneID());
                            nameText.setText(rune.getName(), TextView.BufferType.EDITABLE);
                            locText.setText(rune.getLocalisationClue(), TextView.BufferType.EDITABLE);
                            scoreText.setText(rune.getScore(),TextView.BufferType.EDITABLE);
                            confirmButton.setText(R.string.saveRune);
                        } else {
                            Log.d("FirestoreDatabase", "No document");
                        }
                    } else {
                        Log.d("FirestoreDatabase", "get request failed with ", task.getException());
                    }
                }
            });
        }

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    @SuppressWarnings("Convert2Lambda")
    private void setupButtonsListeners() {
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Rune newRune = parseDataIntoRuneCode();
                if (newRune != null) {
                    if (updatingRune) {
                        dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID)
                                .get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            Rune rune = document.toObject(Rune.class);
                                            boolean changed = runeChanged(newRune, rune);

                                            if (changed) {
                                                editRune(newRune);
                                            } else {
                                                finish();
                                            }
                                        }
                                    } else {
                                        Log.d("FirestoreDatabase", "Rune successfully deleted!");
                                    }
                                });
                    } else {
                        dbFirestore.collection(GlobalVariables.Firestore_Collection_runes)
                                .document(newRune.getRuneID())
                                .set(newRune)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d("FirestoreDatabase", "Rune successfully added!");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("FirestoreDatabase", "Error while adding Rune", e);
                                    }
                                });

                        if (fromActivity.equals(GlobalVariables.EXV_FROM_GAME)) {
                            newRune.setChecked(true);
                            SessionMenuActivity.gameRunes.add(newRune);
                        }

                        finish();
                    }
                }
            }
        });

        deleteRune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (runeID != null) {
                    SharedPreferences dialogPreference = getSharedPreferences(GlobalVariables.PREFS_preference_dialogs,0);
                    boolean localDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_local,true);
                    boolean globalDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_global,true);

                    if (fromActivity == GlobalVariables.EXV_FROM_GAME) {

                        if (localDialog) {
                            // Delete rune from game when clicked on dialog
                            DeleteRuneDialog dialog = new DeleteRuneDialog();
                            dialog.setOnDeleteRuneListener(AddQRRuneActivity.this::deleteRuneFromGame);
                            dialog.show(getSupportFragmentManager(), GlobalVariables.tag_deleteRune_dialog, true);
                        } else {
                            deleteRuneFromGame();
                        }

                    } else {

                        if (globalDialog) {
                            // Delete rune from DB when clicked on dialog
                            DeleteRuneDialog dialog = new DeleteRuneDialog();
                            dialog.setOnDeleteRuneListener(AddQRRuneActivity.this::deleteRuneFromDB);
                            dialog.show(getSupportFragmentManager(), GlobalVariables.tag_deleteRune_dialog, false);
                        } else {
                            deleteRuneFromDB();
                        }
                    }
                }
            }
        });
    }

    private void editRune(Rune newRune) {
        if (fromActivity.equals(GlobalVariables.EXV_FROM_GAME)) {

            boolean editRuneDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_editGameRune,true);

            if (editRuneDialog) {
                EditGameRuneDialog dialog = new EditGameRuneDialog();
                dialog.setOnEditRuneListener(new OnEditGameRuneDialogListener() {
                    @Override
                    public void onEditRune() {
                        editGameRune(newRune);
                    }

                    @Override
                    public void onCancelEdit() {
                        finish();
                    }
                });
                dialog.show(getSupportFragmentManager(), GlobalVariables.tag_editRune_dialog);
            } else {
                editGameRune(newRune);
            }
        } else {
            editRuneFromDB(newRune);
            finish();
        }
    }

    private void editGameRune(Rune newRune) {
        for (Rune rune : SessionMenuActivity.gameRunes) {
            if (rune.getRuneID().equals(runeID)) {
                rune.setName(newRune.getName());
                rune.setLocalisationClue(newRune.getLocalisationClue());
                rune.setScore(newRune.getScore());
                break;
            }
        }
        editRuneFromDB(newRune);
        finish();
    }

    private void editRuneFromDB(Rune newRune) {
        DocumentReference docRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID);
        docRef.update(
                GlobalVariables.Firestore_Document_Rune_name, newRune.getName(),
                GlobalVariables.Firestore_Document_Rune_localisationClue, newRune.getLocalisationClue(),
                GlobalVariables.Firestore_Document_Rune_score, newRune.getScore());
    }

    private boolean runeChanged(Rune newRune, Rune rune) {
        return !newRune.getName().equals(rune.getName())
                || !newRune.getLocalisationClue().equals(rune.getLocalisationClue())
                || !newRune.getScore().equals(rune.getScore());
    }

    private void deleteRuneFromGame() {
        for (Rune rune : SessionMenuActivity.gameRunes) {
            if (rune.getRuneID().equals(runeID)) {
                SessionMenuActivity.gameRunes.remove(rune);
                break;
            }
        }
        finish();
    }

    private void deleteRuneFromDB() {
        dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("FirestoreDatabase", "Rune successfully deleted!");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("FirestoreDatabase", "Error while deleting Rune", e);
                    }
                });
        finish();
    }

    /**
     * Parses the user's inputs in the text fields of the code rune creation Android activity
     * and stores them into a rune object.
     * @return an instance that contains input data. Returns null if the rune name or code field are empty.
     */
    private Rune parseDataIntoRuneCode() {
        String name = nameText.getText().toString().trim();
        String loc = locText.getText().toString().trim();
        String score = scoreText.getText().toString();


        boolean isNotComplete = false;

        if (name.isEmpty()) {
            nameInputLayout.setErrorEnabled(true);
            nameInputLayout.setError(getString(R.string.rune_no_name));
            isNotComplete = true;
        } else {
            nameInputLayout.setErrorEnabled(false);
        }

        if (loc.isEmpty()) {
            clueInputLayout.setErrorEnabled(true);
            clueInputLayout.setError(getString(R.string.rune_no_hint));
            isNotComplete = true;
        } else {
            clueInputLayout.setErrorEnabled(false);
        }

        if (isNotComplete) {
            return null;
        }
        return new Rune(GlobalVariables.Rune_qr_object_type, ownerUID, name, "", loc,score);
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });



        bottomNavBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Integer itemID = item.getItemId();

                if (itemID == R.id.item_1) {
                    swap_add_rune_type(itemID, AddCodeRuneActivity.class);
                    return true;
                } else if (itemID == R.id.item_2) {
                    swap_add_rune_type(itemID, AddNFCRuneActivity.class);
                    return true;
                } else {
                    return true;
                }

            }
        });
    }

    private void swap_add_rune_type(Integer itemID, Class runeTypeClass) {
        Intent intent = new Intent(getApplicationContext(), runeTypeClass);
        if (fromActivity == GlobalVariables.EXV_FROM_GAME) {
            intent.putExtra(GlobalVariables.EXN_FROM, GlobalVariables.EXV_FROM_GAME);
        }

        // Used to keep name, code and hint through add Runes activities
        intent.putExtra(GlobalVariables.Rune_name, nameText.getText().toString().trim());
        intent.putExtra(GlobalVariables.Rune_code, intentRuneCode);
        intent.putExtra(GlobalVariables.Rune_hint, locText.getText().toString().trim());
        intent.putExtra(GlobalVariables.Rune_score, scoreText.getText().toString().trim());

        intent.putExtra(GlobalVariables.EXN_Owner_ID, ownerUID);
        intent.putExtra(GlobalVariables.EXN_rune_nav_ID, itemID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void build_and_show_qr_code(ImageView imageCode, String parsedString) {
        MultiFormatWriter mWriter = new MultiFormatWriter();
        try {
            BitMatrix mMatrix = mWriter.encode(parsedString, BarcodeFormat.QR_CODE, 1000,1000);
            BarcodeEncoder mEncoder = new BarcodeEncoder();
            Bitmap mBitmap = mEncoder.createBitmap(mMatrix);
            imageCode.setImageBitmap(mBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}