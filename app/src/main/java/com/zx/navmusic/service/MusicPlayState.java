package com.zx.navmusic.service;

public class MusicPlayState {

    public String id;

    public String name;

    public String artist;

    public boolean isPlaying;

    public int playSwitchStrategy;

    public int duration;

    public int position;

    public int index;

    public void reset() {
        name = "";
        artist = "";
        isPlaying = false;
        playSwitchStrategy = 0;
        duration = 0;
        position = 0;
        index = 0;
    }
}
