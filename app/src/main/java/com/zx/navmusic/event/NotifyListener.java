package com.zx.navmusic.event;

import com.zx.navmusic.service.MusicPlayState;

public interface NotifyListener {
    default void onMusicStart(MusicPlayState playState) {}

    default void onMusicCompleted(MusicPlayState playState) {}

    default void onMusicPlayPause(MusicPlayState playState) {}

    default void onMusicStateChange(MusicPlayState playState) {}
}
