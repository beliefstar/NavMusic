package com.zx.navmusic.service;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.service.impl.LocalMusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cn.hutool.core.collection.CollUtil;

public abstract class MusicLiveProvider extends LiveData<List<MusicItem>> implements MusicProvider {

    private static final MusicLiveProvider instance = new LocalMusicProvider();

    public static MusicLiveProvider getInstance() {
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
    public void refresh(Context ctx) {
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
