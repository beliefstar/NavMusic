package com.zx.navmusic.common;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import cn.hutool.core.util.StrUtil;

public class App {

    public static String App_Name = "NavMusic";

    public static FragmentActivity MainActivity;

    public static final String MUSIC_ID = "music_id";

    public static void log(String msg, Object... args) {
        String s = StrUtil.format(msg, args);
        Log.d(App_Name, s);
    }
    public static void debug(String msg, Object... args) {
        String s = StrUtil.format("debug - " + msg, args);
        Log.d(App_Name, s);
    }

    public static void toast(String msg, Object... args) {
        toast(MainActivity, msg, args);
    }
    public static void toast(Context context, String msg, Object... args) {
        if (context != null) {
            String s = StrUtil.format(msg, args);
            context.getMainExecutor().execute(() -> {
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
