package com.example.chasetags.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PointsAttribution {
    public static int NEW_RUNE = 10;
    public static int NEW_RUNE_WITH_HINT = 5;
    public static int NEW_RUNE_FIRST = 15;

    public static int NEW_HINT = -2;

    public static int QUIZ_CORRECT = 5;
    public static int QUIZ_WRONG = -5;

    public static int WHEEL_OF_FORTUNE_MAX = 20;
    public static int WHEEL_OF_FORTUNE_MIN = -20;

    public static boolean hasFirstRuneBeenFound = false;


    public static void playerFoundRune(String runeID, String playerName, DatabaseReference runeRef, DatabaseReference playerRef, String roomCode) {
        // find the rune in the database and add the player to the list of players who found it
        runeRef.child("foundBy").child(playerName).setValue(true);

        final int[] hint_score = new int[1];
        DatabaseReference runeScore = FirebaseDatabase.getInstance()
                .getReference(GlobalVariables.RealtimeDB_rooms)
                .child(roomCode).child(GlobalVariables.RealtimeDB_Room_Runes_list).child(runeID).child(GlobalVariables.Rune_score);

        runeScore.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                // To avoid getting an exception when the room is deleted from the realtime DB
                if (dataSnapshot == null || !dataSnapshot.exists()) return;
                String scoreStr = dataSnapshot.getValue(String.class);
                if (scoreStr == null) return;

                // The actual code
                hint_score[0] = Integer.parseInt(dataSnapshot.getValue(String.class));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }


        });


        // add points to the player
        Task<DataSnapshot> scoreRef = playerRef.child(GlobalVariables.RealtimeDB_player_score).get();
        scoreRef.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int addScore = 0;
                if (!hasFirstRuneBeenFound) {
                    /* If !hasFirstRuneBeenFound, that means that this is the very first rune found
                     of te game. Then, when the realtime DB is updated to give points to the player
                     who found it, this change will be caught by the score listener
                     (see GuestGameMenu.setupScoreListener for more info) of ALL the players,
                     which will set hasFirstRuneBeenFound to true for ALL the players.
                     Therefore, entering this 'if' block can only be done once per game.
                     */
                    addScore += NEW_RUNE_FIRST;
                }
                int score = Integer.parseInt((String) task.getResult().getValue()) + addScore + hint_score[0];
                playerRef.child(GlobalVariables.RealtimeDB_player_score).setValue(String.format("%d", score));
            }
        });
    }
}
