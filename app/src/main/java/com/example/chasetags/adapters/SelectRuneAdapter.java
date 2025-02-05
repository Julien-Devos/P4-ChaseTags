package com.example.chasetags.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chasetags.R;
import com.example.chasetags.runes.Rune;
import com.example.chasetags.utils.GlobalVariables;

import java.util.ArrayList;

public class SelectRuneAdapter extends ArrayAdapter<Rune> implements View.OnClickListener {

    private final Context context;
    private final ArrayList<Rune> runesList;
    private final LayoutInflater layoutInflater;

    public SelectRuneAdapter(ArrayList<Rune> runesList, Context context) {
        super(context, R.layout.rune_checkbox_view, runesList);
        this.context = context;
        this.runesList = runesList;
        layoutInflater= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void onClick(View view) {
        int position = (int) view.getTag();
        Rune rune = getItem(position);

        rune.setChecked(!rune.isChecked());
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if(view==null) {
            view = layoutInflater.inflate(R.layout.rune_checkbox_view,parent,false);
        }

        TextView runeName = view.findViewById(R.id.name);
        CheckBox checkBox = view.findViewById(R.id.rune_checkbox);
        ImageView code = view.findViewById(R.id.rune_code_icon);
        ImageView nfc = view.findViewById(R.id.rune_NFC_icon);
        ImageView qr = view.findViewById(R.id.rune_QR_icon);
        TextView value = view.findViewById(R.id.value);
        TextView hint = view.findViewById(R.id.hint);
        TextView code_hint = view.findViewById(R.id.code_hint);
        TextView code_score = view.findViewById(R.id.code_score);
        TextView score = view.findViewById(R.id.score);

        Rune currentRune = runesList.get(position);
        runeName.setText(currentRune.getName());


        if (currentRune.getType().equals(GlobalVariables.Rune_code_object_type)) {
            code.setVisibility(View.VISIBLE);
            value.setVisibility(View.VISIBLE);
            code_hint.setVisibility(View.VISIBLE);
            hint.setVisibility(View.GONE);
            score.setVisibility(View.GONE);
            code_score.setVisibility(View.VISIBLE);
            value.setText(String.format(view.getContext().getString(R.string.rune_code), currentRune.getValue()));
            code_score.setText(String.format(view.getContext().getString(R.string.points),currentRune.getScore()));
            code_hint.setText(String.format(view.getContext().getString(R.string.rune_hint), currentRune.getLocalisationClue()));
        } else if (currentRune.getType().equals(GlobalVariables.Rune_nfc_object_type)) {
            nfc.setVisibility(View.VISIBLE);
            hint.setText(String.format(view.getContext().getString(R.string.rune_hint), currentRune.getLocalisationClue()));
            score.setText(String.format(view.getContext().getString(R.string.points),currentRune.getScore()));
        } else {
            qr.setVisibility(View.VISIBLE);
            hint.setText(String.format(view.getContext().getString(R.string.rune_hint), currentRune.getLocalisationClue()));
            score.setText(String.format(view.getContext().getString(R.string.points),currentRune.getScore()));
        }

        checkBox.setChecked(currentRune.isChecked());
        checkBox.setTag(position);
        checkBox.setOnClickListener(this);

        return view;
    }
}
