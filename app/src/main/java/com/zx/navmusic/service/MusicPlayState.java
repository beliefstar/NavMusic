package com.zx.navmusic.service;

public class MusicPlayState {

    public String name;

    public boolean isPlaying;

    public int playSwitchStrategy;

    public int duration;

    public int position;

    public int index;

    public void reset() {
        name = "";
        isPlaying = false;
        playSwitchStrategy = 0;
        duration = 0;
        position = 0;
        index = 0;
    }
}
