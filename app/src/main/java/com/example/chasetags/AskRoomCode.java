package com.example.chasetags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AskRoomCode extends AppCompatActivity {

    // Miscellaneous

    String playerName = "";
    String roomCode = "";
    SharedPreferences preferences;

    // Realtime DB Stuff
    FirebaseDatabase db;

    // UI Stuff
    Toolbar topAppBar;
    TextView codeInput;
    TextView nicknameInput;
    TextInputLayout nicknameInputLayout;
    TextInputLayout roomCodeInputLayout;
    Button joinButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_room_code);

        initGlobalVariables();

        // Forces uppercase and length size for code input
        codeInput.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(5)});


        setupButtonListeners();
        setupToolbarNavigationListener();
    }

    private void initGlobalVariables() {
        // UI Stuff
        topAppBar = (Toolbar) findViewById(R.id.topAppBar);
        nicknameInputLayout = findViewById(R.id.nickname_input_layout);
        roomCodeInputLayout = findViewById(R.id.room_code_input_layout);
        codeInput = findViewById(R.id.room_code);
        nicknameInput = findViewById(R.id.nickname);
        joinButton = findViewById(R.id.join_button);

        // Realtime DB Stuff
        db = FirebaseDatabase.getInstance();

        // Get the playerName and store it
        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name, 0);
        playerName = preferences.getString(GlobalVariables.PREFS_player_name, "");
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    protected void onDestroy() {
        super.onDestroy();

        DatabaseReference playerRef =
                db.getReference(GlobalVariables.RealtimeDB_players).child(playerName);
        playerRef.removeValue();
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(view -> finish());
    }

    private void setupButtonListeners() {

        joinButton.setOnClickListener(view -> {
            joinButton.setText(getString(R.string.party_loading));
            joinButton.setEnabled(false);
            playerName = nicknameInput.getText().toString();
            roomCode = codeInput.getText().toString();

            // Checks if the name is valid and free
            // If so, adds it to the preferences and checks if the room is valid and exists
            // If so, switches to the activity Multiplayer Room
            checkIfRoomExistsAndExecuteRunnable(
                    () -> checkPlayerNameAndExecuteRunnable(
                            this::switchToMultiplayerRoom
                    )
            );
        });
    }

    private boolean isPlayerNameValid(String s) {
        // Rejects empty names
        if (s.length() == 0) return false;
        // Only accepts alphanumerical (i.e. word) characters (rejects others)
        return s.matches("\\w*");
    }

    private void editPlayerNamePreferences() {
        // Add the playerName to sharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(GlobalVariables.PREFS_player_name, playerName);
        editor.apply();
    }

    private void resetUIAndDisplayMessage(TextInputLayout inputLayout, String errorMessage) {
        nicknameInputLayout.setErrorEnabled(false);
        nicknameInputLayout.setError(null);
        roomCodeInputLayout.setErrorEnabled(false);
        roomCodeInputLayout.setError(null);

        if (inputLayout != null) {
            inputLayout.setErrorEnabled(true);
            inputLayout.setError(errorMessage);
        }

        joinButton.setText(getString(R.string.join_party));
        joinButton.setEnabled(true);
    }

    private void checkPlayerNameAndExecuteRunnable(Runnable exec) {

        // Gets the playerName from the input and checks its validity
        playerName = nicknameInput.getText().toString();
        if (!isPlayerNameValid(playerName)) {
            // Alerts the user (via UI) of the name error
            resetUIAndDisplayMessage(nicknameInputLayout, getString(R.string.invalid_nickname));
            return;
        }

        // The input name is valid

        /* Checks if the name already exists in the real time DB
        (i.e. if a player online also is already using the same name)
        If the name is free, then stores it into the real time DB and the app's preferences,
        then executes the given Runnable instance
         */

        db.getReference(GlobalVariables.RealtimeDB_players).
                child(playerName).
                get().
                addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        // The retrieving of data from the real time database failed
                        Log.e("firebase", "Error checking if player exists", task.getException());
                    } else if (task.getResult().getValue() != null) {
                        // The input name is already used by another player
                        // Since it can't be used, alerts the user of the error
                        resetUIAndDisplayMessage(nicknameInputLayout, getString(R.string.nickname_taken));
                    } else {
                        // The input name is valid and free
                        // 1) Resets the text field's previous errors (if applicable)
                        resetUIAndDisplayMessage(null, null);

                        // 2) Adds the player's name to the real time DB to prevent another player from using it
                        db.getReference(GlobalVariables.RealtimeDB_players)
                                .child(playerName)
                                .setValue(GlobalVariables.RealtimeDB_player_guest);

                        // 3) Adds the name to the app's preferences for easier communication between activities
                        editPlayerNamePreferences();

                        // 4) Executes the desired Runnable instance
                        exec.run();
                    }
                });
    }

    private void checkIfRoomExistsAndExecuteRunnable(Runnable exec) {
        codeInput.setText("");
        if(roomCode.equals("")) {
            resetUIAndDisplayMessage(roomCodeInputLayout, getString(R.string.enter_room_code));
            return;
        }

        // Room code syntax is valid => Check if room exists
        db.getReference(GlobalVariables.RealtimeDB_rooms).
                child(roomCode).
                get().
                addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                if (task.getResult().getValue() != null) {
                    // if game is already started, don't let the player join
                    if (task.getResult().child(GlobalVariables.RealtimeDB_gameStarted).getValue().equals(true)) {
                        resetUIAndDisplayMessage(
                                roomCodeInputLayout,
                                getString(R.string.game_already_started)
                        );
                    } else {
                        // If room exists :
                        resetUIAndDisplayMessage(null, null);
                        exec.run();
                    }
                } else {
                    // If room doesn't exist show error in inputLayout
                    resetUIAndDisplayMessage(
                            roomCodeInputLayout,
                            getString(R.string.room_doesnt_exist)
                    );
                }
            } else {
                Log.e("firebase", "Error checking if room code exists", task.getException());
            }
        });
    }

    private void switchToMultiplayerRoom() {
        Intent intent = new Intent(getApplicationContext(), MultiplayerRoom.class);
        intent.putExtra(GlobalVariables.EXN_Room_code, roomCode);
        startActivity(intent);
    }
}