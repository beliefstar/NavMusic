package com.zx.navmusic.service.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.alibaba.fastjson2.JSON;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.SignatureUtil;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.common.bean.SearchResult;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;


public class CloudMusicProvider extends MusicLiveProvider {

    public static final String HOST = "http://154.12.55.187:8080";
    public static final String PLAY_URL = "/api/music/resource/";
    public static final String ALBUM_URL = "/api/v2/album/image";
    public static final String ALBUM_NAME_URL = "/api/v2/album/name";
    public static final String API_LYRIC = "/api/v2/lyric";

    public static final String API_GET_LIST = "/api/music/list";
    public static final String API_SEARCH = "/api/v2/music/search";
    public static final String API_SEARCH_TOUCH = "/api/music/getPlayUrl";
    public static final String API_UPLOAD = "/api/v2/music/upload/";

    public static final String API_V3_SEARCH = "/api/v3/music/search";
    public static final String API_V3_TOUCH = "/api/v3/music/touch";

    public static final String API_UNLOCK = "/api/ip/unlock";

    public static final String SERVICE_BUSY = "service_busy";
    public static final String SERVICE_ERROR = "service_error";
    public static final String SERVICE_TIMEOUT = "service_timeout";

    private String secret = "fbf3a6a74df34b35a145d9bf341483cc";

    protected final Set<String> initializing = new ConcurrentHashSet<>();

    private List<SearchResult> libraryCache = Collections.emptyList();

    public CloudMusicProvider() {
        super(null);

        Runnable refreshLibraryTask = new Runnable() {
            @Override
            public void run() {
                try {
                    libraryCache = doGetLibrary();
                    App.log("定时更新曲库-{}", libraryCache.size());
                } finally {
                    AsyncTask.delay(this, TimeUnit.MINUTES.toMillis(1));
                }
            }
        };

        AsyncTask.run(refreshLibraryTask);
    }

    public void setToken(String token) {
        this.secret = token;
    }

    public String getToken() {
        return this.secret;
    }

