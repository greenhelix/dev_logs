package com.innopia.bist;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListFragment extends Fragment {

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 리스트 레이아웃 인플레이트
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        // TODO: 리스트 데이터 로드 및 어댑터 설정

        return view;
    }
}
