package com.zx.navmusic.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;

public class UIFragment {

    public static void initView(Context context, View root) {
        if (context == null) {
            return;
        }
        try (TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize })) {
            int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
            root.setPadding(0, 0, 0, mActionBarSize);
        }
    }
}
