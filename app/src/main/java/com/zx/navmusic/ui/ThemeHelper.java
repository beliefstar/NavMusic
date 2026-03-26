package com.zx.navmusic.ui;

import android.app.Activity;
import android.content.Context;

import com.zx.navmusic.R;
import com.zx.navmusic.config.ConfigCenter;

public class ThemeHelper {

    private ThemeHelper() {
    }

    public static void applyTheme(Activity activity) {
        ConfigCenter.ensureCreated(activity.getApplicationContext());
        activity.setTheme(resolveTheme(activity.getApplicationContext()));
    }

    public static int resolveTheme(Context context) {
        ConfigCenter.ensureCreated(context.getApplicationContext());
        int type = ConfigCenter.getThemeType();
        switch (type) {
            case ConfigCenter.THEME_TECH:
                return R.style.Theme_NavMusic_Tech;
            case ConfigCenter.THEME_VIVID:
                return R.style.Theme_NavMusic_Vivid;
            case ConfigCenter.THEME_DARK:
                return R.style.Theme_NavMusic_Dark;
            case ConfigCenter.THEME_SIMPLE:
            default:
                return R.style.Theme_NavMusic_Simple;
        }
    }
}
