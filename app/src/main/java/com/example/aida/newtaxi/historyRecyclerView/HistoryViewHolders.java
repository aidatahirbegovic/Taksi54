package com.example.aida.newtaxi.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.aida.newtaxi.HistorySingleActivity;
import com.example.aida.newtaxi.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView rideId;
    public TextView destination;
    public TextView time;

    public HistoryViewHolders(android.view.View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        rideId = itemView.findViewById(R.id.rideId);
        destination = itemView.findViewById(R.id.destination);
        time = itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);

    }

}
