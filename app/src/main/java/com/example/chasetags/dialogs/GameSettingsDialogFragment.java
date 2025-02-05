package com.example.chasetags.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.R;
import com.example.chasetags.SessionMenuActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.HashMap;

public class GameSettingsDialogFragment extends DialogFragment {

    // TODO : MOVE this string to GlobalVariables
    public static final String TAG = "game_settings_dialog";
    public static HashMap<String, Integer> gameSettingsMap;

    // UI items
    Toolbar toolbar;
    MaterialSwitch timerSwitch;
    RadioGroup gameEndSelection;
    RadioButton endGameRadio1;
    RadioButton endGameRadio2;
    RadioButton endGameRadio3;
    RadioButton endGameRadio4;
    RadioGroup gameHintsOption;
    RadioButton endGameRadio5;
    RadioButton endGameRadio6;
    Button timerEdit;
    RelativeLayout editTimerLayout;
    TextView gameTimerText;

    MaterialTimePicker timePicker;

    int timerHours;
    int timerMinutes;

    public static void display(FragmentManager fragmentManager) {
        GameSettingsDialogFragment exampleDialog = new GameSettingsDialogFragment();
        exampleDialog.show(fragmentManager, TAG);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setWindowAnimations(R.style.DialogSlideAnim);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_ChaseTags);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.settings_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the HashMap from the session activity
        gameSettingsMap = SessionMenuActivity.gameSettings;

        // Getting views
        toolbar = view.findViewById(R.id.toolbar);
        timerSwitch = view.findViewById(R.id.time_switch);
        gameEndSelection = view.findViewById(R.id.radioGroup);
        endGameRadio1 = view.findViewById(R.id.radio_button_1);
        endGameRadio2 = view.findViewById(R.id.radio_button_2);
        endGameRadio3 = view.findViewById(R.id.radio_button_3);
        endGameRadio4 = view.findViewById(R.id.radio_button_4);
        gameHintsOption = view.findViewById(R.id.hints_options_radios);
        endGameRadio5 = view.findViewById(R.id.radio_button_5);
        endGameRadio6 = view.findViewById(R.id.radio_button_6);
        timerEdit = view.findViewById(R.id.timer_edit);
        editTimerLayout = view.findViewById(R.id.edit_timer_layout);
        gameTimerText = view.findViewById(R.id.game_timer_text);

