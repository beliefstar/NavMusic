package com.zx.navmusic.lyric;

import com.zx.navmusic.common.bean.LyricLine;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.List;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.CollUtil;

public class LyricHandler {
    private final Cache<String, List<LyricLine>> cache = CacheUtil.newFIFOCache(15);

    public List<LyricLine> getLyric(String musicId) {
        if (cache.containsKey(musicId)) {
            return cache.get(musicId);
        }
        List<String> lyric = MusicLiveProvider.getInstance().getItemLyric(musicId);
        List<LyricLine> lyricLines = LyricParser.parseLrc(lyric);
        if (CollUtil.isNotEmpty(lyricLines)) {
            cache.put(musicId, lyricLines);
        }
        return lyricLines;
    }
}
