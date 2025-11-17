package com.innopia.bist.ver2.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class OverscanHelper {

    // 5% 마진 계산 (Google 권장) 에 따라 화면이 잘리는 것을 방지하기 위해 이 Helper 코드를 사용한다.
    // 화면 크기를 계산하여 activity에 해당 helper를 적용하고 create되도록 하는 원리이다.

    // 사용법
    // 화면을 그리는 부분에 OverscanHelper.applyOverscanMargins(this, rootView) 등을 하여 적용한다.
    // 이 helper를 적용한 경우 xml 레이아웃 파일에서는 따로 margin을 설정할 필요가 없다.
    // 다른 함수는 int[] 값으로 리턴해주고 단위가 바뀌경우 값을 보여주는 함수이다.

    private static final float MARGIN_PERCENT = 0.05f;

    /**
     * 화면 크기의 5% 마진을 계산하여 적용
     */
    public static void applyOverscanMargins(Activity activity, View rootView) {
        // 화면 크기 가져오기
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // 5% 마진 계산
        int horizontalMargin = (int) (screenWidth * MARGIN_PERCENT);
        int verticalMargin = (int) (screenHeight * MARGIN_PERCENT);

        Log.d("OverScanHelper", "horizon margin : "+ horizontalMargin + " vertical margin : "+ verticalMargin);
        Log.d("OverScanHelper","calculateOverscanMarginsDp : " + calculateOverscanMarginsDp(rootView.getContext()).toString());
        Log.d("OverScanHelper","calculateOverscanMarginsPx : " + calculateOverscanMarginsPx(rootView.getContext()).toString());

        // 마진 적용
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) rootView.getLayoutParams();
        params.setMargins(horizontalMargin, verticalMargin,
                horizontalMargin, verticalMargin);
        rootView.setLayoutParams(params);
    }

    /**
     * 화면 크기의 5% 마진을 계산 (dp 단위 반환)
     */
    public static int[] calculateOverscanMarginsDp(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        // 픽셀을 dp로 변환
        int screenWidthDp = (int) (size.x / metrics.density);
        int screenHeightDp = (int) (size.y / metrics.density);

        // 5% 계산
        int horizontalMarginDp = (int) (screenWidthDp * MARGIN_PERCENT);
        int verticalMarginDp = (int) (screenHeightDp * MARGIN_PERCENT);

        return new int[]{horizontalMarginDp, verticalMarginDp};
    }

    /**
     * 화면 크기의 5% 마진을 계산 (픽셀 단위 반환)
     */
    public static int[] calculateOverscanMarginsPx(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int horizontalMarginPx = (int) (size.x * MARGIN_PERCENT);
        int verticalMarginPx = (int) (size.y * MARGIN_PERCENT);

        return new int[]{horizontalMarginPx, verticalMarginPx};
    }

    /**
     * dp를 픽셀로 변환
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 픽셀을 dp로 변환
     */
    public static int pxToDp(Context context, int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (px / density + 0.5f);
    }
}
