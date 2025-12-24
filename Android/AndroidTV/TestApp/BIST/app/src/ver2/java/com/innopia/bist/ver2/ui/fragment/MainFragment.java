package com.innopia.bist.ver2.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.innopia.bist.ver2.ui.adapter.CardAdapter;
import com.innopia.bist.ver2.data.model.CardItem;
import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.ui.fragment.test.*;
import com.innopia.bist.ver2.util.AutoFitGridLayoutManager;
import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";

    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private AutoFitGridLayoutManager layoutManager;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        setupRecyclerView(root);

        return root;
    }

    private void setupRecyclerView(View root) {
        recyclerView = root.findViewById(R.id.recycler_view);

        int columnWidthPx = getResources().getDimensionPixelSize(R.dimen.column_width);
        layoutManager = new AutoFitGridLayoutManager(requireContext(), columnWidthPx);
        recyclerView.setLayoutManager(layoutManager);

        List<CardItem> cardItems = generateSampleData();

        cardAdapter = new CardAdapter(cardItems, new CardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(CardItem item, int position) {
                Log.d(TAG, "Card clicked: " + item.getText() + " at position " + position);
                handleCardClick(item);
            }
        });
        recyclerView.setAdapter(cardAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private List<CardItem> generateSampleData() {
        List<CardItem> cardItems = new ArrayList<>();

        // 실제 구현된 테스트들
        cardItems.add(new CardItem("Display Test"));      // Test 0
        cardItems.add(new CardItem("Memory Test"));       // Test 1
        cardItems.add(new CardItem("CPU Test"));          // Test 2
        cardItems.add(new CardItem("Network Test"));      // Test 3
        cardItems.add(new CardItem("Storage Test"));      // Test 4
        cardItems.add(new CardItem("Bluetooth Test"));    // Test 5
        cardItems.add(new CardItem("WiFi Test"));         // Test 6
        cardItems.add(new CardItem("Process Monitor"));   // Test 7
        cardItems.add(new CardItem("RCU Button Test"));   // Test 8
        cardItems.add(new CardItem("Video Test"));        // Test 9
        cardItems.add(new CardItem("Temperature Test"));  // Test 10
        cardItems.add(new CardItem("Mic Test"));          // Test 11

        // 나머지는 향후 구현 예정
        for (int i = 13; i <= 28; i++) {
            cardItems.add(new CardItem("Test " + i));
        }

        return cardItems;
    }

    private void handleCardClick(CardItem item) {
        String itemName = item.getText();
        Fragment targetFragment = null;

        switch (itemName) {
            case "Mic Test":
                targetFragment = MicTestFragment.newInstance();
                break;
            case "Display Test":
                targetFragment = Test1Fragment.newInstance();
                break;

            case "Memory Test":
                targetFragment = MemoryTestFragment.newInstance();
                break;

            case "CPU Test":
                targetFragment = CpuTestFragment.newInstance();
                break;

            case "Network Test":
                targetFragment = Test3Fragment.newInstance();
                break;

            case "Storage Test":
                targetFragment = StorageTestFragment.newInstance();
                break;

            case "Bluetooth Test":
                targetFragment = BluetoothTestFragment.newInstance();
                break;

            case "WiFi Test":
                targetFragment = WifiTestFragment.newInstance();
                break;

            case "Process Monitor":
                targetFragment = ProcessMonitorFragment.newInstance();
                break;

            case "RCU Button Test":
                targetFragment = RcuButtonTestFragment.newInstance();
                break;

            case "Video Test":
                targetFragment = VideoTestFragment.newInstance();
                break;

            case "Temperature Test":
                targetFragment = TemperatureTestFragment.newInstance();
                break;

            default:
                Log.d(TAG, "Card " + itemName + " clicked - not implemented yet");
                return;
        }

        if (targetFragment != null) {
            navigateToTestFragment(targetFragment);
        }
    }

    private void navigateToTestFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Log.d(TAG, "Navigated to TestFragment");
    }
}
