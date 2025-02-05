package com.example.chasetags.dialogs;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.chasetags.R;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.OnEditGameRuneDialogListener;

public class EditGameRuneDialog extends DialogFragment {

    // UI items
    Button deleteButton;
    Button cancelButton;
    CheckBox dontShowCheckbox;

    private OnEditGameRuneDialogListener mListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view =  inflater.inflate(R.layout.dialog_with_checkbox, container, false);
        // Set transparent background and no title
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deleteButton = view.findViewById(R.id.delete_button);
        cancelButton = view.findViewById(R.id.cancel);
        dontShowCheckbox = view.findViewById(R.id.rune_checkbox);

        setEditGameRuneText(view);

        deleteButton.setOnClickListener(view1 -> {
            if (mListener != null) {
                mListener.onEditRune();
            }

            if (dontShowCheckbox.isChecked()) {
                SharedPreferences dialogPreference = view.getContext().getSharedPreferences(GlobalVariables.PREFS_preference_dialogs, 0);

                SharedPreferences.Editor editor = dialogPreference.edit();
                editor.putBoolean(GlobalVariables.PREFS_showDialog_editGameRune, false);
                editor.apply();
            }

            dismiss();
        });

        cancelButton.setOnClickListener(view12 -> {
            if (mListener != null) {
                mListener.onCancelEdit();
            }
        });

    }

    public void setOnEditRuneListener(OnEditGameRuneDialogListener listener) {
        mListener = listener;
    }

    private void setEditGameRuneText(View view) {
        TextView title = view.findViewById(R.id.title);
        TextView message = view.findViewById(R.id.message);

        title.setText(R.string.edit_rune);
        message.setText(R.string.edit_rune_dialog_message);
        deleteButton.setText(R.string.confirm_button);
    }

}
