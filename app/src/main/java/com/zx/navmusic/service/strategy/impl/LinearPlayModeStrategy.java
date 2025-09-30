package com.zx.navmusic.service.strategy.impl;

import com.zx.navmusic.service.strategy.PlayModeStrategy;
import com.zx.navmusic.service.strategy.AbsPlayModeStrategy;

public class LinearPlayModeStrategy extends AbsPlayModeStrategy {

    public LinearPlayModeStrategy() {
        super(PlayModeStrategy.LINEAR);
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
        position = peekPrevious();
        return position;
    }

    @Override
    public int next() {
        position = peekNext();
        return position;
    }

    @Override
    public int peekPrevious() {
        return position == 0 ? getMusicProvider().count() - 1 : position - 1;
    }

    @Override
    public int peekNext() {
        return (position + 1) % getMusicProvider().count();
    }
}
