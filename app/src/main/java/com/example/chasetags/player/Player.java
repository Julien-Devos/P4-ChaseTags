package com.example.chasetags.player;

import androidx.annotation.NonNull;

import com.example.chasetags.SessionMenuActivity;
import com.example.chasetags.utils.GlobalVariables;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

@IgnoreExtraProperties
public class Player {

    public String name = "";
    public String role = "";
    public Integer runesCollected = 0;
    public String runesLeft = "";
    public Integer score = 0;

    public Player(){

    }

    public Player(String name, String role) {
        setName(name);
        setRole(role);
        this.runesCollected = 0;
        this.runesLeft = "";
        this.score = 0;
    }

    public Player(HashMap<String, String> hashmap){
        this.name = hashmap.get(GlobalVariables.RealtimeDB_player_name);
        this.role = hashmap.get(GlobalVariables.RealtimeDB_player_role);
        this.runesCollected = (hashmap.get(GlobalVariables.RealtimeDB_player_runesCollected) == null) ? 0 : Integer.parseInt(hashmap.get(GlobalVariables.RealtimeDB_player_runesCollected));
        this.runesLeft = hashmap.get(GlobalVariables.RealtimeDB_player_runesLeft);
        this.score = (hashmap.get(GlobalVariables.RealtimeDB_player_score) == null) ? 0 : Integer.parseInt(hashmap.get(GlobalVariables.RealtimeDB_player_score));
    }

    public Player(Object obj) {
        // Used in MultiplayerRoom.java to convert a generic Object type data snapshot into a Player
        this((HashMap<String, String>) obj);
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> player = new HashMap<>();
        player.put(GlobalVariables.RealtimeDB_player_name, name);
        player.put(GlobalVariables.RealtimeDB_player_role, role);
        player.put(GlobalVariables.RealtimeDB_player_runesCollected, runesCollected.toString());
        player.put(GlobalVariables.RealtimeDB_player_runesLeft, runesLeft);
        player.put(GlobalVariables.RealtimeDB_player_score, score.toString());
        return player;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getRunesCollected() {
        return runesCollected;
    }

    public ArrayList<String> getRunesLeft() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(runesLeft, ArrayList.class);
    }

    public boolean hasFoundAllRunes() {
        return getRunesLeft().isEmpty();
    }

    public void makeRunesLeftList() {
        ArrayList<String> runesLeft = new ArrayList<>();

        for (int i = 0; i < SessionMenuActivity.gameRunes.size(); i++) {
            runesLeft.add(SessionMenuActivity.gameRunes.get(i).getRuneID());
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.runesLeft = gson.toJson(runesLeft);
    }

    public void addRuneCollected(String runeID) {
        this.runesCollected++;
        // Remove the rune from the list of runes left
        ArrayList<String> runesLeft = getRunesLeft();
        runesLeft.remove(runeID);

        // Transform runesLeft into a JSON string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.runesLeft = gson.toJson(runesLeft);
    }

    public Integer getScore() {
        return score;
    }

    public void addScore(Integer score) {
        this.score += score;
    }

    @NonNull
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", runesCollected=" + runesCollected +
                ", runesLeft=" + runesLeft +
                '}';
    }
}
