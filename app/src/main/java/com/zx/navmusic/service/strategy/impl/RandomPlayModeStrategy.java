package com.zx.navmusic.service.strategy.impl;

import com.zx.navmusic.service.strategy.PlayModeStrategy;
import com.zx.navmusic.service.strategy.AbsPlayModeStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomPlayModeStrategy extends AbsPlayModeStrategy {

    private final List<Integer> list = new ArrayList<>();

    public RandomPlayModeStrategy() {
        super(PlayModeStrategy.RANDOM);

        init();
        getMusicProvider().observeForever(lst -> init());
    }

    private synchronized void init() {
        list.clear();
        if (getMusicProvider().count() == 0) {
            return;
        }
        for (int i = 0; i < getMusicProvider().count(); i++) {
            list.add(i);
        }
        Collections.shuffle(list);

        if (position > list.size() - 1) {
            position = list.size() - 1;
        }
    }

    @Override
    public int getCurPos() {
        return list.isEmpty() ? 0 : list.get(position);
    }

    @Override
    public void resetPos(int position) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == position) {
                this.position = i;
            }
        }
    }

    @Override
    public int previous() {
        position = position == 0 ? list.size() - 1 : position - 1;
        return list.get(position);
    }

    @Override
    public int next() {
        position = (position + 1) % list.size();
        return list.get(position);
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
