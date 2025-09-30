package com.zx.navmusic.service.strategy;

import com.zx.navmusic.service.strategy.impl.LinearPlayModeStrategy;
import com.zx.navmusic.service.strategy.impl.RandomPlayModeStrategy;
import com.zx.navmusic.service.strategy.impl.SinglePlayModeStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayModeFactory {

    private static final Map<Integer, PlayModeStrategy> ins = new ConcurrentHashMap<>();

    public static PlayModeStrategy get(int type) {
        return ins.computeIfAbsent(type, k -> {
            switch (k) {
                case PlayModeStrategy.LINEAR:
                    return new LinearPlayModeStrategy();
                case PlayModeStrategy.RANDOM:
                    return new RandomPlayModeStrategy();
                case PlayModeStrategy.SINGLE:
                    return new SinglePlayModeStrategy();
            }
            throw new RuntimeException("unknown PlayModeStrategy type " + k);
        });
    }
}
