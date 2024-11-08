package com.zx.navmusic.util;

import android.content.Context;

public class Utils {


    public static int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (density * dp + 0.5f);
    }
}
