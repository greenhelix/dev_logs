package com.innopia.bist;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class StatDetailActivity extends Activity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stat_detail);

        // Intent에서 데이터 가져오기
        String statTitle = getIntent().getStringExtra("stat_title");
        String statValue = getIntent().getStringExtra("stat_value");

        // 뷰 초기화
        ImageView iconView = findViewById(R.id.stat_detail_icon);
        TextView titleView = findViewById(R.id.stat_detail_title);
        TextView valueView = findViewById(R.id.stat_detail_value);
        TextView descriptionView = findViewById(R.id.stat_detail_description);
        lineChart = findViewById(R.id.stat_detail_chart);

        // 데이터 설정
        titleView.setText(statTitle);
        valueView.setText(statValue);
        descriptionView.setText("Detailed analysis of " + statTitle);

        // 차트 초기화
        setupChart();
    }

    private void setupChart() {
        // 샘플 데이터 생성
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 40));
        entries.add(new Entry(1, 55));
        entries.add(new Entry(2, 45));
        entries.add(new Entry(3, 65));
        entries.add(new Entry(4, 70));
        entries.add(new Entry(5, 85));

        LineDataSet dataSet = new LineDataSet(entries, "Monthly Trend");
        dataSet.setColor(getColor(R.color.brand_color));
        dataSet.setValueTextColor(getColor(R.color.text_primary));
        dataSet.setLineWidth(3f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate(); // 차트 새로고침
    }
}
