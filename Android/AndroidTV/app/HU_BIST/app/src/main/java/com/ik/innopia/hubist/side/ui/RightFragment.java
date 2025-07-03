package com.ik.innopia.hubist.side.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.ik.innopia.hubist.R;

// 불필요한 코드를 모두 정리한 깔끔한 버전
public class RightFragment extends Fragment {

    public RightFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // fragment_right.xml 레이아웃을 화면에 표시합니다.
        return inflater.inflate(R.layout.fragment_right, container, false);
    }
}
