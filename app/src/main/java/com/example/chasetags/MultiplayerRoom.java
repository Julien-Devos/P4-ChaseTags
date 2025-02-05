package com.example.chasetags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chasetags.player.Player;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.CleanRealtimeDB;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.example.chasetags.utils.RealTimeDBHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MultiplayerRoom extends AppCompatActivity{

    // Miscellaneous
    ArrayList<String> playerList;
    SharedPreferences preferences;
    public static String playerName;
    String roomCode;
    String playerRole = "";
    Long gameTimerSelection;
    int gameMode;

    // UI Stuff
    TextView roomCodeText;
    TextView noPlayerText;
    ListView playerListView;
    Button startGameButton;
    Toolbar topAppBar;

    // Real time Database
    FirebaseDatabase realTimeDB;
    DatabaseReference playerRef;
    DatabaseReference roomRef;

    // Listeners
    ValueEventListener roomValueEventListener;
    ValueEventListener playerListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_room);

        // Global variables
        initGlobalVariables();

        // UI Stuff
        startGameButton.setVisibility(View.INVISIBLE);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playerList);
        playerListView.setAdapter(adapter);
        roomCodeText.setText(String.format(getString(R.string.party_code),roomCode));

        // Listeners
        addEventListener();
        addRoomEventListener();
        setupButtonsListener();
        setupToolbarNavigationListener();
    }

    private void getGameModeIfGuest() {
        roomRef.child(GlobalVariables.gameSettings_mode)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        gameMode = dataSnapshot.getValue(Integer.class);
                    }
                });
    }

    private void initGlobalVariables() {
        playerList = new ArrayList<>();
        roomCodeText = findViewById(R.id.roomCode);
        playerListView = findViewById(R.id.player_list);
        startGameButton = findViewById(R.id.start_game);
        topAppBar = findViewById(R.id.topAppBar);
        noPlayerText = findViewById(R.id.no_player);
        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name, 0);
        playerName = preferences.getString(GlobalVariables.PREFS_player_name, "");
        roomCode = getIntent().getStringExtra(GlobalVariables.EXN_Room_code);
        gameTimerSelection =
                getIntent().getLongExtra(GlobalVariables.RealtimeDB_timerSelection,-1);
        realTimeDB = FirebaseDatabase.getInstance();
        playerRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_players).child(playerName);
        roomRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_rooms).child(roomCode);
    }

    private void addEventListener() {

        // TODO : POLISH the code of this method

        // To set (as host) or get (as guest) all the room data to/from the RealTime DB
        // when launching this activity
        playerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // TODO : REWRITE this method

                // So that the data is only retrieved/set once
                playerRef.removeEventListener(playerListener);

                if (!snapshot.exists()) return;

                // Get the role of the player and set it in the room
                playerRole = (String) snapshot.getValue();
                if (playerRole == null) return;

                // Adds a disconnect-safe DB clean protocol
                CleanRealtimeDB.setupDBClean(realTimeDB, roomCode, playerName, playerRole);

                if (playerRole.equals(GlobalVariables.RealtimeDB_player_host)) {
                    startGameButton.setVisibility(View.VISIBLE);

                    Player player = new Player(playerName, playerRole);
                    player.makeRunesLeftList();   // TODO : useless because host does not play
                    roomRef.child(playerName).setValue(player.toHashMap());

                    // Adds attribute "gameStarted" to the room and sets it to false
                    roomRef.child(GlobalVariables.RealtimeDB_gameStarted)
                            .setValue(false);

                    // Adds attribute "gameTimer" to the room and sets it the time got from intent
                    roomRef.child(GlobalVariables.RealtimeDB_timerSelection)
                            .setValue(gameTimerSelection);

                    // Adds attribute "gameSettings" to the room and sets it from SessionMenuActivity
                    roomRef.child(GlobalVariables.gameSettings_mode)
                           .setValue(SessionMenuActivity.gameSettings.get(GlobalVariables.gameSettings_hints));

                    // Add Runes of the room to the realtimeDB
                    roomRef.child(GlobalVariables.RealtimeDB_Room_Runes_list).setValue(SessionMenuActivity.gameRunesToHashMap());
                } else {

                    // Get the room RunesList from realtimeDB and store it into SessionMenuActivity.localRunes
                    DatabaseReference roomRuneListRef =
                            realTimeDB.getReference(GlobalVariables.RealtimeDB_rooms)
                            .child(roomCode)
                            .child(GlobalVariables.RealtimeDB_Room_Runes_list);

                    roomRuneListRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            if (task.getResult().getValue() != null) {

                                Object obj = task.getResult().getValue();

                                // Transform the Object got from the db into an Array of Runes
                                // and store it into SessionMenuActivity.localRunes
                                SessionMenuActivity.gameRunes = realtimeDBObjectToRuneArray(obj);
                                updatePlayerInDB();

                            }
                        }
                    });

                    getGameModeIfGuest();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MultiplayerRoom.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        };

        playerRef.addValueEventListener(playerListener);
    }

    private void updatePlayerInDB() {
        Player player = new Player(playerName, playerRole);
        player.makeRunesLeftList();
        roomRef.child(playerName).setValue(player.toHashMap());
    }

    // Get an Object roomRunesList that comes from the realtimeDB and transform it into an Array of Runes
    private ArrayList<Rune> realtimeDBObjectToRuneArray(Object obj) {

        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;

        ArrayList<Rune> runesList = new ArrayList<>();

        for (Object rune: objMap.values()) {
            HashMap<String, String> map = (HashMap<String, String>) rune;
            Rune newRune = new Rune(map.get("type"), "", map.get("name"), map.get("value"), map.get("localisationClue"),map.get("score"));

            newRune.setRuneID(map.get("runeID"));
            runesList.add(newRune);
        }

        return runesList;
    }

    private void addRoomEventListener() {
        roomValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() && Objects.equals(playerRole, GlobalVariables.RealtimeDB_player_host))
                    return;    // Invalid snapshot, we ignore it

                if (!snapshot.exists()) {
                    // Exit lobby if it is disbanded by host
                    genericExitLobby();
                    return;
                }

                // Lobby still exists : analyse where the change comes from

                // Make user go to game menu if game has started
                if (RealTimeDBHelper.hasGameStarted(snapshot)) {
                    // Start of the game:
                    roomRef.removeEventListener(roomValueEventListener);
                    getGameTimerAndStartGame();
                    return;
                }

                /* If it is neither the room deletion nor the start of the game,
                then it means a player joined/left the lobby ---> update the players list */
                ArrayList<Player> guestsInstances = new ArrayList<>();
                RealTimeDBHelper.refillListWithPlayers(guestsInstances, snapshot);
                // Because we need a ArrayList<String, not an ArrayList<Player>, we convert it :
                playerList = new ArrayList<>();
                for (Player pl : guestsInstances) playerList.add(pl.getName());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(MultiplayerRoom.this,
                        android.R.layout.simple_list_item_1, playerList);
                playerListView.setAdapter(adapter);

                if (playerList.isEmpty()) {
                    noPlayerText.setVisibility(View.VISIBLE);
                    startGameButton.setEnabled(false);
                } else {
                    noPlayerText.setVisibility(View.INVISIBLE);
                    startGameButton.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MultiplayerRoom.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.addValueEventListener(roomValueEventListener);
    }

    private void getGameTimerAndStartGame() {
        roomRef.child(GlobalVariables.RealtimeDB_timerSelection).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists())
                gameTimerSelection = Long.parseLong(task.getResult().getValue().toString());
            else gameTimerSelection = (long) -1;
            // Launch game from here to avoid null timer
            switchToAppropriateGameMenu();
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    private void setupButtonsListener() {
        startGameButton.setOnClickListener(view ->
                roomRef.child(GlobalVariables.RealtimeDB_gameStarted).setValue(true));
    }


    private void switchToAppropriateGameMenu() {
        Intent intent;
        // if player is host, go to HostGameMenu, else go to GuestGameMenu
        if (playerRole.equals(GlobalVariables.RealtimeDB_player_host))
            intent = new Intent(MultiplayerRoom.this.getBaseContext(), HostGameMenu.class);
        else  intent = new Intent(MultiplayerRoom.this.getBaseContext(), GuestGameMenu.class);
        intent.putExtra(GlobalVariables.EXN_Runes_total, SessionMenuActivity.gameRunes.toArray().length);
        intent.putExtra(GlobalVariables.EXN_Room_code, roomCode);
        intent.putExtra(GlobalVariables.RealtimeDB_timerSelection, gameTimerSelection);
        intent.putExtra(GlobalVariables.gameSettings_mode, gameMode);
        startActivity(intent);
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(view -> genericExitLobby());
    }

    @Override
    public void onBackPressed() { genericExitLobby(); }

    protected void onDestroy() {
        super.onDestroy();

        /*
         *
         * THIS is where all the DB data is deleted when a user quits while in the lobby,
         * in a game, or at the end of the game
         *
         */

        roomRef.removeEventListener(roomValueEventListener);
        CleanRealtimeDB.applyDBClean();
    }

    private void genericExitLobby() {
        // To avoid copy-pasting the 'if' statement everywhere in this class
        if (playerRole.equals(GlobalVariables.RealtimeDB_player_host)) {
            // For hosts.

            // TODO : make dialog pop-up to confirm choice to disband the lobby

            // TODO : Go back to previous activity and retrieves all the selected runes
            SessionMenuActivity.gameRunes.clear();    // TODO : allow the user to retrieve the runes
            finish();    // will trigger onDestroy and handle the DB
        }

        // For guests. The next line will trigger OnDestroy and handle the DB
        else MainActivity.finishAndGoBackToTitleScreen(this);
    }
}