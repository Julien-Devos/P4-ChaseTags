package com.example.chasetags.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public abstract class CleanRealtimeDB {
    static FirebaseDatabase db;
    static DatabaseReference roomRef;
    static String playerName;
    static String playerRole;

    public static void setupDBClean(
            FirebaseDatabase db,
            String roomCode,
            String playerName,
            String playerRole
    ) {
        // Assigns values
        CleanRealtimeDB.db = db;
        CleanRealtimeDB.roomRef = db.getReference(GlobalVariables.RealtimeDB_rooms).child(roomCode);
        CleanRealtimeDB.playerName = playerName;
        CleanRealtimeDB.playerRole = playerRole;

        // And sets up a disconnect-safe cleanup
        if (playerRole.equals(GlobalVariables.RealtimeDB_player_guest)) {
            // If the player is a guest, just removes their name from the room
            roomRef.child(playerName).onDisconnect().removeValue();
        } else {
            // If the player is a guest, removes the entire room
            roomRef.onDisconnect().removeValue();
        }

        db.getReference(GlobalVariables.RealtimeDB_players)
                .child(playerName)
                .onDisconnect()
                .removeValue();
    }

    public static void applyDBClean() {

        // Some of these calls are redundant when executed as host, but that's an easy way
        // of handling all the DB actions
        db.getReference(GlobalVariables.RealtimeDB_players)
                .child(playerName)
                .removeValue();
        roomRef.child(playerName).removeValue();
        if (playerRole.equals(GlobalVariables.RealtimeDB_player_host)) roomRef.removeValue();
        clearVariables();
    }

    private static void clearVariables() {
        db = null;
        roomRef = null;
        playerName = null;
        playerRole = null;
    }

}
