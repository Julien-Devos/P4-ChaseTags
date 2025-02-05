package com.example.chasetags;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chasetags.dialogs.GameSettingsDialogFragment;
import com.example.chasetags.adapters.RuneAdapter;
import com.example.chasetags.addRunes.AddCodeRuneActivity;
import com.example.chasetags.addRunes.AddNFCRuneActivity;
import com.example.chasetags.addRunes.AddQRRuneActivity;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.CleanRealtimeDB;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SessionMenuActivity extends AppCompatActivity {

    // Miscellaneous
    String userUID = "";
    String ownerUID = "";
    Context context;

    // UI stuff
    FloatingActionButton buttonOpenFabMenu;
    ExtendedFloatingActionButton buttonAddRune;
    ExtendedFloatingActionButton buttonSelectGlobalRunes;
    Button buttonLaunchGame;
    ListView runesLV;
    Toolbar topAppBar;
    TextView noGameRunes;

    // Session/game options
    boolean isFabManuOpened = false;
    @SuppressLint("StaticFieldLeak")
    public static TextView timeChosen;
    public static Long gameTimerSelection;
    public static HashMap<String, Integer> gameSettings; // hash map of the game settings


    // list of the game Runes
    public static ArrayList<Rune> gameRunes = new ArrayList<>();  // list of the game Runes

    // Real time Database
    FirebaseDatabase realTimeDB;
    FirebaseFirestore firebaseDB;


    // TODO : POLISH the code of this class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_menu);

        realTimeDB = FirebaseDatabase.getInstance();
        firebaseDB = FirebaseFirestore.getInstance();
        context = this.getBaseContext();

        topAppBar = (Toolbar) findViewById(R.id.toolbar2);
        timeChosen = findViewById(R.id.time_choosed);
        noGameRunes = findViewById(R.id.no_game_runes);

        userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getOwnerID();
        setDefaultGameSettings();
        setupToolbarNavigationListener();
        setupButtonListener();
    }

    private void getOwnerID() {
        DocumentReference playerRef = firebaseDB.collection(GlobalVariables.Firestore_Collection_users).document(userUID);
        playerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    ownerUID = document.get(GlobalVariables.Firestore_User_ownerUID).toString();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupRunesListView();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    private void setupButtonListener() {

        buttonLaunchGame = findViewById(R.id.button_launch_game_from_menu);
        buttonLaunchGame.setOnClickListener(v -> {
            buttonLaunchGame.setEnabled(false);
            buttonLaunchGame.setText(R.string.creating_room);

            if (userUID.equals("")) {
                // If we didn't get the playerName
                Toast.makeText(context, "Error!", Toast.LENGTH_SHORT).show();
                finish();
            } else {

                // Check if the host added Runes before starting the game
                if (!gameRunes.isEmpty()) {
                    // Add player to the players section in the db
                    DatabaseReference playerRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_players).child(userUID);
                    playerRef.setValue(GlobalVariables.RealtimeDB_player_host);

                    // Generate random room code by generating 5 random letters (in ASCII : between 65-A and 90-Z)
                    Random random = new Random();
                    String roomCode = random.ints(65, 91).limit(5).collect(StringBuilder::new,
                            StringBuilder::appendCodePoint, StringBuilder::append).toString();

                    Intent intent = new Intent(context, MultiplayerRoom.class);
                    intent.putExtra(GlobalVariables.RealtimeDB_timerSelection, gameTimerSelection);
                    intent.putExtra(GlobalVariables.EXN_Room_code, roomCode);
                    startActivity(intent);
                    /* Important : do not finish this activity, to allow the host to come back
                    and quickly modify its runes before creating a new lobby */
                }
            }
            buttonLaunchGame.setText(R.string.start_game_button);
            buttonLaunchGame.setEnabled(true);
        });

        buttonAddRune = findViewById(R.id.add_rune);
        buttonAddRune.setOnClickListener(view -> {
            showAddRunesMenu();
            switchToAddRuneActivity(null, null);    // ID = -1 because a new rune is created
        });

        buttonSelectGlobalRunes = findViewById(R.id.choose_rune);
        buttonSelectGlobalRunes.setOnClickListener(view -> {
            showAddRunesMenu();
            startActivity(new Intent(context , SelectProfileRunes.class));
        });

        buttonOpenFabMenu = findViewById(R.id.open_fab_menu);
        buttonOpenFabMenu.setOnClickListener(view -> showAddRunesMenu());

    }

    private void showAddRunesMenu() {

        if (!isFabManuOpened) {
            buttonSelectGlobalRunes.setVisibility(View.VISIBLE);
            buttonAddRune.setVisibility(View.VISIBLE);
            isFabManuOpened = true;

        } else {
            buttonSelectGlobalRunes.setVisibility(View.INVISIBLE);
            buttonAddRune.setVisibility(View.INVISIBLE);
            isFabManuOpened = false;
        }

    }

    private void setupRunesListView() {
        runesLV = findViewById(R.id.runes_list);

        ArrayList<Rune> clickableRunesList = new ArrayList<>();
        RuneAdapter adapter = new RuneAdapter(clickableRunesList,SessionMenuActivity.this);

        runesLV.setAdapter(adapter);
        runesLV.setOnItemClickListener((parent, view, position, id) -> {
            Rune rune = clickableRunesList.get(position);
            switchToAddRuneActivity(rune, rune.getType());
        });

        // Filling clickableRunesList with the contents of localRunes and importedRunes
        if (gameRunes != null){
            clickableRunesList.addAll(gameRunes);
        }

        clickableRunesList.sort(Rune::compareRunes);

        if (clickableRunesList.isEmpty()) {
            noGameRunes.setVisibility(View.VISIBLE);
            buttonLaunchGame.setEnabled(false);
        } else {
            noGameRunes.setVisibility(View.GONE);
            buttonLaunchGame.setEnabled(true);
        }
    }

    private void switchToAddRuneActivity(Rune rune, String runeType) {
        Intent intent;
        if (runeType == null || runeType.equals(GlobalVariables.Rune_code_object_type))
            intent = new Intent(this, AddCodeRuneActivity.class);
        else if (runeType.equals(GlobalVariables.Rune_nfc_object_type))
            intent = new Intent(this, AddNFCRuneActivity.class);
        else
            intent = new Intent(this, AddQRRuneActivity.class);

        if (rune != null) {
            intent.putExtra(GlobalVariables.EXN_Rune_ID, rune.getRuneID());
        }

        // Used to keep name, code and hint through add Runes activities
        intent.putExtra(GlobalVariables.Rune_name, "");
        intent.putExtra(GlobalVariables.Rune_code, "");
        intent.putExtra(GlobalVariables.Rune_hint, "");
        intent.putExtra(GlobalVariables.Rune_score, "10");

        intent.putExtra(GlobalVariables.EXN_Owner_ID, ownerUID);
        intent.putExtra(GlobalVariables.EXN_FROM, GlobalVariables.EXV_FROM_GAME);
        startActivity(intent);
    }

    protected void onDestroy() {
        super.onDestroy();

        // Remove player from realtimeDB if it goes back to main activity
        DatabaseReference playerRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_players + "/" + userUID);
        playerRef.removeValue();
    }

    private void setDefaultGameSettings() {
        // Set gameSettings hashmap to default values
        gameSettings = new HashMap<>();
        gameSettings.put(GlobalVariables.gameSettings_timer_status, GlobalVariables.gameSettings_timer_disabled);
        gameSettings.put(GlobalVariables.gameSettings_timer_hours, 0);
        gameSettings.put(GlobalVariables.gameSettings_timer_minutes, 10);
        gameSettings.put(GlobalVariables.gameSettings_ending_choice, GlobalVariables.gameSettings_ending_allPlayers);
        gameSettings.put(GlobalVariables.gameSettings_hints, GlobalVariables.gameSettings_hints_rune_chase);
    }

    private void setupToolbarNavigationListener(){
                topAppBar.setNavigationOnClickListener(view -> finish());

                topAppBar.setOnMenuItemClickListener(item -> {
                    int itemID = item.getItemId();

                    if (itemID == R.id.settings) {
                        GameSettingsDialogFragment.display(getSupportFragmentManager());
                        return true;
                    }

                    if(itemID == R.id.print) {
                        Intent intent = new Intent(getApplicationContext(), PrintQRRunes.class);
                        intent.putExtra("Session", 1);
                        startActivity(intent);
                        return true;
                    }

                    return false;
        });
    }

    public static HashMap<String, Rune> gameRunesToHashMap(){
        HashMap<String, Rune> gameRunesHashMap = new HashMap<>();
        for (Rune rune : gameRunes) {
            gameRunesHashMap.put(rune.getRuneID(), rune);
        }
        return gameRunesHashMap;
    }
}