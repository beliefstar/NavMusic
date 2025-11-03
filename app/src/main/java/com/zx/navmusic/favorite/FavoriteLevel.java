package com.zx.navmusic.favorite;

import com.zx.navmusic.R;

import java.util.List;

public class FavoriteLevel {

    private final static List<LevelInfo> LEVEL_INFOS = List.of(
            new LevelInfo(0, 0, 0),
            new LevelInfo(1, R.drawable.ic_star, 5),
            new LevelInfo(2, R.drawable.ic_moon, 15),
            new LevelInfo(3, R.drawable.ic_sun, 30),
            new LevelInfo(4, R.drawable.ic_crown, 50)
    );

    public static class LevelInfo {
        public final int level;
        public final int resId;
        public final int step;

        public LevelInfo(int level, int resId, int step) {
            this.level = level;
            this.resId = resId;
            this.step = step;
        }
    }

    public static FavoriteLevel.LevelInfo fromScore(int score) {
        for (int i = LEVEL_INFOS.size() - 1; i >= 0; i--) {
            LevelInfo info = LEVEL_INFOS.get(i);
            if (score >= info.step) {
                return info;
            }
        }
        return LEVEL_INFOS.get(0);
    }

}