    @Override
    public int getIndexById(String musicId) {
        List<MusicItem> musicItems = getValue();
        if (musicItems == null) {
            return -1;
        }
        for (int i = 0; i < musicItems.size(); i++) {
            if (musicItems.get(i).id.equals(musicId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Boolean isInitializing(String musicId) {
        return initializing.contains(musicId);
    }

    @Override
    public MusicItem getItem(int index) {
        if (getValue() == null || index < 0 || index >= getValue().size()) {
            return null;
        }
        MusicItem mi = getValue().get(index);
        if (initializing.contains(mi.id)) {
            App.toast("该曲目正在初始化中，请稍后播放");
            return null;
        }
        return mi;
    }

    @Override
    public MusicItem getItem(String musicId) {
        if (StrUtil.isBlank(musicId)) {
            return null;
        }
        List<MusicItem> musicItems = getValue();
        if (musicItems == null) {
            return null;
        }
        for (MusicItem mi : musicItems) {
            if (StrUtil.equals(mi.id, musicId)) {
                return mi;
            }
        }
        return null;
    }

    @Override
    public String getItemRemoteUrl(MusicItem musicItem) {
        return getItemRemoteUrl(musicItem.id);
    }

    @Override
    public String getItemAlbumUrl(String musicId) {
        return SignatureUtil.buildUrl(HOST + ALBUM_URL + "?musicId=" + musicId, secret);
    }

    protected String getItemRemoteUrl(String id) {
        return SignatureUtil.buildUrl(HOST + PLAY_URL + id, secret);
    }

    @Override
    public CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchResult si) {
        App.toast("不支持的操作");
        return CompletableFuture.completedFuture(BeanUtil.toBean(si, MusicItem.class));
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
    public CompletableFuture<List<SearchResult>> search(FragmentActivity activity, String keyword) {
        return AsyncTask.supply(() -> {
            String url = HOST + API_V3_SEARCH + "?keyword=" + keyword;
            List<SearchResult> res = Collections.emptyList();

            try {
                res = executeHttp(HttpRequest.get(url), body -> JSON.parseArray(body, SearchResult.class), res);
            } catch (Exception e) {
                Log.d(App.App_Name, "[CloudMusicProvider]搜索失败" + e);
            }
            return res;
        });
    }

    @Override
    public void init(Context ctx) {
        doRefresh();
    }

    @Override
    public String getAlbumName(String musicId) {
        String url = StrUtil.format("{}?musicId={}", HOST + ALBUM_NAME_URL, musicId);
        try {
            return executeHttp(HttpRequest.get(url), body -> body, null);
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取专辑名称失败" + e);
        }
        return "";
    }

    @Override
    public Bitmap getAlbum(String musicId) {
        String url = getItemAlbumUrl(musicId);

        Bitmap bitmap = null;
        try (HttpResponse response = SignatureUtil.touchHeader(HttpRequest.get(url), secret).execute()) {
            if (response.isOk()) {
                bitmap = BitmapFactory.decodeStream(response.bodyStream());
            }
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取专辑图片失败" + e);
        }
        return bitmap;
    }

    @Override
    public List<String> getItemLyric(String musicId) {
        String url = StrUtil.format("{}?musicId={}", HOST + API_LYRIC, musicId);
        String lyric = null;
        try {
            lyric = executeHttp(HttpRequest.get(url), body -> body, null);
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取歌词失败" + e);
        }
        if (lyric == null) {
            return null;
        }
        return StrUtil.split(lyric, "\n", true, true);
    }

    public void unlock() {
        AsyncTask.run(() -> {
            executeHttp(HttpRequest.post(HOST + API_UNLOCK), body -> {
                App.toast("body: {}", body);
            });
        });
    }

    private CompletableFuture<List<MusicItem>> doRefresh() {
        return AsyncTask.supply(this::doFetchList)
                .whenComplete((list, ex) -> {
                    if (ex == null) {
                        listChange(list);
                    }
                });
    }

    private void listChange(List<MusicItem> lst) {
        postValue(lst);
    }

    private List<SearchResult> doGetLibrary() {
        List<MusicItem> musicItems = doFetchList();
        if (CollUtil.isEmpty(musicItems)) {
            return new ArrayList<>();
        }
        return musicItems.stream()
                .map(t -> {
                    SearchResult si = new SearchResult();
                    si.id = t.id;
                    si.name = t.name;
                    si.artist = t.artist;
                    si.ext = t.ext;
                    return si;
                }).collect(Collectors.toList());
    }

    @Override
    public List<SearchResult> getLibrary() {
        return libraryCache;
    }

    public List<MusicItem> doFetchList() {
        List<MusicItem> musicItems = Collections.emptyList();
        try {
            musicItems = executeHttp(HttpRequest.get(HOST + API_GET_LIST), body -> JSON.parseArray(body, MusicItem.class), musicItems);
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取列表失败" + e);
        }
        App.log("[CloudMusicProvider]FetchList-size: {}", musicItems.size());
        return musicItems;
    }


    private MusicItem getItemById(String musicId, List<MusicItem> list) {
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        for (MusicItem musicItem : list) {
            if (musicItem.id.equals(musicId)) {
                return musicItem;
            }
        }
        return null;
    }

    private String buildMusicItemName(SearchItem si) {
        return si.name + ".mp3";
    }


    protected String touchMusic(SearchResult si) {
        String url = StrUtil.format("{}?id={}", HOST + API_V3_TOUCH, si.id);

        String id = null;
        try {
            id = executeHttp(HttpRequest.get(url), body -> body, null);
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取失败" + e);
        }

        if (StrUtil.isBlank(id)) {
            return null;
        }
        switch (id) {
            case SERVICE_ERROR:
            case SERVICE_BUSY:
            case SERVICE_TIMEOUT:
                App.toast("服务繁忙，请稍后再试");
                return null;
        }

        return id;
    }

    private void executeHttp(HttpRequest req, Consumer<String> consumer) {
        executeHttp(req, body -> {
            consumer.accept(body);
            return null;
        }, null);
    }
    private <T> T executeHttp(HttpRequest req, Function<String, T> func, T defaultValue) {
        try (HttpResponse response = SignatureUtil.touchHeader(req, secret).execute()) {
            if (response.isOk()) {
                String body = response.body();
                return func.apply(body);
            } else {
                App.toast("状态码: {}", response.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return defaultValue;
    }
}