        timerEdit.setOnClickListener(view1 -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            timePicker.show(fragmentManager, "tag");

            setTimerEventListener();
        });

        // Set the gameSettings that was previously saved
        setChosenGameSettings();

        toolbar.setNavigationOnClickListener(v -> dismiss());
        toolbar.inflateMenu(R.menu.game_settings_menu);

        // When timer is toggled change the layout
        timerSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                endGameRadio2.setChecked(false);
                endGameRadio1.setChecked(true);
                editTimerLayout.setVisibility(View.VISIBLE);
                endGameRadio1.setVisibility(View.VISIBLE);
                endGameRadio2.setVisibility(View.GONE);
            } else {
                endGameRadio2.setChecked(true);
                editTimerLayout.setVisibility(View.GONE);
                endGameRadio1.setVisibility(View.GONE);
                endGameRadio2.setVisibility(View.VISIBLE);
            }
        });

        // When saved is pressed save the settings to the hashMap
        toolbar.setOnMenuItemClickListener(item -> {

            // Set timer in the HashMap
            if (!timerSwitch.isChecked()) {
                gameSettingsMap.put(GlobalVariables.gameSettings_timer_status,GlobalVariables.gameSettings_timer_disabled);
                SessionMenuActivity.gameTimerSelection = (long) -1;
                SessionMenuActivity.timeChosen.setText(getText(R.string.infinite_timer));
            } else {
                gameSettingsMap.put(GlobalVariables.gameSettings_timer_status,GlobalVariables.gameSettings_timer_enabled);

                gameSettingsMap.put(GlobalVariables.gameSettings_timer_hours,timerHours);
                gameSettingsMap.put(GlobalVariables.gameSettings_timer_minutes,timerMinutes);
                Long hoursMillis = (long) timerHours * (1000*60*60);
                Long minutesMillis = (long) timerMinutes * (1000*60);
                SessionMenuActivity.gameTimerSelection = hoursMillis + minutesMillis;
                SessionMenuActivity.timeChosen.setText(String.format(getString(R.string.game_timer), timerHours, timerMinutes));
            }

            int chosenGameEnd = gameEndSelection.getCheckedRadioButtonId();

            // Set the game end setting in the HashMap
            if (chosenGameEnd == R.id.radio_button_1) {
                gameSettingsMap.put(GlobalVariables.gameSettings_ending_choice,GlobalVariables.gameSettings_ending_timerEnd);
            } else if (chosenGameEnd == R.id.radio_button_2) {
                gameSettingsMap.put(GlobalVariables.gameSettings_ending_choice,GlobalVariables.gameSettings_ending_allPlayers);
            } else if (chosenGameEnd == R.id.radio_button_3) {
                gameSettingsMap.put(GlobalVariables.gameSettings_ending_choice,GlobalVariables.gameSettings_ending_firstPlayer);
            } else {
                gameSettingsMap.put(GlobalVariables.gameSettings_ending_choice,GlobalVariables.gameSettings_ending_3firstPlayers);
            }

            int chosenHintsOptions = gameHintsOption.getCheckedRadioButtonId();

            // Set the game end setting in the HashMap
            if (chosenHintsOptions == R.id.radio_button_5) {
                gameSettingsMap.put(GlobalVariables.gameSettings_hints,GlobalVariables.gameSettings_hints_rune_chase);
            } else {
                gameSettingsMap.put(GlobalVariables.gameSettings_hints,GlobalVariables.gameSettings_hints_tracking_game);
            }

            dismiss();
            return true;
        });
    }

    private void setTimePicker() {
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(timerHours)
                .setMinute(timerMinutes)
                .setPositiveButtonText(R.string.save_timer_button)
                .setTitleText(R.string.timer_picker)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .build();
    }

    private void setTimerEventListener() {
        timePicker.addOnPositiveButtonClickListener(view1 -> {
            timerHours = timePicker.getHour();
            timerMinutes = timePicker.getMinute();

            gameTimerText.setText(String.format(getString(R.string.timer_picker_time),
                    timerHours, timerMinutes));
        });
    }

    private void setChosenGameSettings() {

        // Set the views with chosen settings of the timer
        timerHours = gameSettingsMap.get(GlobalVariables.gameSettings_timer_hours);
        timerMinutes = gameSettingsMap.get(GlobalVariables.gameSettings_timer_minutes);
        if (gameSettingsMap.get(GlobalVariables.gameSettings_timer_status) == 1) {
            timerSwitch.setChecked(true);
            editTimerLayout.setVisibility(View.VISIBLE);
            endGameRadio2.setVisibility(View.GONE);

            gameTimerText.setText(String.format(getString(R.string.timer_picker_time),
                    timerHours, timerMinutes));

        } else {

            gameTimerText.setText(String.format(getString(R.string.timer_picker_time),
                    timerHours, timerMinutes));

            timerSwitch.setChecked(false);
            editTimerLayout.setVisibility(View.GONE);
            endGameRadio1.setVisibility(View.GONE);
        }
        setTimePicker();

        // Set the view of the chosen way the game end
        switch (gameSettingsMap.get(GlobalVariables.gameSettings_ending_choice)) {
            case GlobalVariables.gameSettings_ending_timerEnd:
                endGameRadio1.setChecked(true);
                break;
            case GlobalVariables.gameSettings_ending_allPlayers:
                endGameRadio2.setChecked(true);
                break;
            case GlobalVariables.gameSettings_ending_firstPlayer:
                endGameRadio3.setChecked(true);
                break;
            case GlobalVariables.gameSettings_ending_3firstPlayers:
                endGameRadio4.setChecked(true);
                break;
        }

        // Set the view of the chosen way hints are released
        switch (gameSettingsMap.get(GlobalVariables.gameSettings_hints)) {
            case GlobalVariables.gameSettings_hints_rune_chase:
                endGameRadio5.setChecked(true);
                break;
            case GlobalVariables.gameSettings_hints_tracking_game:
                endGameRadio6.setChecked(true);
                break;
        }
    }
}
