package com.example.chasetags.utils;


import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chasetags.MainActivity;
import com.example.chasetags.R;

import java.util.Locale;


public class CustomCountDownTimer extends CountDownTimer {
    private final TextView mTextViewCountDown;
    private final TimerRunOutAction runOutAction;
    private String timeLeftFormatted;

    /**
     * Instantiates a new Countdown timer.
     * @param startTimeInMillis the duration in millseconds of the countdown timer before it runs out
     * @param textViewCountDown the text field to display the time remaining in
     * @param runOutAction the action to perform when the countdown timer runs out
     */
    public CustomCountDownTimer(
            long startTimeInMillis,
            TextView textViewCountDown,
            TimerRunOutAction runOutAction
    ) {
        super(startTimeInMillis, 1000);
        this.mTextViewCountDown = textViewCountDown;
        this.runOutAction = runOutAction;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        // Updates the TextView that displays the timer
        int hours = (int) (millisUntilFinished / (1000*60*60)) % 24;
        int minutes = (int) (millisUntilFinished / (1000*60)) % 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        if (hours == 0) {
            timeLeftFormatted = String.format(mTextViewCountDown.getResources().getString(R.string.remaining_time_hours), minutes, seconds);
        } else {
            timeLeftFormatted = String.format(mTextViewCountDown.getResources().getString(R.string.remaining_time), hours, minutes, seconds);
        }

        mTextViewCountDown.setText(timeLeftFormatted);
    }

    @Override
    public void onFinish() { runOutAction.execute(); }
}