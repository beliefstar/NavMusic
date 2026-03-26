package com.zx.navmusic.event;

import com.zx.navmusic.common.App;
import com.zx.navmusic.service.MusicPlayState;

import java.util.Set;
import java.util.function.Consumer;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;

public class NotifyCenter {

    private static final NotifyCenter instance = new NotifyCenter();

    private volatile MusicPlayState musicPlayState;
    private volatile String lastSign;

    private final Set<NotifyListener> notifyListeners;

    public NotifyCenter() {
        notifyListeners = new ConcurrentHashSet<>();
    }

    public static void registerListener(NotifyListener listener) {
        instance.notifyListeners.add(listener);
    }
    public static void unregisterListener(NotifyListener listener) {
        instance.notifyListeners.remove(listener);
    }

    public static void onMusicStart(MusicPlayState playState) {
        instance.trigger(l -> l.onMusicStart(playState), "onMusicStart");
    }

    public static void onMusicCompleted(MusicPlayState playState) {
        instance.trigger(l -> l.onMusicCompleted(playState), "onMusicCompleted");
    }

    public static void onMusicPlayPause(MusicPlayState playState) {
        instance.trigger(l -> l.onMusicPlayPause(playState), "onMusicPlayPause");
    }

    public synchronized static void onMusicStateChange(MusicPlayState playState) {
        String sign = playState.toString();
        if (StrUtil.isNotBlank(instance.lastSign) && StrUtil.equals(sign, instance.lastSign)) {
            return;
        }
        instance.lastSign = sign;
        instance.musicPlayState = playState;
        instance.trigger(l -> l.onMusicStateChange(playState), "onMusicStateChange");
    }

    private void trigger(Consumer<NotifyListener> action, String hookName) {
        for (NotifyListener notifyListener : notifyListeners) {
            try {
                action.accept(notifyListener);
            } catch (Exception e) {
                App.log("consumerError-{}.{}: {}", notifyListener.getClass().getName(), hookName, e.getMessage());
            }
        }
    }

    public static MusicPlayState getMusicPlayState() {
        return instance.musicPlayState;
    }
}
