package com.example.chasetags.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.chasetags.R;
import com.example.chasetags.player.Player;

import java.util.ArrayList;

public class PlayerAdapter extends ArrayAdapter<Player>{

    private final ArrayList<Player> playerList;
    private final LayoutInflater layoutInflater;
    private final Integer runesTotal;

    Context context;

    public PlayerAdapter(ArrayList<Player> playerList, Context context, Integer runesTotal, Context ctxt) {
        super(context, R.layout.player_view, playerList);
        this.playerList = playerList;
        this.runesTotal = runesTotal;
        this.context = ctxt;
        layoutInflater= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if(view==null) {
            view = layoutInflater.inflate(R.layout.player_view,parent,false);
        }

        Player currentPlayer = playerList.get(position);

        TextView playerName = (TextView) view.findViewById(R.id.player_name_adapter);
        TextView progress = (TextView) view.findViewById(R.id.player_progress_adapter);
        TextView score = (TextView) view.findViewById(R.id.player_score_adapter);

        playerName.setText(currentPlayer.getName());
        score.setText(String.format("%dpts", currentPlayer.getScore()));
        progress.setText(String.format("(%d/%d)", currentPlayer.getRunesCollected(), runesTotal));

        return view;
    }
}
