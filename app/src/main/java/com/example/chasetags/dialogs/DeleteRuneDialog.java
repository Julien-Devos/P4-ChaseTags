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
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.chasetags.R;
import com.example.chasetags.utils.GlobalVariables;
import com.example.chasetags.utils.OnDeleteRuneListener;

public class DeleteRuneDialog extends DialogFragment {

    // UI items
    Button deleteButton;
    Button cancelButton;
    CheckBox dontShowCheckbox;
    Boolean gameRune;

    private OnDeleteRuneListener mListener;

    public void show(@NonNull FragmentManager manager, @Nullable String tag, boolean gameRune) {
        super.show(manager, tag);
        this.gameRune = gameRune;
    }

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

        if (gameRune) {
            TextView title = view.findViewById(R.id.title);
            TextView message = view.findViewById(R.id.message);
            title.setText(R.string.remove_rune_dialog);
            message.setText(R.string.remove_rune_message_dialog);
            deleteButton.setText(R.string.remove_button);
        }

        deleteButton.setOnClickListener(view1 -> {
            if (mListener != null) {
                mListener.onDeleteRune();
            }

            if (dontShowCheckbox.isChecked()) {
                SharedPreferences dialogPreference = view.getContext().getSharedPreferences(GlobalVariables.PREFS_preference_dialogs, 0);
                SharedPreferences.Editor editor = dialogPreference.edit();

                if (gameRune) {
                    editor.putBoolean(GlobalVariables.PREFS_showDialog_local, false);
                } else {
                    editor.putBoolean(GlobalVariables.PREFS_showDialog_global, false);
                }
                editor.apply();
            }

            dismiss();
        });

        cancelButton.setOnClickListener(view12 -> dismiss());

    }

    public void setOnDeleteRuneListener(OnDeleteRuneListener listener) {
        mListener = listener;
    }

}
