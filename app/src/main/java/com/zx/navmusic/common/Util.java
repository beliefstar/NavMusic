package com.zx.navmusic.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;

import com.zx.navmusic.MainActivity;
import com.zx.navmusic.PlaybackActivity;
import com.zx.navmusic.PlaybackNewActivity;
import com.zx.navmusic.R;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.MusicName;
import com.zx.navmusic.config.ConfigCenter;

import java.util.List;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;

public class Util {

    public static void parseMusicItem(MusicItem t) {
        String name = t.name;

        if (StrUtil.isBlank(name)) {
            return;
        }
        if (StrUtil.isNotBlank(t.artist) && StrUtil.isNotBlank(t.ext)) {
            return;
        }

        MusicName musicName = parseMusicName(name);
        t.name = musicName.name;
        t.artist = musicName.artist;
        t.ext = musicName.ext;
    }

    public static MusicName parseMusicName(String name) {
        MusicName mn = new MusicName();
        if (StrUtil.isBlank(name)) {
            return mn;
        }
        String ext = extName(name);
        String main = mainName(name);

        Pair<String, String> pair = parseMusicRawName(main);
        mn.name = pair.getKey();
        mn.artist = pair.getValue();
        mn.ext = ext;
        return mn;
    }

    public static Pair<String, String> parseMusicRawName(String name) {
        if (name.contains("《") && name.contains("》")) {
            int l = name.indexOf('《');
            int r = name.indexOf('》');
            if (l < r) {
                String artist = name.substring(0, l).trim();
                String title = name.substring(l + 1, r).trim();

                return Pair.of(title, artist);
            }
        }

        if (name.contains("-")) {
            List<String> parts = StrUtil.split(name, "-", true, true);
            if (parts.size() >= 2) {
                String artist = parts.get(0).trim();
                String part1 = parts.get(1);
                String title = part1.trim();

                return Pair.of(title, artist);
            }
        }
        return Pair.of(name, "");
    }

    public static Intent intentPlaying(Context ctx) {
        return new Intent(ctx, ConfigCenter.isUseNewPlaybackUi()
                ? PlaybackNewActivity.class
                : PlaybackActivity.class);
    }

    public static void navigatePlaying(Activity ctx) {
        ctx.startActivity(new Intent(ctx, ConfigCenter.isUseNewPlaybackUi()
                ? PlaybackNewActivity.class
                : PlaybackActivity.class));
        ctx.overridePendingTransition(R.anim.slide_in_bottom, 0);
    }

    public static void navigateMain(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ctx.startActivity(intent);
    }

    public static int convertSpToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int musicRankRes(int score) {
        if (score > 50) {
            return R.drawable.ic_crown;
        }
        if (score > 30) {
            return R.drawable.ic_sun;
        }
        if (score > 15) {
            return R.drawable.ic_moon;
        }
        if (score > 5) {
            return R.drawable.ic_star;
        }
        return 0;
    }


    public static String extName(String name) {
        int i = name.lastIndexOf(".");
        if (i == -1 || i == name.length() - 1) {
            return name;
        }
        return name.substring(i + 1);
    }

    public static String mainName(String name) {
        int i = name.lastIndexOf(".");
        if (i == -1) {
            return name;
        }
        return name.substring(0, i);
    }

    public static String storeName(String name) {
        return name.replace("/", "_");
    }
}
