package com.zx.navmusic.album;

import android.graphics.Bitmap;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.concurrent.locks.ReentrantLock;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.StrUtil;

public class AlbumHandler {
    private final Cache<String, Bitmap> albumCache = CacheUtil.newFIFOCache(5);
    private final ReentrantLock albumNameLock = new ReentrantLock();

    public AlbumHandler() {
    }

    public synchronized Bitmap getAlbum(String musicId) {
        if (albumCache.containsKey(musicId)) {
            return albumCache.get(musicId);
        }

        Bitmap bitmap = MusicLiveProvider.getInstance().getAlbum(musicId);
        if (bitmap != null) {
            albumCache.put(musicId, bitmap);
            getAlbumName(musicId);
            return bitmap;
        }
        return null;
    }

    public String getAlbumName(String musicId) {
        MusicItem mi = MusicLiveProvider.getInstance().getItem(musicId);
        if (mi == null) {
            return null;
        }
        if (StrUtil.isNotBlank(mi.album)) {
            return mi.album;
        }
        AsyncTask.run(() -> {
            albumNameLock.lock();
            try {
                if (StrUtil.isNotBlank(mi.album)) {
                    return;
                }
                App.log("本地没有专辑名称，准备从云端加载");
                String album = MusicLiveProvider.getInstance().getAlbumName(musicId);
                if (StrUtil.isNotBlank(album)) {
                    mi.album = album;
                    MusicService.INSTANCE.triggerMusicStateChange();
                }
            } finally {
                albumNameLock.unlock();
            }
        });
        return null;
    }
}
