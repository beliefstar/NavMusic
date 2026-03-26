package com.zx.navmusic.service;

import androidx.annotation.NonNull;

public class MusicPlayState {

    public String id;

    public String name;

    public String artist;

    public String album;

    public boolean isPlaying;

    public int playSwitchStrategy;

    public int duration;

    public int position;

    public int index;

    @NonNull
    @Override
    public String toString() {
        return "MusicPlayState{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", isPlaying=" + isPlaying +
                ", playSwitchStrategy=" + playSwitchStrategy +
                ", duration=" + duration +
                ", position=" + position +
                ", index=" + index +
                '}';
    }
}
