package com.zx.navmusic.service;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MusicProvider {

    void init(Context ctx);

    void refresh();

    List<MusicItem> getList();

    int count();

    CompletableFuture<List<SearchItem>> search(FragmentActivity activity, String keyword);

    List<SearchItem> searchLocal(String keyword);

    int getIndexById(String musicId);

    MusicItem getItem(int index);

    String getItemRemoteUrl(MusicItem mi);

    String getItemAlbumUrl(String musicId);

    CompletableFuture<List<String>> getItemLyric(String musicId);

    CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchItem si);

    void remove(int position);

    void remove(String id);
}
