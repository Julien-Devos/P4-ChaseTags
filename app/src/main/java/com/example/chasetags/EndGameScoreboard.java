package com.example.chasetags;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.adapters.PlayerAdapter;
import com.example.chasetags.player.Player;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.example.chasetags.utils.PointsAttribution;
import com.example.chasetags.utils.RealTimeDBHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class EndGameScoreboard extends AppCompatActivity {

    // Miscellaneous
    int totalRunes;

    // UI Stuff
    Button exit_button;
    ArrayList<Player> guestsList;
    TextView reasonText;
    ListView scoreboard;

    // Realtime DB
    DatabaseReference roomRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game_scoreboard);

        // Global variables init
        initGlobalVariables();

        // Retrieving the list of guests from the Real Time DB and sorting it by
        // increasing number of runes collected.
        setupScoreboard();

        // Listener and UI stuff
        setupButtonListener();
    }
    
    private void initGlobalVariables() {
        exit_button = findViewById(R.id.go_back_to_title_screen);
        scoreboard = findViewById(R.id.scoreboard);
        guestsList = new ArrayList<>();
        totalRunes = getIntent().getIntExtra(GlobalVariables.EXN_Runes_total, 0);
        String roomCode = getIntent().getStringExtra(GlobalVariables.EXN_Room_code);
        reasonText = findViewById(R.id.reason_text);
        int endGameReason = getIntent().getIntExtra(GlobalVariables.EXN_EndGame_Reason, R.string.end_game_unknown_reason);
        reasonText.setText(getString(endGameReason));
        roomRef = FirebaseDatabase.getInstance()
                .getReference(GlobalVariables.RealtimeDB_rooms)
                .child(roomCode);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Resets the public static variable that states whether a rune has already been found
        // by another player, as to not corrupt the next game's points
        PointsAttribution.hasFirstRuneBeenFound = false;

        // Deletes the room from the Real Time DB, as it will never be useful again
        // TODO : adapt the code so that roomRef is only deleted when all the players received the
        //    DB info (i.e. had the time to fully load this activity
        MainActivity.finishAndGoBackToTitleScreen(this);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Makes the user go back to the title screen via onDestroy()
        finish();
    }

    private void setupButtonListener() {

        /* Merely exits the activity and goes back to main menu via onDestroy
        Same stuff as anywhere else in the project, with the small
        difference that we clear the activity history before going back
        to the title screen
         */
        AppCompatActivity thisActivity = this;
        exit_button.setOnClickListener(view -> thisActivity.finish());
    }


    private void setupScoreboard() {
        roomRef.get().addOnCompleteListener(task -> {
            // Ignore failed call to DB
            if (!task.isSuccessful())    // TODO : REPLACE hard-coded String
                Toast.makeText(EndGameScoreboard.this,
                        "Error while fetching scoreboard data",
                        Toast.LENGTH_LONG).show();

            DataSnapshot roomSnapshot = task.getResult();
            // Ignore empty room data
            if (!roomSnapshot.exists()) return;

            // Fills the list with the guests
            RealTimeDBHelper.refillListWithPlayers(guestsList, roomSnapshot);

            // Then update the ListView
            scoreboard.setAdapter(new PlayerAdapter(guestsList, EndGameScoreboard.this, totalRunes, this.getBaseContext()));
        });
    }


    protected static void finishAndJoinActivity(AppCompatActivity from, String roomCode, int runesTotal, int endGameReason) {
        Intent intent = new Intent(from.getBaseContext(), EndGameScoreboard.class);
        intent.putExtra(GlobalVariables.EXN_Room_code, roomCode)
                .putExtra(GlobalVariables.EXN_Runes_total, runesTotal);
        if (endGameReason >= 0) intent.putExtra(GlobalVariables.EXN_EndGame_Reason, endGameReason);
        from.startActivity(intent);
        from.finish();
    }
}