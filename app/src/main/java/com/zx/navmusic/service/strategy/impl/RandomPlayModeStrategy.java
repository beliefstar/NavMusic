package com.zx.navmusic.service.strategy.impl;

import com.zx.navmusic.common.App;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.config.ConfigCenter;
import com.zx.navmusic.favorite.FavoriteLevel;
import com.zx.navmusic.service.strategy.AbsPlayModeStrategy;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomPlayModeStrategy extends AbsPlayModeStrategy {

    private final List<Integer> list = new ArrayList<>();
    private int musicPos = -1;

    public RandomPlayModeStrategy() {
        super(PlayModeStrategy.RANDOM);
    }

    protected synchronized void init() {
        if (getMusicProvider() == null) {
            return;
        }
        if (list.size() != getMusicProvider().count()) {
            freshList();
        }
        if (!list.isEmpty()) {
            if (musicPos == -1) {
                musicPos = list.get(0);
                position = 0;
            }
            else if (!list.get(position).equals(musicPos)) {
                resetPos(musicPos, false);
            }
        }
        super.init();
    }

    private void freshList() {
        list.clear();
        int step = ConfigCenter.getFavoriteStep();

        List<MusicItem> items = getMusicProvider().getList();
        for (int i = 0; i < items.size(); i++) {
            list.add(i);

            MusicItem item = items.get(i);
            FavoriteLevel.LevelInfo level = FavoriteLevel.fromScore(item.score);
            int num = level.level * step;

            for (int t = num; t > 0; t--) {
                list.add(i);
            }
        }

        Collections.shuffle(list);

        App.log("[RandomPlay]re random list --> {}", list);
    }

    @Override
    public int getCurPos() {
        return list.isEmpty() ? 0 : musicPos;
    }

    @Override
    public void resetPos(int position, boolean refresh) {
        if (refresh) {
            freshList();
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(position)) {
                this.position = i;
                this.musicPos = list.get(i);
                return;
            }
        }
    }

    @Override
    public int previous() {
        position = position == 0 ? list.size() - 1 : position - 1;
        musicPos = list.get(position);
        return musicPos;
    }

    @Override
    public int next() {
        position = (position + 1) % list.size();
        musicPos = list.get(position);
        return musicPos;
    }

    @Override
    public int peekPrevious() {
        return list.get(position == 0 ? list.size() - 1 : position - 1);
    }

    @Override
    public int peekNext() {
        return list.get((position + 1) % list.size());
    }
}
