package com.zx.navmusic.service.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zx.navmusic.MusicService;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.Constants;
import com.zx.navmusic.common.Encryptor;
import com.zx.navmusic.common.LocalAudioStore;
import com.zx.navmusic.common.LocalStore;
import com.zx.navmusic.common.SignatureUtil;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.config.ConfigCenter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpGlobalConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class LocalMusicProvider extends CloudMusicProvider {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ReentrantLock albumLock = new ReentrantLock();

    private final Runnable storeTask = new Runnable() {
        @Override
        public void run() {
            try {
                App.log("定时保存数据");
                storeData(MusicService.INSTANCE);
            } finally {
                mainHandler.postDelayed(this, TimeUnit.MINUTES.toMillis(1));
            }
        }
    };

    private String lastStoreVersion = null;

    @Override
    public void init(Context ctx) {
        List<MusicItem> musicItems = LocalStore.loadMusicData(ctx);
        postValue(musicItems);

        observeForever(ms -> {
            storeData(ctx);
        });

        mainHandler.postDelayed(storeTask, TimeUnit.MINUTES.toMillis(1));

//        AsyncTask.run(() -> {
//            List<MusicItem> result = new ArrayList<>();
//
//            List<MusicItem> ms = doFetchList();
//            for (MusicItem m : ms) {
//                m.cache = true;
//                Uri uri = LocalAudioStore.find(ctx, m.name);
//                if (uri != null) {
//                    result.add(m);
//                }
//            }
//
//            postValue(result);
//        });
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
            if (!initializing.add(si.id)) {
                return null;
            }
            String name = si.name;
            si.name = StrUtil.format("{}(正在获取资源)", si.name);
            MusicItem mi = dirty == null ? addItem(si) : dirty;
            si.name = name;
            try {
                String url = queryTouchUrl(si);
                if (StrUtil.isBlank(url)) {
                    App.toast("无法获取该歌曲资源");
                    return null;
                }

                mi.name = name;
                mi.cache = !ConfigCenter.isUseLocalMode();

                return doTouchMusic(activity, mi, url);
            } finally {
                initializing.remove(si.id);
            }
        });
    }

    @Override
    public void remove(int position) {
        ArrayList<MusicItem> list = new ArrayList<>(getList());
        list.remove(position);
        postValue(list);
    }

    @Override
    public void remove(String id) {
        ArrayList<MusicItem> list = new ArrayList<>(getList());
        list.removeIf(m -> StrUtil.equals(m.id, id));
        postValue(list);
    }

    @Override
    public CompletableFuture<List<SearchItem>> search(FragmentActivity activity, String keyword) {
//        App.toast(activity, "useLocalMode: {}", useLocalMode);
        if (!ConfigCenter.isUseLocalMode()) {
            return super.search(activity, keyword);
        }

        return AsyncTask.supply(() -> {
            String host = "https://www.hifini.com";
            String range = "1";
            String encodeKeyword = URLUtil.encode(keyword).replace("%", "_");
            String url = StrUtil.format("{}/search-{}-{}-{}.htm", host, encodeKeyword, range, 1);

            String listContent = HttpUtil.get(url);
            Document doc = Jsoup.parse(listContent);
            Elements as = doc.body().getElementsByTag("a");

            List<SearchItem> items = new ArrayList<>();
            for (Element a : as) {
                String ref = a.attr("href");
                if (ref.startsWith("thread-")) {
                    SearchItem si = parseSearchItem(a);
                    if (si != null) {
                        items.add(si);
                    }
                }
            }
            return items;
        });
    }

    @Override
    public List<String> getItemLyric(String musicId) {
        String content = LocalStore.loadLyric(MusicService.INSTANCE, musicId);
        if (StrUtil.isNotBlank(content)) {
            return StrUtil.split(content, "\n", true, true);
        }

        AsyncTask.run(() -> {
            App.log("本地没有歌词，准备从云端加载");
            List<String> lyrics = super.getItemLyric(musicId);
            if (CollUtil.isNotEmpty(lyrics)) {
                App.log("从云端加载歌词成功，准备保存到本地");
                LocalStore.flushLyric(MusicService.INSTANCE, musicId, String.join("\n", lyrics));
                MusicService.INSTANCE.triggerMusicStateChange();
            }
        });
        return null;
    }

    @Override
    public Bitmap getAlbum(String musicId) {
        Bitmap bitmap = LocalStore.loadAlbum(MusicService.INSTANCE, musicId);
        if (bitmap != null) {
            return bitmap;
        }

        AsyncTask.run(() -> {
            albumLock.lock();
            try {
                if (LocalStore.loadAlbum(MusicService.INSTANCE, musicId) != null) {
                    return;
                }

                Bitmap b = super.getAlbum(musicId);
                if (b != null) {
                    LocalStore.flushAlbum(MusicService.INSTANCE, musicId, b);
                    MusicService.INSTANCE.triggerMusicStateChange();
                }
            } finally {
                albumLock.unlock();
            }
        });
        return null;
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
            String version = buildStoreVersion(value);
            if (StrUtil.equals(version, lastStoreVersion)) {
                return;
            }
            lastStoreVersion = version;
            LocalStore.flush(ctx, value);
        }
    }

    private String buildStoreVersion(List<MusicItem> items) {
        if (items == null || items.isEmpty()) {
            return "0";
        }
        String s = JSON.toJSONString(items);
        return MD5.create().digestHex(s, StandardCharsets.UTF_8);
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
        LocalAudioStore.put(activity, mi.name, out -> {
            HttpGlobalConfig.setMaxRedirectCount(999);

            HttpDownloader.download(url, out, true, new StreamProgress() {
                @Override
                public void start() {
                }

                @Override
                public void progress(long total, long progressSize) {
                    String p = NumberUtil.formatPercent(progressSize * 1.0 / total, 2);
                    if (mi.name.contains("(") && mi.name.endsWith(")")) {
                        int i = mi.name.lastIndexOf("(");
                        mi.name = mi.name.substring(0, i);
                    }
                    mi.name = mi.name + "(" + p + ")";
                    postValue(getValue());
                }

                @Override
                public void finish() {
                    if (mi.name.contains("(") && mi.name.endsWith(")")) {
                        int i = mi.name.lastIndexOf("(");
                        mi.name = mi.name.substring(0, i);
                        postValue(getValue());
                    }
                }
            });
        });

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
        if (!ConfigCenter.isUseLocalMode()) {
            return getDownloadUrl(si);
        }
        String bbsToken = ConfigCenter.getBbsToken();
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

    public SearchItem parseSearchItem(Element a) {
        String ref = a.attr("href");
        String name = parseName(a.text());
        if (StrUtil.isBlank(ref) || StrUtil.isBlank(name)) {
            System.out.println(a);
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

    private static String parseName(String name) {
        int lidx = name.indexOf("[");
        int ridx = name.lastIndexOf("]");
        if (lidx == -1 || ridx == -1) {
            return name;
        }
        return name.substring(0, lidx) + name.substring(ridx + 1);
    }
}
