package com.zx.navmusic.common.bean;

import androidx.annotation.NonNull;

public class MusicItem {

    public String id;

    public String name;

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
