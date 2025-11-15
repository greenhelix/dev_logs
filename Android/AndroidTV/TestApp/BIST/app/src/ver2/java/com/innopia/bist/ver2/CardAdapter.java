package com.innopia.bist.ver2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.innopia.bist.ver2.R;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<CardItem> items;

    public CardAdapter(List<CardItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem item = items.get(position);
        holder.cardText.setText(item.getText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView cardText;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardText = itemView.findViewById(R.id.card_text);
        }
    }
}
