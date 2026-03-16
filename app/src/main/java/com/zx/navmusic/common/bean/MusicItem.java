package com.zx.navmusic.common.bean;

import com.zx.navmusic.common.Util;

import cn.hutool.core.util.StrUtil;

public class MusicItem {

    public String id;

    public String name;

    public String artist;

    public String ext;

    public String album;

    public Boolean cache;

    public int download;

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


    public MusicItem(String id, String name, String artist, String ext) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.ext = ext;
        this.cache = true;
    }

    public int getRankRes() {
        return Util.musicRankRes(score);
    }

    public String displayName() {
        if (StrUtil.isBlank(name)) {
            return "";
        }
        if (StrUtil.isNotBlank(artist) && StrUtil.isNotBlank(ext)) {
            return StrUtil.format("{}-{}.{}", artist, name, ext);
        }
        if (StrUtil.isNotBlank(artist)) {
            return StrUtil.format("{}-{}", artist, name);
        }
        if (StrUtil.isNotBlank(ext)) {
            return StrUtil.format("{}.{}", name, ext);
        }
        return name;
    }

    @Override
    public String toString() {
        return "MusicItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", ext='" + ext + '\'' +
                ", album='" + album + '\'' +
                ", cache=" + cache +
                ", score=" + score +
                '}';
    }
}
