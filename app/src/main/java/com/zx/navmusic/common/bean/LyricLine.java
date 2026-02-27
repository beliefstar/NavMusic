package com.zx.navmusic.common.bean;


public class LyricLine {
    public long timeMs;
    public String text;

    public LyricLine(long t, String s) {
        timeMs = t;
        text = s;
    }
}
