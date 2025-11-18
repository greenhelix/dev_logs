package com.innopia.bist.ver2.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.data.model.CardItem;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private static final String TAG = "CardAdapter";
    private List<CardItem> items;
    private OnCardClickListener clickListener;

    // 포커스 애니메이션 설정
    private static final float FOCUS_SCALE = 1.08f;       // 8% 확대 (10%에서 줄임)
    private static final float NORMAL_SCALE = 1.0f;       // 원래 크기
    private static final float FOCUS_ELEVATION = 24f;     // 포커스 시 그림자 높이
    private static final float NORMAL_ELEVATION = 6f;     // 일반 그림자 높이
    private static final int ANIMATION_DURATION = 150;    // 애니메이션 시간 (밀리초)

    // 클릭 리스너 인터페이스
    public interface OnCardClickListener {
        void onCardClick(CardItem item, int position);
    }
    public CardAdapter(List<CardItem> items, OnCardClickListener listener) {
        this.items = items;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);

        CardView cardView = view.findViewById(R.id.card_view);

        if (cardView != null) {
            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);

            cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    animateCard(cardView, hasFocus);
                }
            });
        }

        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem item = items.get(position);

        // ⭐ 타이틀 설정
        if (holder.cardTitle != null) {
            holder.cardTitle.setText(item.getText());
            Log.d(TAG, "Set title: " + item.getText());
        }

        // ⭐ 서브타이틀 설정
        if (holder.cardSubtitle != null) {
            holder.cardSubtitle.setText("Item #" + (position + 1));
            Log.d(TAG, "Set subtitle: Item #" + (position + 1));
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCardClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardTitle;      // ⭐ 타이틀용
        TextView cardSubtitle;   // ⭐ 서브타이틀용
        CardView cardView;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_view);

            // ⭐ 두 개의 TextView 모두 찾기
            if (cardView != null) {
                // CardView 안에서 찾기
                cardTitle = cardView.findViewById(R.id.card_title);
                cardSubtitle = cardView.findViewById(R.id.card_subtitle);

                Log.d(TAG, "CardView found, cardTitle: " + (cardTitle != null)
                        + ", cardSubtitle: " + (cardSubtitle != null));
            } else {
                // CardView가 루트일 경우
                cardTitle = itemView.findViewById(R.id.card_title);
                cardSubtitle = itemView.findViewById(R.id.card_subtitle);

                Log.d(TAG, "CardView is root, cardTitle: " + (cardTitle != null)
                        + ", cardSubtitle: " + (cardSubtitle != null));
            }
        }
    }

    /**
     * 포커스 시 카드 애니메이션
     */
    private void animateCard(CardView cardView, boolean hasFocus) {
        float targetScale = hasFocus ? FOCUS_SCALE : NORMAL_SCALE;
        float targetElevation = hasFocus ? FOCUS_ELEVATION : NORMAL_ELEVATION;

        // 부드러운 애니메이션을 위한 Interpolator
        DecelerateInterpolator interpolator = new DecelerateInterpolator();

        // Scale 애니메이션
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardView, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardView, "scaleY", targetScale);
        ObjectAnimator elevation = ObjectAnimator.ofFloat(cardView, "cardElevation", targetElevation);

        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        elevation.setInterpolator(interpolator);

        scaleX.setDuration(ANIMATION_DURATION);
        scaleY.setDuration(ANIMATION_DURATION);
        elevation.setDuration(ANIMATION_DURATION);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, elevation);
        animatorSet.start();
    }
}
