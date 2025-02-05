package com.example.chasetags;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;

public class UserIntroductionActivity extends AppCompatActivity {

    TextView pageCounter;
    TextView title;
    TextView description;

    ImageView image1;
    ImageView image2;
    ImageView image3;
    ImageView image4;
    ImageView image5;

    Button back_button;
    Button next_button;

    Integer currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_introduction);

        currentPage = 1;

        pageCounter = findViewById(R.id.page_counter);
        title = findViewById(R.id.title);
        description = findViewById(R.id.description);
        image1 = findViewById(R.id.image1);
        image2 = findViewById(R.id.image2);
        image3 = findViewById(R.id.image3);
        image4 = findViewById(R.id.image4);
        image5 = findViewById(R.id.image5);
        back_button = findViewById(R.id.back_button);
        next_button = findViewById(R.id.next_button);

        checkPage();
        setupButtonsListeners();
    }

    private void setupButtonsListeners() {

        back_button.setOnClickListener(view -> {

            currentPage --;
            checkPage();

        });

        next_button.setOnClickListener(view -> {

            currentPage ++;
            checkPage();

        });

    }

    private void checkPage() {

        System.out.println(currentPage);

        switch (currentPage) {
            case 1:
                pageCounter.setText(String.format(getString(R.string.page_number),currentPage));
                back_button.setVisibility(View.INVISIBLE);

                image1.setVisibility(View.VISIBLE);
                image2.setVisibility(View.INVISIBLE);

                title.setText(R.string.tutorial_title_1);
                description.setText(R.string.tutorial_desc_1);
                break;
            case 2:
                pageCounter.setText(String.format(getString(R.string.page_number),currentPage));
                back_button.setVisibility(View.VISIBLE);

                image1.setVisibility(View.INVISIBLE);
                image2.setVisibility(View.VISIBLE);
                image3.setVisibility(View.INVISIBLE);

                title.setText(R.string.tutorial_title_2);
                description.setText(R.string.tutorial_desc_2);
                break;
            case 3:
                pageCounter.setText(String.format(getString(R.string.page_number),currentPage));

                image2.setVisibility(View.INVISIBLE);
                image3.setVisibility(View.VISIBLE);
                image4.setVisibility(View.INVISIBLE);

                title.setText(R.string.tutorial_title_3);
                description.setText(R.string.tutorial_desc_3);
                break;
            case 4:
                pageCounter.setText(String.format(getString(R.string.page_number),currentPage));

                image3.setVisibility(View.INVISIBLE);
                image4.setVisibility(View.VISIBLE);
                image5.setVisibility(View.INVISIBLE);

                title.setText(R.string.tutorial_title_4);
                description.setText(R.string.tutorial_desc_4);

                next_button.setText(getString(R.string.next_button));
                break;
            case 5:
                pageCounter.setText(String.format(getString(R.string.page_number),currentPage));

                image4.setVisibility(View.INVISIBLE);
                image5.setVisibility(View.VISIBLE);

                title.setText(R.string.tutorial_title_5);
                description.setText(R.string.tutorial_desc_5);

                next_button.setText(getString(R.string.end_tutorial));
                break;
            case 6:
                goToMainMenu();
                break;
        }

    }

    private void goToMainMenu() {
        SharedPreferences tutorialPreference = getSharedPreferences(GlobalVariables.PREFS_preference_tutorial,0);
        SharedPreferences.Editor editor = tutorialPreference.edit();
        editor.putBoolean(GlobalVariables.PREFS_showTutorial,false);
        editor.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }
}