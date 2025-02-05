package com.example.chasetags;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.chasetags.adapters.SelectRuneAdapter;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectProfileRunes extends AppCompatActivity {

    private ArrayList<Rune> runesList;
    private ListView listView;
    private SelectRuneAdapter adapter;

    ProgressBar runesLoading;
    TextView noProfileRunes;
    Toolbar topAppBar;
    Button saveButton;
    Button goToProfileRunes;

    FirebaseFirestore dbFirestore;

    String userUID = "";
    String ownerUID = "";
    FirebaseUser user;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_profile_runes);

        dbFirestore = FirebaseFirestore.getInstance();

        runesLoading = findViewById(R.id.runes_loading);
        saveButton = findViewById(R.id.save_button);
        noProfileRunes = findViewById(R.id.no_profile_runes);
        goToProfileRunes = findViewById(R.id.go_to_profile_runes);
        goToProfileRunes.bringToFront();

        topAppBar = (Toolbar) findViewById(R.id.topAppBar);

        listView = (ListView) findViewById(R.id.list);
        runesList = new ArrayList<>();
        runesList.addAll(SessionMenuActivity.gameRunes);

        user = FirebaseAuth.getInstance().getCurrentUser();
        userUID = user.getUid();

        adapter = new SelectRuneAdapter(runesList, SelectProfileRunes.this);

        addGlobalRunesToList();
        setupButtonListeners();
        setupToolbarNavigationListener();
    }

    private void addGlobalRunesToList(){
        noProfileRunes.setVisibility(View.INVISIBLE);
        goToProfileRunes.setVisibility(View.INVISIBLE);
        runesLoading.setVisibility(View.VISIBLE);

        DocumentReference playerRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_users).document(userUID);
        playerRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        ownerUID = document.get(GlobalVariables.Firestore_User_ownerUID).toString();

                        CollectionReference playerRunesRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes);
                        Query playerRunesQuery = playerRunesRef.whereEqualTo(GlobalVariables.Firestore_User_ownerUID, ownerUID);
                        playerRunesQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {

                                    for (QueryDocumentSnapshot document : task.getResult()){
                                        Rune currRune = document.toObject(Rune.class);
                                        if (currRune.getOwnerUID().equals(ownerUID)){

                                            boolean runeInList = false;
                                            for (Rune rune : runesList) {
                                                if (rune.getRuneID().equals(currRune.getRuneID())){
                                                    runeInList = true;
                                                    break;
                                                }
                                            }
                                            if (!runeInList){
                                                runesList.add(currRune);
                                            }
                                        }
                                    }

                                    runesList.sort(Rune::compareRunes);

                                    if (runesList.isEmpty()) {
                                        saveButton.setVisibility(View.INVISIBLE);
                                        noProfileRunes.setVisibility(View.VISIBLE);
                                        goToProfileRunes.setVisibility(View.VISIBLE);
                                    }

                                    runesLoading.setVisibility(View.INVISIBLE);
                                    setupListAdapter();
                                }
                            }
                        });
                    } else {
                        // Stop loading and add text to say there's no global rune
                        runesLoading.setVisibility(View.INVISIBLE);
                        saveButton.setVisibility(View.INVISIBLE);
                        noProfileRunes.setVisibility(View.VISIBLE);
                        goToProfileRunes.setVisibility(View.VISIBLE);

                        // Generate random user ownerID
                        Map<String, Object> playerData = new HashMap<>();
                        ownerUID = userUID;
                        playerData.put(GlobalVariables.Firestore_User_ownerUID, ownerUID);
                        dbFirestore.collection(GlobalVariables.Firestore_Collection_users).document(userUID).set(playerData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d("FirestoreDatabase", "Player successfully added!");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("FirestoreDatabase", "Error while adding player", e);
                                    }
                                });
                    }
                } else {
                    Log.d("FirestoreDatabase", "get request failed with ", task.getException());
                }
            }
        });

    }

    private void setupListAdapter(){
        listView.setAdapter(adapter);
    }

    private void setupButtonListeners(){

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SessionMenuActivity.gameRunes.clear();

                for (Rune rune : runesList){
                    if (rune.isChecked()){
                        SessionMenuActivity.gameRunes.add(rune);
                    }
                }
                finish();
            }
        });

        goToProfileRunes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ManageProfileRunes.class));
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        addGlobalRunesToList();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}