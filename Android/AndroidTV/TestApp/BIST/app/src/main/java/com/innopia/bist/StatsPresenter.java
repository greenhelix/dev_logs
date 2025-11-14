package com.innopia.bist;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.innopia.bist.R;
import com.innopia.bist.StatItem;

public class StatsPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stats_card, parent, false);

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        StatItem statItem = (StatItem) item;
        View view = viewHolder.view;

        ImageView iconView = view.findViewById(R.id.stats_icon);
        TextView labelView = view.findViewById(R.id.stats_label);
        TextView valueView = view.findViewById(R.id.stats_value);
        TextView changeView = view.findViewById(R.id.stats_change);

        iconView.setImageResource(statItem.getIconResourceId());
        labelView.setText(statItem.getLabel());
        valueView.setText(statItem.getValue());
        changeView.setText(statItem.getChange());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // 뷰 정리
    }
}

