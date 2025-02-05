package com.example.chasetags;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.adapters.PlayerAdapter;
import com.example.chasetags.player.Player;
import com.example.chasetags.utils.CustomCountDownTimer;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.example.chasetags.utils.RealTimeDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HostGameMenu extends AppCompatActivity {

    // Miscellaneous
    Context context;
    int runesTotal;

    // UI Stuff
    Button endGameButton;
    ArrayList<Player> guestsList;
    ListView playersLV;
    // Timer
    Long timerSelection;
    CustomCountDownTimer myTimer;
    TextView timerTextview;

    // Real Time Database
    String roomCode;
    DatabaseReference roomRef;
    ValueEventListener eventListener;


    long startTime = 0;

    // Timer when theres no limited time for the game
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextview.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_game_menu);

        // Global variables init
        initGlobalVariables();

        // List adapter
        // TODO : see if those next 2 lines are useful, because they are redundant with addRoomEventListener();
        PlayerAdapter adapter = new PlayerAdapter(guestsList, HostGameMenu.this, runesTotal, getBaseContext());
        playersLV.setAdapter(adapter);

        // Listeners
        setupButtonsListeners();
        setupRoomEventListener();
        setupTimer();
    }

    private void initGlobalVariables() {
        context = getBaseContext();
        endGameButton = findViewById(R.id.end_game_button);
        guestsList = new ArrayList<>();
        playersLV = findViewById(R.id.players_list);
        runesTotal = getIntent().getIntExtra(GlobalVariables.EXN_Runes_total, 0);
        timerTextview = findViewById(R.id.timerTextView);
        timerSelection = getIntent().
                getLongExtra(GlobalVariables.RealtimeDB_timerSelection, -1);
        roomCode = getIntent().getStringExtra(GlobalVariables.EXN_Room_code);
        roomRef = FirebaseDatabase.getInstance()
                .getReference(GlobalVariables.RealtimeDB_rooms)
                .child(roomCode);
    }

    private void setupTimer() {
        if (timerSelection!=-1){
            if (myTimer != null) myTimer.cancel();
            // Creates a timer that automatically ends the game when it runs out
            myTimer = new CustomCountDownTimer(
                    timerSelection,
                    timerTextview,
                    () -> endGameNormally(R.string.end_game_timer)
            );
            myTimer.start();
        } else {
            // Start a timer
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }


    private void setupButtonsListeners(){
        endGameButton.setOnClickListener(view -> showLeaveDialog());
    }

    private void setupRoomEventListener(){
        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ignore empty snapshots : they should not happen (because the guests can't
                // shouldn't be able to delete the room
                if (!snapshot.exists()) return;

                // Update the list of guests, to take notice of runes updates OR players quitting
                RealTimeDBHelper.refillListWithPlayers(guestsList, snapshot);

                // Automatically ends game if there is no guest remaining
                if (guestsList.size() == 0) {
                    endGameBecauseNoMoreGuests();
                    return;
                }

                int autoEndSettings = SessionMenuActivity.gameSettings.get(GlobalVariables.gameSettings_ending_choice);
                if (autoEndSettings != GlobalVariables.gameSettings_ending_timerEnd) {
                    // If the auto game end can be triggered by the players finding all their runes

                    // Counting the number of players that have finished
                    int playersDone = 0;
                    for (Player player : guestsList) {
                        if (player.hasFoundAllRunes()) playersDone++;
                    }

                    // Retrieving the number of players to wait for before ending the game
                    int playersThreshold;
                    switch (autoEndSettings) {
                        case GlobalVariables.gameSettings_ending_firstPlayer:
                            playersThreshold = 1;
                            break;
                        case GlobalVariables.gameSettings_ending_3firstPlayers:
                            playersThreshold = 3;
                            break;
                        default:    // Includes "wait for all players" setting
                            playersThreshold = guestsList.size();
                    }

                    // Ending the game if all players have found their runes (disregarding the threshold)
                    if (playersDone == guestsList.size()) endGameNormally(R.string.end_game_all_players_done);
                    // Or ending the game if the threshold is passed
                    else if (playersDone >= playersThreshold) endGameNormally(R.string.end_game_threshold_passed);

                    // Else : nothing happens -> the game keeps going

                }

                // Updates the ListView (scoreboard) with all the guests
                PlayerAdapter adapter =
                        new PlayerAdapter(guestsList, HostGameMenu.this, runesTotal, getBaseContext());
                playersLV.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HostGameMenu.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.addValueEventListener(eventListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    @Override
    public void onBackPressed() {
        // To make the dialog pop up when the host tries to press "back"
        showLeaveDialog();
    }

    private void showLeaveDialog(){
        // Shows a dialog to alert the host they are about to end the game for everyone

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.leave_game_dialog_title))
                .setMessage(getString(R.string.leave_game_dialog_message_host))
                .setPositiveButton(getString(
                        R.string.leave_game_dialog_positive),
                        (dialogInterface, i) -> endGameNormally(R.string.end_game_host_decision))
                .setNegativeButton(getString(R.string.leave_game_dialog_negative), null)
                .show();
    }

    private void endGameBecauseNoMoreGuests() {
        Toast.makeText(getBaseContext(),
           R.string.end_game_toast_no_players, Toast.LENGTH_LONG)
            .show();
        if (myTimer != null) myTimer.cancel();
        roomRef.removeEventListener(eventListener);
        MainActivity.finishAndGoBackToTitleScreen(this);
    }

    private void endGameNormally(int reasonID) {
        // Stops the timer to avoid triggered actions after the game has ended
        // myTimer is null when the stopwatch mode is active
        if (myTimer != null) myTimer.cancel();

        // Tells all the guest device (through the db) why the game has been stopped
        roomRef.child(GlobalVariables.RealtimeDB_endGameReason)
                .setValue(reasonID);

        // To notify the guests that the game has stopped
        roomRef.child(GlobalVariables.RealtimeDB_gameStarted).setValue(false);
        roomRef.removeEventListener(eventListener);
        // To go to the scoreboard activity
        EndGameScoreboard.finishAndJoinActivity(this, roomCode, runesTotal, reasonID);

        /* The activity is not finished yet, because we still need to keep MultiPlayerRoom.java
         active, because triggering its onDestroy() would delete the Real Time DB before
        we have the chance to retrieve the data for the end-of-game scoreboard */
    }
}