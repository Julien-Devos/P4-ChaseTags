package com.example.chasetags.askRunes;

import static com.example.chasetags.MultiplayerRoom.playerName;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chasetags.GuestGameMenu;
import com.example.chasetags.R;
import com.example.chasetags.SessionMenuActivity;
import com.example.chasetags.player.Player;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.PointsAttribution;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

public class AskRuneGeneric extends AppCompatActivity {

    SharedPreferences preferences;
    String roomCode;
    String runeType;

    Toolbar topAppBar;
    NavigationBarView bottomNavBar;

    FirebaseDatabase realTimeDB;
    DatabaseReference playerRef;


    protected void onCreate(String typeOfRune) {
        runeType = typeOfRune;

        if (runeType.equals(GlobalVariables.Rune_code_object_type)) {
            setContentView(R.layout.activity_ask_rune_code);
        } else if (runeType.equals(GlobalVariables.Rune_nfc_object_type)) {
            setContentView(R.layout.activity_ask_rune_nfc);
        } else {
            setContentView(R.layout.activity_ask_qr_rune);
        }

        // Change navigation bar color since it's different in this activity
        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name,0);

        realTimeDB = FirebaseDatabase.getInstance();
        roomCode = getIntent().getStringExtra(GlobalVariables.EXN_Room_code);
        playerRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_rooms).child(roomCode).child(playerName);

        if (runeType.equals(GlobalVariables.Rune_code_object_type) || runeType.equals(GlobalVariables.Rune_nfc_object_type)) {
            topAppBar = findViewById(R.id.topAppBar);
            bottomNavBar = findViewById(R.id.bottom_navigation);
            setupToolbarNavigationListener();

            int selectedNavItem = getIntent().getIntExtra("itemIdSelected", -1);
            if (selectedNavItem != -1) {
                bottomNavBar.setSelectedItemId(selectedNavItem);
            }
        }
    }

    private void setupToolbarNavigationListener(){
        topAppBar.setNavigationOnClickListener(view -> finish());

        bottomNavBar.setOnItemSelectedListener(item -> {
            int itemID = item.getItemId();

            if (itemID == R.id.item_1) {
                if (!runeType.equals(GlobalVariables.Rune_code_object_type)) {
                    change_ask_rune_type(itemID, AskRuneCode.class);
                }
                return true;
            } else if (itemID == R.id.item_2) {
                if (!runeType.equals(GlobalVariables.Rune_nfc_object_type)) {
                    change_ask_rune_type(itemID, AskRuneNFC.class);
                }
                return true;
            } else {
                if (!runeType.equals(GlobalVariables.Rune_qr_object_type)) {
                    change_ask_rune_type(itemID, AskQRRune.class);
                }
                return true;
            }
        });
    }

    private void change_ask_rune_type(Integer itemID, Class askRuneTypeClass) {
        Intent intent = new Intent(getApplicationContext(), askRuneTypeClass);
        intent.putExtra("itemIdSelected", itemID);
        intent.putExtra(GlobalVariables.EXN_Room_code, roomCode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    /*
    * Removes the rune from the player's list of runes
    *
    * Params :
    *   @param runeString the rune to remove. If isCode is true, this is the code of the rune. Otherwise, it is the ID of the rune
    *   @param isCode true if the runeString is the code of the rune, false if it is the ID of the rune
    * */
    public void removeRuneFromPlayer(String runeString){
        String playerName = preferences.getString(GlobalVariables.PREFS_player_name, "");
        DatabaseReference playerRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_rooms).child(roomCode).child(playerName);

        playerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    HashMap<String, String> obj = (HashMap<String, String>) snapshot.getValue();
                    Player player = new Player(obj);
                    // get runeID from SessionMenuActivity.localRunes
                    for (int i = 0; i < SessionMenuActivity.gameRunes.size(); i++) {
                        Rune rune = SessionMenuActivity.gameRunes.get(i);
                        String toCompare = runeType.equals(GlobalVariables.Rune_qr_object_type) ? rune.getRuneID() : rune.getValue();
                        if (toCompare.equals(runeString)) {
                            player.addRuneCollected(rune.getRuneID());
                            attributePointsToPlayer(rune.getRuneID());
                            GuestGameMenu.runesToFind.remove(rune);
                            break;
                        }
                    }
                    playerRef.setValue(player.toHashMap());
                }
            }
        });
    }

    public void handleSuccess(String runeString) {
        // suffering from success...
        incrementCounter();
        addRuneToSharedPrefList(runeString);
        removeRuneFromPlayer(runeString);
        Toast.makeText(getApplicationContext(), getString(R.string.toast_rune_entered_successfully), Toast.LENGTH_SHORT).show();
        finish();
    }

    public Boolean isRuneInputInRunesList(String runeInput){
        for (Rune currRune : SessionMenuActivity.gameRunes) {
            String toCompare = runeType.equals(GlobalVariables.Rune_qr_object_type) ? currRune.getRuneID() : currRune.getValue();
            if (toCompare.equals(runeInput)){
                return true;
            }
        }
        return false;
    }

    public Boolean hasRuneAlreadyBeenEntered(String runeString){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Get JSON String notation from shared preferences
        String json = preferences.getString(GlobalVariables.PREFS_runes_validated_list, "");
        // Unpack JSON String notation into a String array
        String[] validatedRunes = gson.fromJson(json, String[].class);

        if (validatedRunes != null) {
            for (String runeCode : validatedRunes) {
                if (runeCode.equals(runeString)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void incrementCounter(){
        int runesProgress = preferences.getInt(GlobalVariables.PREFS_rune_progress, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(GlobalVariables.PREFS_rune_progress, runesProgress+1);
        editor.apply();
    }

    private void addRuneToSharedPrefList(String code){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Get JSON String notation from shared preferences
        String json = preferences.getString(GlobalVariables.PREFS_runes_validated_list, "");
        // Unpack JSON String notation into a String array
        String[] validatedRunes = gson.fromJson(json, String[].class);

        String[] newValidatedRunes;
        // Add new rune to the array
        if (validatedRunes == null) {
            newValidatedRunes = new String[1];
            newValidatedRunes[0] = code;
        }
        else {
            newValidatedRunes = new String[validatedRunes.length + 1];
            System.arraycopy(validatedRunes, 0, newValidatedRunes, 0, validatedRunes.length);
            newValidatedRunes[validatedRunes.length] = code;
        }

        // Convert the array into a JSON String notation
        String newValidatedRunesJSON = gson.toJson(newValidatedRunes);

        // Store it into shared preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(GlobalVariables.PREFS_runes_validated_list, newValidatedRunesJSON);
        editor.apply();
    }

    private void attributePointsToPlayer(String runeID) {
        DatabaseReference runeRef = realTimeDB.getReference(GlobalVariables.RealtimeDB_rooms).child(roomCode).child(GlobalVariables.RealtimeDB_Room_Runes_list).child(runeID);
        PointsAttribution.playerFoundRune(runeID, playerName, runeRef, playerRef, roomCode);

    }
}
