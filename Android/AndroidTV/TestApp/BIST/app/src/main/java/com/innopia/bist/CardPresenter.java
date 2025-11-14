package com.innopia.bist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.innopia.bist.R;
import com.innopia.bist.ContentItem;

public class CardPresenter extends Presenter {

    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 300;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = CARD_WIDTH;
        params.height = CARD_HEIGHT;
        view.setLayoutParams(params);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ContentItem contentItem = (ContentItem) item;
        View view = viewHolder.view;

        ImageView imageView = view.findViewById(R.id.card_image);
        TextView titleView = view.findViewById(R.id.card_title);
        TextView subtitleView = view.findViewById(R.id.card_subtitle);

        imageView.setImageResource(contentItem.getImageResourceId());
        titleView.setText(contentItem.getTitle());
        subtitleView.setText(contentItem.getSubtitle());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // 뷰 정리
    }
}
