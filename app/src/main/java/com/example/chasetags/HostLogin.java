package com.example.chasetags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.utils.GlobalVariables;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HostLogin extends AppCompatActivity {

    // Miscellaneous
    SharedPreferences preferences;

    // UI Stuff
    Toolbar topAppBar;
    TextView welcomeText;
    Button loginButton;
    TextView mustLogin;
    Button newSessionButton;
    Button manageRunesButton;
    Button logoutButton;

    // Realtime DB Stuff
    FirebaseDatabase db;

    // Login
    ActivityResultLauncher<Intent> signInLauncher;
    FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_login);

        initGlobalVariables();

        // Important for sign-in, do not move out of onCreate
        signInLauncher = registerForActivityResult(
                new FirebaseAuthUIActivityResultContract(),
                result -> onSignInResult(result)
        );

        // Prevents unsigned people from accessible further functionalities
        updateInteractivityUI((user != null));

        setupButtonListeners();
    }

    private void initGlobalVariables() {
        // UI Stuff
        topAppBar = findViewById(R.id.topAppBar);
        welcomeText = findViewById(R.id.welcomeText);
        loginButton = findViewById(R.id.login);
        mustLogin = findViewById(R.id.must_login);
        newSessionButton = findViewById(R.id.session_menu);
        manageRunesButton = findViewById(R.id.manage_runes);
        logoutButton = findViewById(R.id.logout);

        // Realtime DB Stuff
        db = FirebaseDatabase.getInstance();

        // Get the playerName and store it
        preferences = getSharedPreferences(GlobalVariables.PREFS_preference_name, 0);

        // Login stuff
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupButtonListeners() {
        newSessionButton.setOnClickListener(view -> switchToSessionMenuActivity());
        manageRunesButton.setOnClickListener(view -> switchToManageRunesActivity());

        topAppBar.setNavigationOnClickListener(view -> finish());

        loginButton.setOnClickListener(view -> {
            // Lists all the available sign-in providers (= sign-in methods)
            final List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build()
                    //new AuthUI.IdpConfig.GoogleBuilder().build()
            );
            // Create and launch sign-in intent
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();
            signInLauncher.launch(signInIntent);
        });

        logoutButton.setOnClickListener(view -> {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> updateInteractivityUI(false));
            updateInteractivityUI(false);
        });
    }

    private void updateInteractivityUI(boolean isLoggedIn) {
        String displayedName = "";
        if (user != null && user.getDisplayName() != null) displayedName = user.getDisplayName();
        welcomeText.setText(String.format(getString(R.string.welcome_login), displayedName));
        welcomeText.setVisibility((isLoggedIn) ? View.VISIBLE : View.INVISIBLE);
        loginButton.setVisibility((isLoggedIn) ? View.INVISIBLE : View.VISIBLE);
        mustLogin.setVisibility((isLoggedIn) ? View.INVISIBLE : View.VISIBLE);
        newSessionButton.setEnabled(isLoggedIn);
        manageRunesButton.setEnabled(isLoggedIn);
        logoutButton.setVisibility((isLoggedIn) ? View.VISIBLE : View.INVISIBLE);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        String toast;
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            user = FirebaseAuth.getInstance().getCurrentUser();
            updateInteractivityUI(true);
            toast = "Successfully logged in :)";

            // Add the userUID as player name to sharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(GlobalVariables.PREFS_player_name, user.getUid());
            editor.apply();

        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            updateInteractivityUI(false);
            if (response == null) toast = "User cancelled login :(";
            else if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) toast ="Login failed due to connection :(";
            else toast = "Login failed for an unknown reason :(";
        }
        Toast.makeText(HostLogin.this, toast, Toast.LENGTH_LONG).show();
    }


    private void editPlayerNamePreferences() {
        // Add the playerName to sharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(GlobalVariables.PREFS_player_name, user.getUid());
        editor.apply();
    }

    private void switchToSessionMenuActivity() {
        SessionMenuActivity.gameRunes = new ArrayList<>();
        editPlayerNamePreferences();
        Intent intent = new Intent(this, SessionMenuActivity.class);
        startActivity(intent);
    }

    private void switchToManageRunesActivity() {
        Intent intent = new Intent(this, ManageProfileRunes.class);
        startActivity(intent);
    }
}