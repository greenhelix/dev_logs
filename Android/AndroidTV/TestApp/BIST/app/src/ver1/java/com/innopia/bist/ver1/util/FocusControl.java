package com.innopia.bist.ver1.util;

import android.view.View;
//import com.innopia.bist.R;

public class FocusControl {

    /**
     * Sets or removes focus properties for the given views based on the 'enable' flag.
     *
     * @param enable If true, makes views focusable and applies the focus border.
     *               If false, removes focusability and the background drawable.
     * @param views  A variable number of View objects to modify.
     */
    public static void setFocusable(boolean enable, View... views) {
        if (views == null) {
            return;
        }

        for (View view : views) {
            if (view != null) {
                if (enable) {
                    // Enable focus properties and apply the visual border.
                    view.setFocusable(true);
                    view.setFocusableInTouchMode(true);
                    view.setBackgroundResource(R.drawable.focusable_item_border);
                } else {
                    // Disable focus properties and remove the background to hide the border.
                    view.setFocusable(false);
                    view.setFocusableInTouchMode(false);
                    view.setBackground(null); // Remove the custom drawable.
                }
            }
        }
    }
}
