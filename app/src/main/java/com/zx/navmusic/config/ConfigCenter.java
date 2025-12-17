package com.zx.navmusic.config;

import android.content.Context;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.common.LocalStore;
import com.zx.navmusic.common.bean.ConfigDataBean;

import java.util.function.Consumer;

import cn.hutool.core.util.StrUtil;

public class ConfigCenter {

    public static ConfigCenter INSTANCE;

    public static final String DEFAULT_BBS_TOKEN = "B913yznE7cUXNjnWjIAJtIAHZdodup7D8iEnJmoP_2F3FUpn0h7FuSidh_2FYVUIUZ4bfjy3TSOnI0xstnhjTbfG9VJN56M_3D";

    public ConfigDataBean configData;

    public ConfigCenter(Context ctx) {
        init(ctx);
    }

    public static void create(Context ctx) {
        INSTANCE = new ConfigCenter(ctx);
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
    }


    public static void change(Consumer<ConfigDataBean> changer) {
        change(changer, MusicService.INSTANCE);
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

    public static int getFavoriteStep() {
        return INSTANCE.configData.favoriteStep;
    }
}
