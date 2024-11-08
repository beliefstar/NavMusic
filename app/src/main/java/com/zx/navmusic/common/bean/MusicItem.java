package com.zx.navmusic.common.bean;

public class MusicItem {

    public String id;

    public String name;

    public Boolean cache;

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
}
