package com.zx.navmusic.service.strategy.impl;

import com.zx.navmusic.service.strategy.PlayModeStrategy;
import com.zx.navmusic.service.strategy.AbsPlayModeStrategy;

public class SinglePlayModeStrategy extends AbsPlayModeStrategy {

    public SinglePlayModeStrategy() {
        super(PlayModeStrategy.SINGLE);
    }

    @Override
    public int getCurPos() {
        return position;
    }

    @Override
    public void resetPos(int position) {
        this.position = position;
    }

    @Override
    public int previous() {
        return position;
    }

    @Override
    public int next() {
        return position;
    }

    @Override
    public int peekPrevious() {
        return position;
    }

    @Override
    public int peekNext() {
        return position;
    }
}
