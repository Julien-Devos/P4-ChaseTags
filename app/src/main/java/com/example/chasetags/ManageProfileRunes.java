package com.example.chasetags;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.chasetags.adapters.RuneAdapter;
import com.example.chasetags.addRunes.AddCodeRuneActivity;
import com.example.chasetags.addRunes.AddNFCRuneActivity;
import com.example.chasetags.addRunes.AddQRRuneActivity;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.CleanRealtimeDB;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ManageProfileRunes extends AppCompatActivity {
    public static ArrayList<Rune> playerRunesList;
    ListView myRunesList;
    ProgressBar runesLoading;
    TextView noGlobalRunes;
    FloatingActionButton addRuneButton;
    Toolbar topAppBar;

    FirebaseFirestore dbFirestore;
    FirebaseDatabase db;

    String userUID = "";
    String ownerUID = "";
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_profile_runes);

        dbFirestore = FirebaseFirestore.getInstance();
        db = FirebaseDatabase.getInstance();

        runesLoading = findViewById(R.id.runes_loading);
        noGlobalRunes = findViewById(R.id.no_profile_runes);
        noGlobalRunes.setVisibility(View.INVISIBLE);

        topAppBar = (Toolbar) findViewById(R.id.topAppBar);

        user = FirebaseAuth.getInstance().getCurrentUser();
        userUID = user.getUid();

        setupRunesListView();
        setupButtonListener();
        setupToolbarNavigationListener();
    }

    private void setupButtonListener() {
        addRuneButton = findViewById(R.id.add_rune_menu_button);
        addRuneButton.setOnClickListener(view -> addOrModifyRune(null, GlobalVariables.Rune_code_object_type));
    }

    private void setupRunesListView() {
        myRunesList = findViewById(R.id.my_runes_list);

        playerRunesList = new ArrayList<>();

        addPlayerRunesToList(playerRunesList);
        runesLoading.setVisibility(View.VISIBLE);
        noGlobalRunes.setVisibility(View.INVISIBLE);
    }

    private void addPlayerRunesToList(ArrayList<Rune> playerRunesList) {

        DocumentReference playerRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_users).document(userUID);
        playerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    ownerUID = document.get(GlobalVariables.Firestore_User_ownerUID).toString();

                    CollectionReference playerRunesRef = dbFirestore.collection(GlobalVariables.Firestore_Collection_runes);
                    Query playerRunesQuery = playerRunesRef.whereEqualTo(GlobalVariables.Firestore_User_ownerUID, ownerUID);
                    playerRunesQuery.get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {

                            for (QueryDocumentSnapshot document1 : task1.getResult()){
                                Rune currRune = document1.toObject(Rune.class);
                                if (currRune.getOwnerUID().equals(ownerUID)){
                                    playerRunesList.add(currRune);
                                }
                            }

                            runesLoading.setVisibility(View.INVISIBLE);

                            if (playerRunesList.isEmpty()) {
                                noGlobalRunes.setVisibility(View.VISIBLE);
                            }

                            setListOnClickListener(playerRunesList);
                        }
                    });

                } else {
                    // Stop loading and add text to say there's no global rune
                    runesLoading.setVisibility(View.INVISIBLE);
                    noGlobalRunes.setVisibility(View.VISIBLE);

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
        });

    }

    protected void onResume() {
        super.onResume();
        setupRunesListView();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    private void setListOnClickListener(ArrayList<Rune> playerRunesList) {
        playerRunesList.sort(Rune::compareRunes);
        myRunesList.setAdapter(new RuneAdapter(playerRunesList, ManageProfileRunes.this));

        myRunesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                addOrModifyRune(playerRunesList.get(position), playerRunesList.get(position).getType());

            }
        });
    }


    private void addOrModifyRune(Rune rune, String runeType) {
        Intent intent = null;

        if (rune != null || !runeType.equals("")) {
            if (runeType == null) { runeType = rune.getType(); }

            if (runeType.equals(GlobalVariables.Rune_code_object_type)) {
                intent = new Intent(this, AddCodeRuneActivity.class);
            }
            else if (runeType.equals(GlobalVariables.Rune_nfc_object_type)) {
                intent = new Intent(this, AddNFCRuneActivity.class);
            } else {
                intent = new Intent(this, AddQRRuneActivity.class);
            }

            if (rune != null) {
                intent.putExtra(GlobalVariables.EXN_Rune_ID, rune.getRuneID());
            }

            // Used to keep name, code and hint through add Runes activities
            intent.putExtra(GlobalVariables.Rune_name, "");
            intent.putExtra(GlobalVariables.Rune_code, "");
            intent.putExtra(GlobalVariables.Rune_hint, "");
            intent.putExtra(GlobalVariables.Rune_score, "10");

            intent.putExtra(GlobalVariables.EXN_Owner_ID, ownerUID);
            startActivity(intent);
        }
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        topAppBar.setOnMenuItemClickListener(item -> {
            int itemID = item.getItemId();

            if(itemID == R.id.print) {
                Intent intent = new Intent(getApplicationContext(), PrintQRRunes.class);
                intent.putExtra("Session", 0);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    protected void onDestroy() {
        super.onDestroy();

        // Remove player from realtimeDB if it goes back to main activity
        DatabaseReference playerRef = db.getReference(GlobalVariables.RealtimeDB_players + "/" + userUID);
        playerRef.removeValue();
    }
}
