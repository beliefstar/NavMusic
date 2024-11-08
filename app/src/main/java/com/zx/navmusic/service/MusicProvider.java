package com.zx.navmusic.service;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MusicProvider {

    void refresh(Context ctx);

    List<MusicItem> getList();

    int count();

    CompletableFuture<List<SearchItem>> search(FragmentActivity activity, String keyword);

    int getIndexById(String musicId);

    MusicItem getItem(int index);

    String getItemRemoteUrl(MusicItem mi);

    CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchItem si);

    void remove(int position);
}
