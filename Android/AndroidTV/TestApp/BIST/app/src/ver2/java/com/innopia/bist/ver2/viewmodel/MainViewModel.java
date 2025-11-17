package com.innopia.bist.ver2.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.innopia.bist.ver2.CardItem;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private static final String TAG = "MainViewModel";

    private final MutableLiveData<List<CardItem>> cardItems = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public MainViewModel() {
        loadCardItems();
        statusMessage.setValue("System Online");
    }

    public LiveData<List<CardItem>> getCardItems() {
        return cardItems;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    private void loadCardItems() {
        List<CardItem> items = new ArrayList<>();
        for (int i = 1; i <= 28; i++) {
            items.add(new CardItem("Test " + i));
        }
        cardItems.setValue(items);
    }

    public void refreshData() {
        loadCardItems();
        statusMessage.setValue("Data refreshed at " + System.currentTimeMillis());
    }
}
