package com.zx.navmusic.service;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.service.impl.LocalMusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.CollUtil;

public abstract class MusicLiveProvider extends LiveData<List<MusicItem>> implements MusicProvider {

    private static volatile MusicLiveProvider instance;

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
    public List<SearchItem> searchLocal(String keyword) {
        List<MusicItem> values = getValue();
        if (CollUtil.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<SearchItem> sis = new ArrayList<>();
        for (MusicItem value : values) {
            if (value.name.contains(keyword)) {
                SearchItem si = new SearchItem();
                si.name = value.name;
                si.id = value.id;
                si.cache = true;
                sis.add(si);
            }
        }
        return sis;
    }
}
