package com.zx.navmusic.service.strategy.impl;

import com.zx.navmusic.common.App;
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
            App.log("[RandomPlay]re random list --> {}", list);
            list.clear();
            for (int i = 0; i < getMusicProvider().count(); i++) {
                list.add(i);
            }
            Collections.shuffle(list);
        }
        if (!list.isEmpty()) {
            if (musicPos == -1) {
                musicPos = list.get(0);
                position = 0;
                App.log("[RandomPlay]first init --> {}", musicPos);
            }
            else if (!list.get(position).equals(musicPos)) {
                App.log("[RandomPlay]resetPos --> {}", musicPos);
                resetPos(musicPos);
            }
        }
        super.init();
    }

    @Override
    public int getCurPos() {
        return list.isEmpty() ? 0 : musicPos;
    }

    @Override
    public void resetPos(int position) {
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
