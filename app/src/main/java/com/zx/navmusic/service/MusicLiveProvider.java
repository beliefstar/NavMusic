package com.zx.navmusic.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchResult;
import com.zx.navmusic.service.impl.LocalMusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.CollUtil;

public abstract class MusicLiveProvider extends LiveData<List<MusicItem>> implements MusicProvider {

    private static volatile MusicLiveProvider instance;

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    public synchronized static MusicLiveProvider getInstance() {
        if (instance == null) {
            instance = new LocalMusicProvider();
        }
        return instance;
    }

    public MusicLiveProvider(List<MusicItem> value) {
        super(value);
    }

    public MusicLiveProvider() {
    }


    @Override
    public List<MusicItem> getList() {
        return getValue();
    }

    @Override
    public void init(Context ctx) {
    }

    @Override
    public void refresh() {
        postValue(getValue());
    }

    @Override
    public int count() {
        List<MusicItem> value = getValue();
        if (value == null) {
            return 0;
        }
        return value.size();
    }

    @Override
    public void remove(int position) {

    }

    @Override
    public void remove(String id) {

    }

    @Override
    public List<SearchResult> searchLocal(String keyword) {
        List<MusicItem> values = getValue();
        if (CollUtil.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<SearchResult> sis = new ArrayList<>();
        for (MusicItem value : values) {
            if (value.name.contains(keyword)) {
                SearchResult si = new SearchResult();
                si.name = value.name;
                si.artist = value.artist;
                si.ext = value.ext;
                si.id = value.id;
                sis.add(si);
            }
        }
        return sis;
    }

    @Override
    public List<SearchResult> searchLibrary(String keyword) {
        List<SearchResult> values = getLibrary();
        if (CollUtil.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<SearchResult> sis = new ArrayList<>();
        for (SearchResult value : values) {
            if (value.name.contains(keyword)) {
                SearchResult si = new SearchResult();
                si.name = value.name;
                si.artist = value.artist;
                si.ext = value.ext;
                si.id = value.id;
                sis.add(si);
            }
        }
        return sis;
    }
}
