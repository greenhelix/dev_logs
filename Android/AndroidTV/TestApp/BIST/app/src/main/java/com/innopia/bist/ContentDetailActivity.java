package com.innopia.bist;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.innopia.bist.R;

public class ContentDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_detail);

        // Intent에서 데이터 가져오기
        String title = getIntent().getStringExtra("content_title");
        String subtitle = getIntent().getStringExtra("content_subtitle");

        // 뷰 초기화
        ImageView imageView = findViewById(R.id.detail_image);
        TextView titleView = findViewById(R.id.detail_title);
        TextView subtitleView = findViewById(R.id.detail_subtitle);
        TextView descriptionView = findViewById(R.id.detail_description);

        // 데이터 설정
        titleView.setText(title);
        subtitleView.setText(subtitle);
        descriptionView.setText("Detailed information about " + title);

        // 이미지는 실제 구현시 Glide 등으로 로드
        imageView.setImageResource(R.drawable.default_background);
    }
}
