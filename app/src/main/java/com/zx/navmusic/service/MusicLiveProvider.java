package com.zx.navmusic.service;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.service.impl.LocalMusicProvider;

import java.util.List;

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
}
