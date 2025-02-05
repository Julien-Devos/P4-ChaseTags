package com.example.chasetags.utils;

import android.os.Build;

import com.example.chasetags.player.Player;
import com.google.firebase.database.DataSnapshot;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public abstract class RealTimeDBHelper {

    public static String[] reservedNames = {
            GlobalVariables.RealtimeDB_gameStarted,
            GlobalVariables.RealtimeDB_timerSelection,
            GlobalVariables.RealtimeDB_Room_Runes_list,
            GlobalVariables.RealtimeDB_endGameReason,
            GlobalVariables.gameSettings_mode
    };


    public static boolean hasGameStarted(DataSnapshot snapshot) {
        // Used to detect if the host has started the game
        for (DataSnapshot child: snapshot.getChildren()) {
            if (!Objects.equals(child.getKey(), GlobalVariables.RealtimeDB_gameStarted)) continue;
            // The child snapshot is the roomArgument 'gameStarted'. We return its value :
            return (Objects.equals(child.getValue(), true));
        }
        return false;
    }

    /**
     * Returns whether a data snapshot child represents a Player (and not a room option)
     * @param snap the snapshot to analyse
     * @return the Player object equivalent of 'snap' if it represents a guest. If 'snap' doesn't
     * represent a player or represents the host, returns null.
     */
    private static Player isGuest(@NonNull DataSnapshot snap) {
        String key = snap.getKey();
        if (key == null) return null;

        for (String reservedKey : reservedNames) {
            if (key.equals(reservedKey)) return null;    // snap doesn't represent a player
        }

        // snap represents a player :
        Player player = new Player((HashMap<String, String>) snap.getValue());
        if (player.getRole().equals(GlobalVariables.RealtimeDB_player_guest)) return player;
        return null;    // player is the host
    }

    /**
     * Empties the given list, then fills it with all the players in the DB room represented
     * by a snapshot. The game's host is not added into the list, only guests are.
     * @param listToFill The list to fill. Will be emptied before the operation.
     * @param roomSnapshot The snapshot that represents the current state of the room in the
     *                     real time database
     */
    public static void refillListWithPlayers(ArrayList<Player> listToFill, DataSnapshot roomSnapshot) {
        listToFill.clear();
        for (DataSnapshot snap : roomSnapshot.getChildren()) {
            Player snapEquivalent = isGuest(snap);
            if (snapEquivalent != null) listToFill.add(snapEquivalent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            listToFill.sort((p1, p2) -> p2.getScore() - p1.getScore());
    }
}
