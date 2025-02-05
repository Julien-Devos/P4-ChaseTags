package com.example.chasetags.askRunes;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.chasetags.R;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.HandleNFCTagScan;
import com.google.android.material.textfield.TextInputLayout;

public class AskRuneCode extends AskRuneGeneric {
    EditText runeInputText;
    Button validateRuneInputButton;
    TextInputLayout runeCodeInputLayout;

    String runeInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(GlobalVariables.Rune_code_object_type);

        runeInputText = findViewById(R.id.rune_input_text);
        validateRuneInputButton = findViewById(R.id.validate_rune_input_button);
        runeCodeInputLayout = findViewById(R.id.rune_code_input_layout);

        addButtonsListener();
    }

    private void addButtonsListener(){
        validateRuneInputButton.setOnClickListener(view -> {
            runeInput = runeInputText.getText().toString();

            if (runeInput.equals("")) {
                runeCodeInputLayout.setErrorEnabled(true);
                runeCodeInputLayout.setError(getString(R.string.enter_rune_code));
            }
            else {
                if (super.isRuneInputInRunesList(runeInput)) {
                    if (super.hasRuneAlreadyBeenEntered(runeInput)) {
                        runeCodeInputLayout.setErrorEnabled(true);
                        runeCodeInputLayout.setError(getString(R.string.toast_rune_already_entered));
                    }
                    else {
                        runeCodeInputLayout.setErrorEnabled(false);
                        runeCodeInputLayout.setError(null);
                        super.handleSuccess(runeInput);
                    }
                } else {
                    runeCodeInputLayout.setErrorEnabled(true);
                    runeCodeInputLayout.setError(getString(R.string.toast_rune_enter_fail));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Read nfc tags to avoid new nfc tag scanned popup
        HandleNFCTagScan.ReadNFCTags(this);
    }
}