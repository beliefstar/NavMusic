package com.zx.navmusic.service.impl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.Constants;
import com.zx.navmusic.common.Encryptor;
import com.zx.navmusic.common.LocalAudioStore;
import com.zx.navmusic.common.LocalStore;
import com.zx.navmusic.common.SignatureUtil;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpGlobalConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class LocalMusicProvider extends CloudMusicProvider {

    private boolean useLocalMode = false;

    public String bbsToken = "GTX1ABMONOcFUu_2B2wFVr15JHc23Caub8VdR3pPjFkMCe_2Bb9HwLmPVB9V99nkczUYCExsAEY9GczcJt2LL195QdUzAFZ1Eq3Z";

    @Override
    public void refresh(Context ctx) {
//        AsyncTask.run(() -> {我的天空
//            List<MusicItem> mis = doFetchList();
//            postValue(mis);
//
//            LocalStore.flush(ctx, mis);
//        });

        List<MusicItem> musicItems = LocalStore.loadMusicData(ctx);
        postValue(musicItems);

        observeForever(ms -> {
            storeData(ctx);
        });
    }

    @Override
    public CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchItem si) {
        MusicItem i = queryByName(si.name);

        Uri uri = LocalAudioStore.find(activity, si.name);
        if (uri != null) {
            if (i == null) {
                i = addItem(si);
            }
            if (Boolean.FALSE.equals(i.cache)) {
                uploadAsync(activity, i, uri);
            }
            return CompletableFuture.completedFuture(i);
        }

        final MusicItem dirty = i;
        return AsyncTask.supply(() -> {
            String url = queryTouchUrl(si);
            if (StrUtil.isBlank(url)) {
                App.toast("无法获取该歌曲资源");
                return null;
            }

            if (!initializing.add(si.id)) {
                return null;
            }
            MusicItem mi = dirty == null ? addItem(si) : dirty;
            mi.cache = false;

            MusicItem r = doTouchMusic(activity, mi, url);
            if (r != null) {
                initializing.remove(si.id);
            }
            return r;
        });
    }

    @Override
    public void remove(int position) {
        ArrayList<MusicItem> list = new ArrayList<>(getList());
        list.remove(position);
        postValue(list);
    }

    @Override
    public CompletableFuture<List<SearchItem>> search(FragmentActivity activity, String keyword) {
//        App.toast(activity, "useLocalMode: {}", useLocalMode);
        if (!useLocalMode) {
            return super.search(activity, keyword);
        }

        return AsyncTask.supply(() -> {
            String host = "https://www.hifini.com";
            String range = "1";
            String encodeKeyword = URLUtil.encode(keyword).replace("%", "_");
            String url = StrUtil.format("{}/search-{}-{}-{}.htm", host, encodeKeyword, range, 1);

            String listContent = HttpUtil.get(url);
            List<String> titles = ReUtil.findAll("(<a href=\"thread-\\w+.htm\">.*?</a>)", listContent, 1);

            List<SearchItem> items = new ArrayList<>();
            if (CollUtil.isNotEmpty(titles)) {
                for (String title : titles) {
                    SearchItem si = parseSearchItem(title);
                    if (si != null) {
                        items.add(si);
                    }
                }
            }
            return items;
        });
    }

    public void setUseLocalMode(boolean mode) {
        this.useLocalMode = mode;
    }

    public boolean getUseLocalMode() {
        return useLocalMode;
    }

    private MusicItem addItem(SearchItem si) {
        MusicItem mi = new MusicItem(si.id, si.name, false);

        List<MusicItem> nextList = new ArrayList<>(getValue());
        nextList.add(0, mi);
        postValue(nextList);

        return mi;
    }

    private void storeData(Context ctx) {
        List<MusicItem> value = getValue();
        if (value != null) {
            LocalStore.flush(ctx, value);
        }
    }

    private MusicItem queryByName(String name) {
        List<MusicItem> items = getValue();
        if (items == null) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).name.equals(name)) {
                return items.get(i);
            }
        }
        return null;
    }

    private MusicItem doTouchMusic(FragmentActivity activity, MusicItem mi, String url) {
        HttpGlobalConfig.setMaxRedirectCount(999);
        try (HttpResponse response = HttpRequest.get(url).timeout(60000)
                .header("Referer", "https://www.hifini.com/")
                .setFollowRedirects(true)
                .setMaxRedirectCount(999)
                .execute()) {
            LocalAudioStore.put(activity, mi.name, response.bodyStream());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(App.App_Name, "[CloudMusicProvider]获取失败" + e);
            return null;
        }
        storeData(activity);

        if (Boolean.FALSE.equals(mi.cache)) {
            Uri uri = LocalAudioStore.find(activity, mi.name);
            if (uri != null) {
                uploadAsync(activity, mi, uri);
            }
        }
        return mi;
    }

    private void uploadAsync(FragmentActivity activity, MusicItem mi, Uri uri) {
        AsyncTask.run(() -> {
            String url = HOST + API_UPLOAD + mi.id;
            LocalAudioStore.read(activity, uri, in -> {
                try (HttpResponse response = SignatureUtil.touchHeader(HttpRequest.post(url), getToken()).body(IoUtil.readBytes(in)).execute()) {
                    App.log("上传资源: {}", response.getStatus());
                    mi.cache = true;
                    storeData(activity);
                } catch (Exception e) {
                    App.log("上传资源失败: {}", e.getMessage());
                }
            });
        });
    }

    private String queryTouchUrl(SearchItem si) {
        if (Boolean.TRUE.equals(si.cache)) {
            return super.getItemRemoteUrl(si.id);
        }
        try {
            String url = "https://www.hifini.com/" + si.thread;
            HttpRequest req = HttpRequest.get(url)
                    .header("Cookie", "bbs_token=" + bbsToken);
            String listContent = req.execute().body();
//            String listContent = HttpUtil.get(url);
            List<String> titles = ReUtil.findAll("music: \\[(.*?)\\]", listContent, 1);
            return parsePlayUrl(titles.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            App.log("parsePlayUrlError:{}", e.getMessage());
            return null;
        }
    }

    public static String parsePlayUrl(String s) {
        if (s.contains("get_music.php")) {
            List<String> titles = ReUtil.findAll("url: '(get_music\\.php.*?)pic", s, 1);
            return parsePlayUrlPhp(titles.get(0));
        }
        JSONArray ja = JSON.parseArray("[" + s + "]");
        JSONObject jo = ja.getJSONObject(0);
        return jo.getString("url");
    }

    public static String parsePlayUrlPhp(String url) {
        url = StrUtil.trim(url);
        String[] split = url.split("\\+");
        String prefix = Optional.ofNullable(split[0]).map(String::trim).map(t -> t.substring(0, t.length() - 1)).orElse("");

        List<String> titles = ReUtil.findAll("generateParam\\('(.*?)'\\)", split[1], 1);
        String key = titles.get(0);
        key = Encryptor.generateParam(key);
        return "https://www.hifini.com/" + prefix + key;
    }

    public SearchItem parseSearchItem(String it) {
        String ref = extractRef(it);
        String name = extractTitle(it);
        if (StrUtil.isBlank(ref) || StrUtil.isBlank(name)) {
            System.out.println(it);
            return null;
        }
        String id = buildStoreId(name);
        SearchItem si = new SearchItem();
        si.id = id;
        si.name = name + Constants.MUSIC_NAME_SUFFIX;
        si.thread = ref;
        si.cache = false;
        return si;
    }

    public static String buildStoreId(String name) {
        return Base64.encodeUrlSafe(name) + Constants.MUSIC_NAME_SUFFIX;
    }

    private static String extractRef(String s) {
        List<String> titles = ReUtil.findAll("<a href=\"(thread-\\w+.htm)\">.*?</a>", s, 1);
        for (String title : titles) {
            title = title.replace("<em>", "");
            title = title.replace("</em>", "");
            title = title.trim();
            return title;
        }
        return "";
    }
    private static String extractTitle(String s) {
        List<String> titles = ReUtil.findAll("<a href=\"thread-\\w+.htm\">(.*?)(\\[.*?])?</a>", s, 1);
        for (String title : titles) {
            title = title.replace("<em>", "");
            title = title.replace("</em>", "");
            title = title.trim();
            return title;
        }
        return "";
    }
}
