package com.example.chasetags;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.askRunes.AskRuneCode;
import com.example.chasetags.player.Player;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.CleanRealtimeDB;
import com.example.chasetags.utils.CustomCountDownTimer;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.example.chasetags.utils.PointsAttribution;
import com.example.chasetags.utils.RealTimeDBHelper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class GuestGameMenu extends AppCompatActivity {

    // Miscellaneous
    SharedPreferences preferences;
    String playerName;
    Context context;
    int runesTotal;
    int gameMode;

    // UI stuff
    TextView runeCounterText;
    Button enterRuneButton;
    Button leaveGameButton;
    TextView hintTitleTextView;
    TextView hintTextView;

    // Timer
    CustomCountDownTimer myTimer;
    TextView timerTextview;
    Long timerSelection;
    long startTime = 0;

    // Hints
    public static List<Rune> runesToFind;
    Rune randomRuneFromHintIndex;
    int randomHintIndex;

    // Real Time Database
    String roomCode;
    FirebaseDatabase realTimeDB;
    DatabaseReference roomRef;
    ValueEventListener eventListener;
    ChildEventListener childEventListener;

    // Score
    TextView playerScore;
    TextView bestPlayerScore;
    int bestScore = 0;


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
        setContentView(R.layout.activity_guest_game_menu);

        // Global variables
        initGlobalVariables();
        setupGameModeVariables();

        // Listeners
        setupButtonsListeners();
        setupEndGameRoomEventListener();
        setupTimer();

        // Score
        setupScoreListeners();
    }

    private void initGlobalVariables() {
        runeCounterText = findViewById(R.id.rune_counter);
        enterRuneButton = findViewById(R.id.enter_rune_button);
        leaveGameButton = findViewById(R.id.leave_game_button);
        context = getBaseContext();
        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name,0);
        playerName = preferences.getString(GlobalVariables.PREFS_player_name, "");
        runesTotal = getIntent().getIntExtra(GlobalVariables.EXN_Runes_total, 0);
        timerTextview = findViewById(R.id.timerTextView);
        timerSelection = getIntent().getLongExtra(GlobalVariables.RealtimeDB_timerSelection, -1);
        hintTitleTextView = findViewById(R.id.hintTitleTextView);
        hintTextView = findViewById(R.id.hintTextView);
        playerScore = findViewById(R.id.playerScore);
        bestPlayerScore = findViewById(R.id.bestPlayerScore);
        roomCode = getIntent().getStringExtra(GlobalVariables.EXN_Room_code);
        realTimeDB = FirebaseDatabase.getInstance();
        roomRef = realTimeDB
                .getReference(GlobalVariables.RealtimeDB_rooms)
                .child(roomCode);
        runesToFind = (List<Rune>) SessionMenuActivity.gameRunes.clone();
    }

    private void setupGameModeVariables() {
        gameMode = getIntent().getIntExtra(GlobalVariables.gameSettings_mode, 0);
        if(gameMode == GlobalVariables.gameSettings_hints_tracking_game) {
            hintTitleTextView.setVisibility(View.VISIBLE);
            hintTextView.setVisibility(View.VISIBLE);
            hintTitleTextView.setText(getString(R.string.next_rune_hint));
            updateHintView();
        }
        else {
            hintTitleTextView.setVisibility(View.GONE);
            hintTextView.setVisibility(View.GONE);
        }
    }

    private void setupTimer(){    // TODO : make timer useless for guest (only there for display) : remove force-quit
        if (timerSelection!=-1){
            if (myTimer != null) myTimer.cancel();
            // Creates a timer that will display its value and do nothing when it runs out.
            // It is the host's game terminating update to the database that will inform the guest
            // about the end of the game
            myTimer = new CustomCountDownTimer(
                    timerSelection,
                    timerTextview,
                    () -> {  } );
            myTimer.start();
        } else {
            // Start a timer
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void updateHintView() {
        if(runesToFind.isEmpty()) {
            hintTitleTextView.setText(getString(R.string.no_hint_remaining));
            hintTextView.setVisibility(View.GONE);
        }
        else if(!runesToFind.contains(randomRuneFromHintIndex)) {
            randomHintIndex = new Random().nextInt(runesToFind.size());
            randomRuneFromHintIndex = runesToFind.get(randomHintIndex);
            hintTextView.setText(randomRuneFromHintIndex.getLocalisationClue());
        }
    }

    @Override
    protected void onStart() {    // TODO : see if updateRuneCounter also works in onResume (to save space)
        super.onStart();
        updateRuneCounter();
    }

    private void updateRuneCounter(){
        int runesProgress = preferences.getInt(GlobalVariables.PREFS_rune_progress, 0);
        @SuppressLint("DefaultLocale") String text = String.format(getString(R.string.rune_counter), runesProgress, runesTotal);
        runeCounterText.setText(text);
    }

    private void setupEndGameRoomEventListener(){
         eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // The room doesn't exist anymore because the host left it
                if (!snapshot.exists() ||snapshot.getValue() == null) {
                    // Cleaning the db wis automated via MultiplayerRoom.onDestroy()
                    Toasty.warning(GuestGameMenu.this, getString(R.string.host_leaves_game), Toast.LENGTH_LONG).show();
                    MainActivity.finishAndGoBackToTitleScreen(GuestGameMenu.this);
                    return;
                }

                // Ignores when the game is still running
                else if (RealTimeDBHelper.hasGameStarted(snapshot)) return;

                // The 'gameStarted' attribute has been set to false by the host. Therefore, we retrieve the reason and end the game,
                // then sends it to the end-of-game protocol
                roomRef.child(GlobalVariables.RealtimeDB_endGameReason).get().addOnCompleteListener(
                        task -> {
                            int reason;
                            if (!task.isSuccessful()) reason = -1;    // hard-coded
                            else {
                                try {
                                    reason = task.getResult().getValue(Integer.class);
                                } catch (NullPointerException e) {
                                    reason = -1;
                                }
                            }
                            gameStoppedByHost(reason);
                });

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealtimeDatabase","Error while checking if room is alive in gameMenu");
            }
         };
         roomRef.addValueEventListener(eventListener);
    }

    private void setupButtonsListeners(){
        leaveGameButton.setOnClickListener(view -> showLeaveDialog());

        enterRuneButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, AskRuneCode.class);
            intent.putExtra(GlobalVariables.EXN_Runes_total, runesTotal);
            intent.putExtra(GlobalVariables.EXN_Room_code, roomCode);
            startActivity(intent);
        });
    }

    private void setupScoreListeners(){
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                for (String key : RealTimeDBHelper.reservedNames) {
                    // Ignore change if it concerns a non-player child
                    if (key.equals(dataSnapshot.getKey())) return;
                }

                // The change is due to a player that has found a rune
                PointsAttribution.hasFirstRuneBeenFound = true;
                HashMap<String, String> objPlayer = (HashMap<String, String>) dataSnapshot.getValue();
                Player updatedPlayer = new Player(objPlayer);

                if (playerName.equals(updatedPlayer.getName())) {
                    playerScore.setText(
                            String.format(getString(R.string.your_score), updatedPlayer.getScore())
                    );
                }
                if (updatedPlayer.getScore() >= bestScore) {
                    bestScore = updatedPlayer.getScore();
                    bestPlayerScore.setText(
                            String.format(getString(R.string.best_score), updatedPlayer.getName(), updatedPlayer.getScore())
                    );
                }
            }

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        roomRef.addChildEventListener(childEventListener);
    }


    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);

        if(gameMode == GlobalVariables.gameSettings_hints_tracking_game) {
            updateHintView();
        }
    }


    @Override
    public void onBackPressed() {
        // To make the dialog pop up when the host tries to press "back"
        showLeaveDialog();
    }

    private void showLeaveDialog(){
        // Shows a dialog to alert the guest they are about to quit the game
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.leave_game_dialog_title))
                .setMessage(getString(R.string.leave_game_dialog_message))
                .setPositiveButton(getString(
                        R.string.leave_game_dialog_positive),
                        (dialogInterface, i) -> intentionallyQuitGameGuest())
                .setNegativeButton(getString(R.string.leave_game_dialog_negative), null)
                .show();
    }

    private void gameStoppedByHost(int reasonID) {
        Toasty.warning(context, getString(R.string.game_has_stopped), Toast.LENGTH_LONG).show();
        // Stops the timer to avoid getting triggered actions after the game has ended
        // myTimer is null when the stopwatch mode is active
        if (myTimer != null) myTimer.cancel();
        roomRef.removeEventListener(eventListener);
        roomRef.removeEventListener(childEventListener);
        EndGameScoreboard.finishAndJoinActivity(this, roomCode, runesTotal, reasonID);
    }

    private void intentionallyQuitGameGuest() {
        // The guest DECIDES to quit the game while it is still active

        // Stops the timer to avoid getting triggered actions after the game has ended
        // myTimer is null when the stopwatch mode is active
        if (myTimer != null) myTimer.cancel();

        /* TODO remove if not necessary
        // The guest is deleted from the Real Time DB
        roomRef.child(playerName).removeValue();
        realTimeDB.getReference(GlobalVariables.RealtimeDB_players)
                .child(playerName)
                .removeValue(); */
        // To avoid getting updates from a game we no longer partake in
        roomRef.removeEventListener(eventListener);
        roomRef.removeEventListener(childEventListener);

        // Makes the guest go back to the title screen
        MainActivity.finishAndGoBackToTitleScreen(this);
    }
}
