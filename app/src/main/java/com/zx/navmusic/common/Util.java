package com.zx.navmusic.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;

import com.zx.navmusic.MainActivity;
import com.zx.navmusic.PlaybackActivity;
import com.zx.navmusic.R;
import com.zx.navmusic.service.MusicPlayState;

public class Util {

    public static void parsePlayState(MusicPlayState t) {
        String name = t.name;

        if (name.contains("《") && name.contains("》")) {
            int l = name.indexOf('《');
            int r = name.indexOf('》');
            if (l < r) {
                String artist = name.substring(0, l).trim();
                String title = name.substring(l + 1, r).trim();

                t.name = title;
                t.artist = artist;
            }
        }

        if (name.contains("-")) {
            String[] parts = name.split("-");
            if (parts.length >= 2) {
                String artist = parts[0].trim();
                String part1 = parts[1];
                if (part1.contains(".")) {
                    part1 = part1.substring(0, part1.lastIndexOf('.'));
                }
                String title = part1.trim();

                t.name = title;
                t.artist = artist;
            }
        }
    }


    public static Intent intentPlaying(Context ctx) {
        return new Intent(ctx, PlaybackActivity.class);
    }

    public static void navigatePlaying(Activity ctx) {
        ctx.startActivity(new Intent(ctx, PlaybackActivity.class));
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

    public static Uri getFileUri(Context ctx, String filename) {
        return LocalAudioStore.find(ctx, filename);
    }
}
