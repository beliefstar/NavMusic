package com.zx.navmusic.service.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

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
import com.zx.navmusic.common.Util;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.MusicName;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.common.bean.SearchResult;

import org.jsoup.nodes.Element;

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
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpGlobalConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

public class LocalMusicProvider extends CloudMusicProvider {

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
        musicItems.forEach(Util::parseMusicItem);

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
    public CompletableFuture<MusicItem> touchMusic(FragmentActivity activity, SearchResult si) {
        MusicItem i = queryBySearch(si);

        Uri uri = i == null
                ? LocalAudioStore.find(activity, si.name, si.artist)
                : LocalAudioStore.find(activity, i);

        if (uri != null) {
            if (i == null) {
                i = addItem(buildStoreId(si.name, si.artist, si.ext), si);
            }
            return CompletableFuture.completedFuture(i);
        }

        if (!initializing.add(si.id)) {
            return null;
        }
        final MusicItem dirty = i;
        return AsyncTask.supply(() -> {
            MusicItem mi = dirty == null ? addItem(si) : dirty;
            String musicId;
            try {
                musicId = touchMusic(si);
                if (StrUtil.isBlank(musicId)) {
                    App.toast("无法获取该歌曲资源");
                    mi.name = mi.name + "(无法获取该歌曲资源)";
                    postValue(getValue());
                    return null;
                }
                fixMusicInfo(mi, musicId);
                mi.cache = true;
                return doTouchMusic(activity, mi, getItemRemoteUrl(musicId));
            } finally {
                initializing.remove(si.id);
            }
        });
    }

    private void fixMusicInfo(MusicItem mi, String musicId) {
        MusicName musicName = Util.parseMusicId(musicId);
        mi.id = musicId;
        mi.name = musicName.name;
        mi.artist = musicName.artist;
        mi.ext = musicName.ext;
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

    private MusicItem addItem(SearchResult si) {
        return addItem(si.id, si);
    }

    private MusicItem addItem(String id, SearchResult si) {
        MusicItem mi = new MusicItem(id, si.name, si.artist, si.ext);

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

    private MusicItem queryBySearch(SearchResult si) {
        List<MusicItem> items = getValue();
        if (items == null) {
            return null;
        }
        for (MusicItem item : items) {
            if (StrUtil.equals(item.name, si.name) && StrUtil.equals(item.artist, si.artist)) {
                return item;
            }
        }
        return null;
    }

    private MusicItem doTouchMusic(FragmentActivity activity, MusicItem mi, String url) {
        LocalAudioStore.put(activity, mi.displayName(), out -> {
            HttpGlobalConfig.setMaxRedirectCount(999);

            HttpDownloader.download(url, out, true, new StreamProgress() {
                @Override
                public void start() {
                }

                @Override
                public void progress(long total, long progressSize) {
                    mi.download = (int) (progressSize * 1.0 / total * 10000);
                    postValue(getValue());
                }

                @Override
                public void finish() {
                    mi.download = 0;
                    postValue(getValue());
                }
            });
        });

        storeData(activity);
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

    private String queryTouchUrl(SearchResult si) {
//        if (Boolean.TRUE.equals(si.cache)) {
//            return super.getItemRemoteUrl(si.id);
//        }
        return touchMusic(si);
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

    public static String buildStoreId(String name, String artist, String ext) {
        return Base64.encodeUrlSafe(artist + "-" + name) + "." + ext;
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
