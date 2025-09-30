package com.zx.navmusic.service.strategy;

import com.zx.navmusic.service.MusicLiveProvider;

public abstract class AbsPlayModeStrategy implements PlayModeStrategy {
    protected int position = 0;
    private final int type;

    public AbsPlayModeStrategy(int type) {
        this.type = type;
    }

    protected MusicLiveProvider getMusicProvider() {
        return MusicLiveProvider.getInstance();
    }

    @Override
    public int getType() {
        return type;
    }
}
