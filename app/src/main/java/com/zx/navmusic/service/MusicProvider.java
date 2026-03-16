package com.zx.navmusic.service;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.fragment.app.FragmentActivity;

import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MusicProvider {

    void init(Context ctx);

    void refresh();

    List<MusicItem> getList();

    int count();

    List<SearchResult> getLibrary();

    CompletableFuture<List<SearchResult>> search(FragmentActivity activity, String keyword);

    List<SearchResult> searchLocal(String keyword);

    List<SearchResult> searchLibrary(String keyword);

    int getIndexById(String musicId);

    MusicItem getItem(int index);

    Boolean isInitializing(String musicId);

    MusicItem getItem(String musicId);

    String getItemRemoteUrl(MusicItem mi);

    String getItemAlbumUrl(String musicId);

    List<String> getItemLyric(String musicId);

    Bitmap getAlbum(String musicId);

    String getAlbumName(String musicId);

    CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchResult si);

    void remove(int position);

    void remove(String id);
}
