package com.zx.navmusic.service.strategy;

import com.zx.navmusic.common.App;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.concurrent.CompletableFuture;

public abstract class AbsPlayModeStrategy implements PlayModeStrategy {
    protected int position = 0;
    private final int type;
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();

    public AbsPlayModeStrategy(int type) {
        this.type = type;

        getMusicProvider().observeForever(lst -> {
            if (lst != null && !lst.isEmpty()) {
                init();
            }
        });
    }

    protected MusicLiveProvider getMusicProvider() {
        return MusicLiveProvider.getInstance();
    }

    protected void init() {
        if (!initFuture.isDone()) {
            App.log("[PlayMode]init --> {}", getCurPos());
            initFuture.complete(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> listenInit() {
        return initFuture;
    }

    @Override
    public int getType() {
        return type;
    }
}
