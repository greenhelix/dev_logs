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
import com.innopia.bist.ver2.ui.fragment.test.Test1Fragment;
import com.innopia.bist.ver2.ui.fragment.test.Test2Fragment;
import com.innopia.bist.ver2.ui.fragment.test.Test3Fragment;
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

        // RecyclerView 초기화
        setupRecyclerView(root);

        return root;
    }

    private void setupRecyclerView(View root) {
        recyclerView = root.findViewById(R.id.recycler_view);

        // AutoFitGridLayoutManager 설정
        int columnWidthPx = getResources().getDimensionPixelSize(R.dimen.column_width);
        layoutManager = new AutoFitGridLayoutManager(requireContext(), columnWidthPx);
        recyclerView.setLayoutManager(layoutManager);

        // 샘플 데이터 생성
        List<CardItem> cardItems = generateSampleData();

        // 어댑터 설정 (클릭 리스너 포함)
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
        for (int i = 1; i <= 28; i++) {
            cardItems.add(new CardItem("Test " + i));
        }
        return cardItems;
    }

    /**
     * 카드 클릭 처리
     */
    private void handleCardClick(CardItem item) {
        String itemName = item.getText();
        Fragment targetFragment = null;

        switch (itemName) {
            case "Test 1":
                targetFragment = Test1Fragment.newInstance();
                navigateToTestFragment(targetFragment);
                break;

            case "Test 2":
                targetFragment = Test2Fragment.newInstance();
                navigateToTestFragment(targetFragment);
                break;

            case "Test 3":
                targetFragment = Test3Fragment.newInstance();
                navigateToTestFragment(targetFragment);
                break;

//            case "HDMI Test": // ⭐ HDMI 테스트 (나중에 추가)
//                targetFragment = HdmiTestFragment.newInstance();
//                navigateToTestFragment(targetFragment);
//                break;

            default:
                Log.d(TAG, "Card " + itemName + " clicked - not implemented yet");
                // TODO: 나머지 Test Fragment 추가 예정
                break;
        }
    }

    /**
     * TestFragment로 이동 Animation 효과 추가
     */
    private void navigateToTestFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,   // enter
                        R.anim.fade_out,  // exit
                        R.anim.fade_in,   // popEnter
                        R.anim.fade_out   // popExit
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Log.d(TAG, "Navigated to TestFragment");
    }
}
