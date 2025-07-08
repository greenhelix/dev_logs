package com.ik.innopia.hubist.side.ui;

import android.content.Context; // 추가
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
// import androidx.recyclerview.widget.LinearLayoutManager; // 지금은 사용하지 않으므로 주석 처리 가능

import com.ik.innopia.hubist.databinding.FragmentLeftBinding;

public class LeftFragment extends Fragment {

    // 1. 프래그먼트와 액티비티 간의 통신을 위한 인터페이스 정의
    public interface OnTestMenuClickListener {
        void onTestMenuSelected(String testName);
    }

    private OnTestMenuClickListener menuClickListener;
    private FragmentLeftBinding binding;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 2. 프래그먼트가 액티비티에 붙을 때, 리스너를 구현했는지 확인하고 연결합니다.
        if (context instanceof OnTestMenuClickListener) {
            menuClickListener = (OnTestMenuClickListener) context;
        } else {
            // 구현하지 않았다면 오류를 발생시켜 개발자가 알 수 있게 합니다.
            throw new RuntimeException(context.toString()
                    + " must implement OnMenuClickListener");
        }
    }

    public LeftFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLeftBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 버튼 클릭 리스너를 설정합니다.
        setupButtonClickListeners();

        // RecyclerView 관련 코드는 지금 필요 없으므로 주석 처리하거나 나중을 위해 남겨둘 수 있습니다.
        // showButtonView();
        // setupRecyclerView();
    }

    private void setupButtonClickListeners() {
        binding.wifiTestButton.setOnClickListener(v -> {
            if (menuClickListener != null) {
                // "WIFI"라는 고유한 문자열을 전달
                menuClickListener.onTestMenuSelected("WIFI");
            }
        });
        // 예: 블루투스 버튼 추가 시
        // binding.btTestButton.setOnClickListener(v -> {
        //     if (menuClickListener != null) {
        //         menuClickListener.onTestMenuSelected("BLUETOOTH");
        //     }
        // });
    }

    // 아래 메서드들은 지금 당장 필요하지 않지만, 확장성을 위해 남겨둘 수 있습니다.
    public void showButtonView() {
        binding.buttonContainer.setVisibility(View.VISIBLE);
        binding.menuItemList.setVisibility(View.GONE);
    }

    public void showRecyclerView() {
        binding.buttonContainer.setVisibility(View.GONE);
        binding.menuItemList.setVisibility(View.VISIBLE);
    }

    private void setupRecyclerView() {
        // RecyclerView 관련 설정 코드
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // 4. 프래그먼트가 액티비티에서 떨어질 때, 리스너 참조를 해제하여 메모리 누수를 방지합니다.
        menuClickListener = null;
    }
}
