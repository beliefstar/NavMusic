package com.zx.navmusic.common.bean;


import cn.hutool.core.util.StrUtil;

/**
 * @author xzhen
 * @creatAt 2026-03-12 10:12
 */
public class SearchResult {

    public String id;

    public String name;

    public String artist;

    public String ext;

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
}
