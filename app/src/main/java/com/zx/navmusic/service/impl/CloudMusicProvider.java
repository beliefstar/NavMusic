package com.zx.navmusic.service.impl;

import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.alibaba.fastjson2.JSON;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.LocalAudioStore;
import com.zx.navmusic.common.SignatureUtil;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;


public class CloudMusicProvider extends MusicLiveProvider {

    public static final String HOST = "http://154.12.55.187:8080";
    public static final String PLAY_URL = "/api/music/resource/";

    public static final String API_GET_LIST = "/api/music/list";
    public static final String API_SEARCH = "/api/v2/music/search";
    public static final String API_SEARCH_TOUCH = "/api/music/getPlayUrl";
    public static final String API_UPLOAD = "/api/v2/music/upload/";

    public static final String API_UNLOCK = "/api/ip/unlock";

    public static final String SERVICE_BUSY = "service_busy";
    public static final String SERVICE_ERROR = "service_error";
    public static final String SERVICE_TIMEOUT = "service_timeout";

    private String token = "fbf3a6a74df34b35a145d9bf341483cc";

    protected final Set<String> initializing = new ConcurrentHashSet<>();

    public CloudMusicProvider() {
        super(null);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
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
    public String getItemRemoteUrl(MusicItem musicItem) {
        return getItemRemoteUrl(musicItem.id);
    }

    protected String getItemRemoteUrl(String id) {
        return SignatureUtil.buildUrl(HOST + PLAY_URL + id, token);
    }

    @Override
    public CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchItem si) {
        String tempId = UUID.randomUUID().toString(true);
        String name = buildMusicItemName(si);
        if (!initializing.add(tempId)) {
            return CompletableFuture.completedFuture(null);
        }

        List<MusicItem> nextList = new ArrayList<>(getValue());
        nextList.add(0, new MusicItem(tempId, name));
        listChange(nextList);

        return AsyncTask.supply(() -> {
            MusicItem mi = doTouchMusic(activity, si);
            if (mi != null) {
                initializing.remove(tempId);
            }
            return mi;
        });
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
    public CompletableFuture<List<SearchItem>> search(FragmentActivity activity, String keyword) {
        return AsyncTask.supply(() -> {
            String url = HOST + API_SEARCH + "?keyword=" + keyword;
            List<SearchItem> res = Collections.emptyList();

            try {
                res = executeHttp(HttpRequest.get(url), body -> JSON.parseArray(body, SearchItem.class), res);
            } catch (Exception e) {
                Log.d(App.App_Name, "[CloudMusicProvider]搜索失败" + e);
            }
            return res;
        });
    }

    @Override
    public void refresh(Context ctx) {
        doRefresh();
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

    private MusicItem doTouchMusic(FragmentActivity activity, SearchItem si) {
        String url = StrUtil.format("{}?thread={}&name={}", HOST + API_SEARCH_TOUCH, si.thread, si.name);


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

        try {
            List<MusicItem> list = doRefresh().get();
            MusicItem mi = getItemById(id, list);
            if (mi != null) {
                String playUrl = getItemRemoteUrl(mi);
                try (HttpResponse response = HttpRequest.get(playUrl).execute()) {
                    LocalAudioStore.put(activity, mi.name, response.bodyStream());
                } catch (Exception e) {
                    Log.d(App.App_Name, "[CloudMusicProvider]下载到本地失败" + e);
                }
            }
            return mi;
        } catch (Exception e) {
            Log.d(App.App_Name, "[CloudMusicProvider]获取失败" + e);
        }
        return null;
    }

    private void executeHttp(HttpRequest req, Consumer<String> consumer) {
        executeHttp(req, body -> {
            consumer.accept(body);
            return null;
        }, null);
    }
    private <T> T executeHttp(HttpRequest req, Function<String, T> func, T defaultValue) {
        try (HttpResponse response = SignatureUtil.touchHeader(req, token).execute()) {
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
