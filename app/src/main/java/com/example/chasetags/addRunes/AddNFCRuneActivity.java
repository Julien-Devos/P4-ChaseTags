package com.example.chasetags.addRunes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chasetags.dialogs.DeleteRuneDialog;
import com.example.chasetags.dialogs.EditGameRuneDialog;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.R;
import com.example.chasetags.SessionMenuActivity;
import com.example.chasetags.runes.Rune;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;


public class AddNFCRuneActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    Context context;

    ImageView noTagLinkedIcon;
    ImageView tagLinkedIcon;
    TextView noTagLinkedText;
    TextView tagLinkedText;
    TextView scanInstructions;
    Button confirmButton;
    FloatingActionButton deleteRune;
    Toolbar topAppBar;
    NavigationBarView bottomNavBar;
    EditText runeNameEditText;
    EditText runeClueEditText;
    EditText scoreText;
    TextInputLayout nameInputLayout;
    TextInputLayout clueInputLayout;
    ProgressBar tagLoading;

    SharedPreferences dialogPreference;

    Rune gotRune = null;

    Ndef scannedTag;
    private NfcAdapter mNfcAdapter;

    FirebaseFirestore dbFirestore;

    // FOR GLOBAL RUNES USAGE
    private String runeID;
    private String ownerUID = "";
    Boolean updatingRune = false;

    Integer fromActivity;
    String intentRuneCode;
    String runeTagID = "";

    ArrayList<Rune> playerRunesList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_nfc_rune);

        // Change navigation bar color since it's different in this activity
        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        context = getApplicationContext();

        topAppBar = findViewById(R.id.topAppBar);
        bottomNavBar = findViewById(R.id.bottom_navigation);
        scanInstructions = findViewById(R.id.scan_instructions);
        runeNameEditText = findViewById(R.id.new_rune_name);
        runeClueEditText = findViewById(R.id.loc_clue);
        scoreText = findViewById(R.id.rune_score);
        confirmButton = findViewById(R.id.create_rune);
        deleteRune = findViewById(R.id.delete_rune);
        nameInputLayout = findViewById(R.id.input_layout);
        clueInputLayout = findViewById(R.id.input_layout2);
        noTagLinkedIcon = findViewById(R.id.no_tag_scanned_icon);
        tagLinkedIcon = findViewById(R.id.scanned_tag_icon);
        noTagLinkedText = findViewById(R.id.no_tag_linked_text);
        tagLinkedText = findViewById(R.id.tag_linked_text);
        tagLoading = findViewById(R.id.tag_loading);

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";}}
            return null;
        };
        scoreText.setFilters(new InputFilter[]{filter});


        runeID = getIntent().getStringExtra(GlobalVariables.EXN_Rune_ID);
        if (runeID != null) {
            bottomNavBar.setVisibility(View.GONE);
        }

        String runeName = getIntent().getStringExtra(GlobalVariables.Rune_name);
        intentRuneCode = getIntent().getStringExtra(GlobalVariables.Rune_code);
        String runeHint = getIntent().getStringExtra(GlobalVariables.Rune_hint);
        String runeScore = getIntent().getStringExtra(GlobalVariables.Rune_score);
        if (!runeName.isEmpty()) {
            runeNameEditText.setText(runeName);
        } if (!runeHint.isEmpty()) {
            runeClueEditText.setText(runeHint);
        } if(!runeScore.isEmpty()){
            scoreText.setText(runeScore);
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

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

    private void setupButtonsListeners() {

        confirmButton.setOnClickListener(view -> {

            gotRune = parseDataIntoRuneCode();

            if (gotRune != null) {

                if (gotRune.getValue().isEmpty()) {
                    scanInstructions.setVisibility(View.VISIBLE);
                } else if (updatingRune) {
                    dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID)
                            .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Rune rune = document.toObject(Rune.class);
                                        boolean changed = runeChanged(gotRune, rune);

                                        if (changed) {
                                            editRune();
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
                            .document(gotRune.getRuneID())
                            .set(gotRune)
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
                        gotRune.setChecked(true);
                        SessionMenuActivity.gameRunes.add(gotRune);
                    }

                    finish();

                }
            }
        });

        deleteRune.setOnClickListener(view -> {

            if (runeID != null) {
                SharedPreferences dialogPreference = getSharedPreferences(GlobalVariables.PREFS_preference_dialogs,0);
                boolean localDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_local,true);
                boolean globalDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_global,true);

                if (fromActivity == GlobalVariables.EXV_FROM_GAME) {

                    if (localDialog) {
                        // Delete rune from game when clicked on dialog
                        DeleteRuneDialog dialog = new DeleteRuneDialog();
                        dialog.setOnDeleteRuneListener(AddNFCRuneActivity.this::deleteRuneFromGame);
                        dialog.show(getSupportFragmentManager(), GlobalVariables.tag_deleteRune_dialog, true);
                    } else {
                        deleteRuneFromGame();
                    }

                } else {

                    if (globalDialog) {
                        // Delete rune from DB when clicked on dialog
                        DeleteRuneDialog dialog = new DeleteRuneDialog();
                        dialog.setOnDeleteRuneListener(AddNFCRuneActivity.this::deleteRuneFromDB);
                        dialog.show(getSupportFragmentManager(), GlobalVariables.tag_deleteRune_dialog, false);
                    } else {
                        deleteRuneFromDB();
                    }
                }
            }
        });
    }

    private void editRune() {
        if (fromActivity.equals(GlobalVariables.EXV_FROM_GAME)) {

            boolean editRuneDialog = dialogPreference.getBoolean(GlobalVariables.PREFS_showDialog_editGameRune,true);

            if (editRuneDialog) {
                EditGameRuneDialog dialog = new EditGameRuneDialog();
                dialog.setOnEditRuneListener(new OnEditGameRuneDialogListener() {
                    @Override
                    public void onEditRune() {
                        editGameRune(gotRune);
                    }

                    @Override
                    public void onCancelEdit() {
                        finish();
                    }
                });
                dialog.show(getSupportFragmentManager(), GlobalVariables.tag_editRune_dialog);
            } else {
                editGameRune(gotRune);
            }
        } else {
            editRuneFromDB();
            finish();
        }
    }

    private void editGameRune(Rune newRune) {
        for (Rune rune : SessionMenuActivity.gameRunes) {
            if (rune.getRuneID().equals(runeID)) {
                rune.setName(newRune.getName());
                rune.setValue(newRune.getValue());
                rune.setLocalisationClue(newRune.getLocalisationClue());
                rune.setScore(newRune.getScore());
                break;
            }
        }
        editRuneFromDB();
        finish();
    }

    private void editRuneFromDB() {
        DocumentReference docRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes).document(runeID);
        docRef.update(
                GlobalVariables.Firestore_Document_Rune_name, gotRune.getName(),
                GlobalVariables.Firestore_Document_Rune_value, gotRune.getValue(),
                GlobalVariables.Firestore_Document_Rune_localisationClue, gotRune.getLocalisationClue(),
                GlobalVariables.Firestore_Document_Rune_score, gotRune.getScore());
    }

    private boolean runeChanged(Rune newRune, Rune rune) {
        return !newRune.getName().equals(rune.getName())
                || !newRune.getValue().equals(rune.getValue())
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

    private void tagScanned(Rune rune) {

        if (!rune.getValue().isEmpty()) {
            noTagLinkedText.setVisibility(View.INVISIBLE);
            noTagLinkedIcon.setVisibility(View.INVISIBLE);
            tagLinkedText.setVisibility(View.VISIBLE);
            tagLinkedIcon.setVisibility(View.VISIBLE);
        } else {
            tagLinkedText.setVisibility(View.INVISIBLE);
            tagLinkedIcon.setVisibility(View.INVISIBLE);
            noTagLinkedText.setVisibility(View.VISIBLE);
            noTagLinkedIcon.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Parses the user's inputs in the text fields of the code rune creation Android activity
     * and stores them into a rune object.
     * @return an instance that contains input data. Returns null if the rune name or code field are empty.
     */
    private Rune parseDataIntoRuneCode() {
        String name = runeNameEditText.getText().toString().trim();
        String loc = runeClueEditText.getText().toString().trim();
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

        Rune newRune = new Rune(GlobalVariables.Rune_nfc_object_type, ownerUID, name, runeTagID, loc,score);
        if (gotRune != null) {
            String oldRuneID = gotRune.getRuneID();
            newRune.setRuneID(oldRuneID);
        }
        return newRune;
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
                            gotRune = document.toObject(Rune.class);

                            runeNameEditText.setText(gotRune.getName(), TextView.BufferType.EDITABLE);
                            runeClueEditText.setText(gotRune.getLocalisationClue(), TextView.BufferType.EDITABLE);
                            scoreText.setText(gotRune.getScore(),TextView.BufferType.EDITABLE);
                            confirmButton.setText(R.string.saveRune);
                            runeTagID = gotRune.getValue();
                            tagScanned(gotRune);
                        } else {
                            Log.d("FirestoreDatabase", "No document");
                        }
                    } else {
                        Log.d("FirestoreDatabase", "get request failed with ", task.getException());
                    }
                }
            });
        }

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
        if(mNfcAdapter!= null)
            mNfcAdapter.disableReaderMode(this);
    }

    public void onTagDiscovered(Tag tag) {
        scannedTag = Ndef.get(tag);

        if (scannedTag != null) {

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            }

            String tagID = bytesToHexString(scannedTag.getTag().getId());

            runOnUiThread(() -> {
                checkPlayerRunesForTag(tagID);
            });

        }
    }

    private void checkPlayerRunesForTag(String tagID) {

        noTagLinkedIcon.setVisibility(View.INVISIBLE);
        tagLinkedIcon.setVisibility(View.INVISIBLE);
        tagLoading.setVisibility(View.VISIBLE);

        if (playerRunesList == null) {
            playerRunesList = new ArrayList<>();
            CollectionReference playerRunesRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes);
            Query playerRunesQuery = playerRunesRef.whereEqualTo(GlobalVariables.Firestore_User_ownerUID, ownerUID)
                    .whereEqualTo(GlobalVariables.Rune_type, GlobalVariables.Rune_nfc_object_type);
            playerRunesQuery.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Rune currRune = document.toObject(Rune.class);
                        playerRunesList.add(currRune);
                    }

                    isTagAlreadyLinked(tagID);
                }
            });
        } else {
            isTagAlreadyLinked(tagID);
        }

    }

    private void isTagAlreadyLinked(String tagID) {
        if (!playerRunesList.isEmpty()) {
            for (Rune rune : playerRunesList) {
                if ( rune.getValue().equals(tagID) && (gotRune == null || !rune.getRuneID().equals(gotRune.getRuneID()))) {
                    Toast.makeText(context, String.format(getString(R.string.tag_alredy_linked),rune.getName()), Toast.LENGTH_LONG).show();
                    tagLoading.setVisibility(View.INVISIBLE);
                    noTagLinkedIcon.setVisibility(View.VISIBLE);
                    tagLinkedText.setVisibility(View.INVISIBLE);
                    noTagLinkedText.setVisibility(View.VISIBLE);
                    runeTagID = "";
                    return;
                }
            }
        }
        runeTagID = tagID;
        tagLoading.setVisibility(View.INVISIBLE);
        tagLinkedIcon.setVisibility(View.VISIBLE);
        tagLinkedText.setText(getString(R.string.new_tag_linked));
        noTagLinkedText.setVisibility(View.INVISIBLE);
        tagLinkedText.setVisibility(View.VISIBLE);
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
                int itemID = item.getItemId();

                if (itemID == R.id.item_1) {
                    swap_add_rune_type(itemID, AddCodeRuneActivity.class);
                    return true;
                } else if (itemID == R.id.item_2) {
                    return true;
                } else {
                    swap_add_rune_type(itemID, AddQRRuneActivity.class);
                    return true;
                }

            }
        });
    }

    // Transform nfc id from bytes to string
    private String bytesToHexString(byte[] bytesTagID) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (bytesTagID == null || bytesTagID.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (byte b : bytesTagID) {
            buffer[0] = Character.forDigit((b >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(b & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

    private void swap_add_rune_type(Integer itemID, Class runeTypeClass) {
        Intent intent = new Intent(getApplicationContext(), runeTypeClass);
        if (fromActivity == GlobalVariables.EXV_FROM_GAME) {
            intent.putExtra(GlobalVariables.EXN_FROM, GlobalVariables.EXV_FROM_GAME);
        }

        // Used to keep name, code and hint through add Runes activities
        intent.putExtra(GlobalVariables.Rune_name, runeNameEditText.getText().toString().trim());
        intent.putExtra(GlobalVariables.Rune_code, intentRuneCode);
        intent.putExtra(GlobalVariables.Rune_hint, runeClueEditText.getText().toString().trim());
        intent.putExtra(GlobalVariables.Rune_score, scoreText.getText().toString().trim());

        intent.putExtra(GlobalVariables.EXN_Owner_ID, ownerUID);
        intent.putExtra(GlobalVariables.EXN_rune_nav_ID, itemID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }
}