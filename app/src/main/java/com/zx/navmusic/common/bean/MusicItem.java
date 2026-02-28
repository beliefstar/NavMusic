package com.zx.navmusic.common.bean;

import androidx.annotation.NonNull;

import com.zx.navmusic.common.Util;

public class MusicItem {

    public String id;

    public String name;

    public String album;

    public Boolean cache;

    public Integer score = 0;

    public MusicItem() {
    }

    public MusicItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public MusicItem(String id, String name, boolean cache) {
        this.id = id;
        this.name = name;
        this.cache = cache;
    }

    public int getRankRes() {
        return Util.musicRankRes(score);
    }

    @NonNull
    @Override
    public String toString() {
        return "MusicItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cache=" + cache +
                ", score=" + score +
                '}';
    }
}
