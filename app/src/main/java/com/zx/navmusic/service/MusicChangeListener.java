package com.zx.navmusic.service;

import com.zx.navmusic.common.bean.MusicItem;

public interface MusicChangeListener {

    default void onMusicStart(MusicItem item) {}

    default void onMusicCompleted() {}

    default void onMusicPlayPause(int playState) {}

    default void onPlayerReady() {}
}
