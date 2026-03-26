package com.zx.navmusic.config;

import android.content.Context;

import com.zx.navmusic.common.LocalStore;
import com.zx.navmusic.common.bean.ConfigDataBean;

import java.util.function.Consumer;

import cn.hutool.core.util.StrUtil;

public class ConfigCenter {
    public static final int THEME_SIMPLE = 0;
    public static final int THEME_TECH = 1;
    public static final int THEME_VIVID = 2;
    public static final int THEME_DARK = 3;

    public static ConfigCenter INSTANCE;

    public static final String DEFAULT_BBS_TOKEN = "B913yznE7cUXNjnWjIAJtIAHZdodup7D8iEnJmoP_2F3FUpn0h7FuSidh_2FYVUIUZ4bfjy3TSOnI0xstnhjTbfG9VJN56M_3D";

    public ConfigDataBean configData;

    public ConfigCenter(Context ctx) {
        init(ctx);
    }

    public static void create(Context ctx) {
        INSTANCE = new ConfigCenter(ctx);
    }

    public static void ensureCreated(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new ConfigCenter(ctx.getApplicationContext());
        }
    }

    private void init(Context ctx) {
        configData = LocalStore.loadConfigData(ctx);


        if (StrUtil.isBlank(configData.bbsToken)) {
            configData.bbsToken = DEFAULT_BBS_TOKEN;
        }

        if (configData.favoriteStep == null) {
            configData.favoriteStep = 1;
        }

        if (configData.useLocalMode == null) {
            configData.useLocalMode = false;
        }

        if (configData.favoriteSort == null) {
            configData.favoriteSort = true;
        }

        if (configData.bluetoothLyric == null) {
            configData.bluetoothLyric = false;
        }

        if (configData.themeType == null
                || configData.themeType < THEME_SIMPLE
                || configData.themeType > THEME_DARK) {
            configData.themeType = THEME_SIMPLE;
        }
    }

    public static void change(Consumer<ConfigDataBean> changer, Context ctx) {
        changer.accept(INSTANCE.configData);
        LocalStore.flushConfig(ctx, INSTANCE.configData);
    }

    public static String getBbsToken() {
        return INSTANCE.configData.bbsToken;
    }

    public static boolean isUseLocalMode() {
        return INSTANCE.configData.useLocalMode;
    }

    public static boolean isUseNewPlaybackUi() {
        return INSTANCE.configData.useNewPlaybackUi;
    }

    public static boolean isFavoriteSort() {
        return INSTANCE.configData.favoriteSort;
    }

    public static int getFavoriteStep() {
        return INSTANCE.configData.favoriteStep;
    }

    public static boolean isBluetoothLyric() {
        return INSTANCE.configData.bluetoothLyric;
    }

    public static int getThemeType() {
        return INSTANCE.configData.themeType;
    }
}
