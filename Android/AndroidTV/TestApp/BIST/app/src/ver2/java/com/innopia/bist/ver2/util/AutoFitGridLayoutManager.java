package com.innopia.bist.ver2.util;

import android.content.Context;
import android.util.Log;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AutoFitGridLayoutManager extends GridLayoutManager {

    private static final String TAG = "AutoFitGridLayout";

    private int columnWidth;
    private boolean isColumnWidthChanged = true;
    private int lastWidth;
    private int lastHeight;

    public AutoFitGridLayoutManager(Context context, int columnWidth) {
        super(context, 1);
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    public AutoFitGridLayoutManager(Context context, int columnWidth, int orientation, boolean reverseLayout) {
        super(context, 1, orientation, reverseLayout);
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    private int checkedColumnWidth(Context context, int columnWidth) {
        if (columnWidth <= 0) {
            columnWidth = (int) (200 * context.getResources().getDisplayMetrics().density);
        }
        return columnWidth;
    }

    public void setColumnWidth(int newColumnWidth) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth;
            isColumnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int width = getWidth();
        int height = getHeight();

        if (columnWidth > 0 && width > 0 && height > 0
                && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {

            // 화면 폭에서 패딩 제외
            int totalSpace = width - getPaddingRight() - getPaddingLeft();

            // 열 개수 계산
            int spanCount = Math.max(1, totalSpace / columnWidth);

            Log.d(TAG, "Width: " + width + "px, Column Width: " + columnWidth + "px, Span Count: " + spanCount);

            setSpanCount(spanCount);
            isColumnWidthChanged = false;
        }

        lastWidth = width;
        lastHeight = height;
        super.onLayoutChildren(recycler, state);
    }
}
