package com.innopia.bist;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.innopia.bist.R;
import com.innopia.bist.ContentItem;
import com.innopia.bist.StatItem;
import com.innopia.bist.CardPresenter;
import com.innopia.bist.StatsPresenter;

import java.util.Timer;
import java.util.TimerTask;

public class TestMainFragment extends BrowseSupportFragment {

    private static final int BACKGROUND_UPDATE_DELAY = 300;

    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private Handler mHandler = new Handler();
    private BackgroundManager mBackgroundManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUIElements();
        loadRows();
        setupEventListeners();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepareBackgroundManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }
    }

    private void setupUIElements() {
        // 배지 또는 타이틀 설정
        setBadgeDrawable(getActivity().getResources()
                .getDrawable(R.drawable.app_logo, null));
        setTitle(getString(R.string.dashboard_title));

        // 헤더 상태 설정
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // 브랜드 컬러 설정
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.brand_color));

        // 검색 아이콘 색상
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(),
                R.color.search_color));
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = getResources()
                .getDrawable(R.drawable.default_background, null);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void loadRows() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // 통계 카드 행 추가
        ArrayObjectAdapter statsRowAdapter = new ArrayObjectAdapter(
                new StatsPresenter());
        statsRowAdapter.add(new StatItem("Total Revenue", "$125,000",
                "+12.5%", R.drawable.ic_revenue));
        statsRowAdapter.add(new StatItem("Active Users", "1,234",
                "+8.2%", R.drawable.ic_users));
        statsRowAdapter.add(new StatItem("Total Orders", "856",
                "+15.3%", R.drawable.ic_orders));
        statsRowAdapter.add(new StatItem("Growth Rate", "23.5%",
                "+3.2%", R.drawable.ic_growth));

        HeaderItem statsHeader = new HeaderItem(0, "Dashboard Statistics");
        mRowsAdapter.add(new ListRow(statsHeader, statsRowAdapter));

        // 최근 활동 행 추가
        ArrayObjectAdapter activityRowAdapter = new ArrayObjectAdapter(
                new CardPresenter());
        activityRowAdapter.add(new ContentItem("Recent Order",
                "Order #12345", R.drawable.img_order));
        activityRowAdapter.add(new ContentItem("New Customer",
                "John Doe", R.drawable.img_customer));
        activityRowAdapter.add(new ContentItem("Product Update",
                "Widget v2.0", R.drawable.img_product));
        activityRowAdapter.add(new ContentItem("New Review",
                "5 Stars", R.drawable.img_review));

        HeaderItem activityHeader = new HeaderItem(1, "Recent Activity");
        mRowsAdapter.add(new ListRow(activityHeader, activityRowAdapter));

        // 분석 차트 행 추가
        ArrayObjectAdapter chartsRowAdapter = new ArrayObjectAdapter(
                new CardPresenter());
        chartsRowAdapter.add(new ContentItem("Monthly Sales",
                "View Chart", R.drawable.img_chart_line));
        chartsRowAdapter.add(new ContentItem("Product Performance",
                "View Chart", R.drawable.img_chart_bar));
        chartsRowAdapter.add(new ContentItem("Market Share",
                "View Chart", R.drawable.img_chart_pie));

        HeaderItem chartsHeader = new HeaderItem(2, "Analytics");
        mRowsAdapter.add(new ListRow(chartsHeader, chartsRowAdapter));

        setAdapter(mRowsAdapter);
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof StatItem) {
                StatItem statItem = (StatItem) item;
                // 통계 상세 화면으로 이동
                Intent intent = new Intent(getActivity(), StatDetailActivity.class);
                intent.putExtra("stat_title", statItem.getLabel());
                intent.putExtra("stat_value", statItem.getValue());
                startActivity(intent);

            } else if (item instanceof ContentItem) {
                ContentItem contentItem = (ContentItem) item;
                // 콘텐츠 상세 화면으로 이동
                Intent intent = new Intent(getActivity(), ContentDetailActivity.class);
                intent.putExtra("content_title", contentItem.getTitle());
                intent.putExtra("content_subtitle", contentItem.getSubtitle());
                startActivity(intent);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof ContentItem) {
                ContentItem contentItem = (ContentItem) item;
                updateBackground(contentItem.getImageResourceId());
            }
        }
    }

    private void updateBackground(int resourceId) {
        // 배경 업데이트 딜레이 타이머
        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }

        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(resourceId),
                BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {
        private int mResourceId;

        public UpdateBackgroundTask(int resourceId) {
            mResourceId = resourceId;
        }

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mResourceId != 0) {
                        Drawable drawable = getResources()
                                .getDrawable(mResourceId, null);
                        mBackgroundManager.setDrawable(drawable);
                    } else {
                        mBackgroundManager.setDrawable(mDefaultBackground);
                    }
                }
            });
        }
    }
}
