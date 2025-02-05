package com.example.chasetags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.utils.CleanRealtimeDB;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;

public class MainActivity extends AppCompatActivity {

    // Miscellaneous
    SharedPreferences preferences;

    // UI Stuff
    Button hostLoginButton;
    Button joinLobbyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences tutorialPreference = getSharedPreferences(GlobalVariables.PREFS_preference_tutorial,0);
        boolean displayTutorial = tutorialPreference.getBoolean(GlobalVariables.PREFS_showTutorial,true);

        if (displayTutorial) {
            Intent intent = new Intent(this, UserIntroductionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        } else {
            setContentView(R.layout.activity_main);

            // Change navigation bar color since it's different in this activity
            getWindow().setNavigationBarColor(getApplicationContext().getColor(R.color.ic_launcher_background));

            initGlobalVariables();
            setupButtonListeners();
        }
    }

    private void initGlobalVariables() {
        // UI Stuff
        joinLobbyButton = findViewById(R.id.join_room);
        hostLoginButton = findViewById(R.id.use_app_as_host);

        // Clear all SharedPreferences set before when the app opens
        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name,0);
        preferences.edit().clear().apply();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }

    private void setupButtonListeners() {
        hostLoginButton.setOnClickListener(
                view -> switchToActivity(HostLogin.class));
        joinLobbyButton.setOnClickListener(
                view -> MainActivity.this.switchToActivity(AskRoomCode.class));
    }

    private void switchToActivity(Class<?> activity) {
        // Lock UI
        hostLoginButton.setEnabled(false);
        joinLobbyButton.setEnabled(false);

        // Do the switch
        startActivity(new Intent(this, activity));

        // Unlock UI
        hostLoginButton.setEnabled(true);
        joinLobbyButton.setEnabled(true);
    }

    public static void finishAndGoBackToTitleScreen(AppCompatActivity from) {
        // Cleansing everything
        SessionMenuActivity.gameRunes.clear();
        // And going back to the title screen
        Intent intent = new Intent(from.getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        from.startActivity(intent);
        from.finish();
    }
}
