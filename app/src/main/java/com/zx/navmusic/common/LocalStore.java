package com.zx.navmusic.common;

import android.content.Context;

import com.alibaba.fastjson2.JSON;
import com.zx.navmusic.common.bean.ConfigDataBean;
import com.zx.navmusic.common.bean.MusicItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;

public class LocalStore {

    public static final String DATA_FILE = "repo.data";
    public static final String CONFIG_FILE = "config.data";
    public static final String LYRIC_FILE = "lyrics";

    public static void loadFile(Context context) {
        File dataDir = context.getDataDir();
        System.out.println("====================");
        System.out.println(dataDir.getAbsolutePath());
        System.out.println("====================");
        System.out.println("====================");
    }

    public static List<MusicItem> loadMusicData(Context ctx) {
        File root = ctx.getDataDir();
        File file = new File(root, DATA_FILE);
        if (file.exists()) {
            String content;
            try (FileInputStream in = new FileInputStream(file)) {
                content = IoUtil.readUtf8(in.getChannel());
            } catch (Exception e) {
                App.toast("文件读取失败 {}", e.getMessage());
                return new ArrayList<>();
            }
            return JSON.parseArray(content, MusicItem.class);
        }
        return new ArrayList<>();
    }

    public static void flush(Context ctx, List<MusicItem> list) {
        File root = ctx.getDataDir();
        File file = new File(root, DATA_FILE);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            IoUtil.write(Files.newOutputStream(file.toPath()), true, JSON.toJSONBytes(list));
        } catch (IOException e) {
            App.toast("文件写入失败 {}", e.getMessage());
        }
    }

    public static ConfigDataBean loadConfigData(Context ctx) {
        File root = ctx.getDataDir();
        File file = new File(root, CONFIG_FILE);
        if (file.exists()) {
            String content;
            try (FileInputStream in = new FileInputStream(file)) {
                content = IoUtil.readUtf8(in.getChannel());
            } catch (Exception e) {
                App.toast("文件读取失败 {}", e.getMessage());
                return new ConfigDataBean();
            }
            return JSON.parseObject(content, ConfigDataBean.class);
        }
        return new ConfigDataBean();
    }

    public static void flushConfig(Context ctx, ConfigDataBean configData) {
        File root = ctx.getDataDir();
        File file = new File(root, CONFIG_FILE);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            IoUtil.write(Files.newOutputStream(file.toPath()), true, JSON.toJSONBytes(configData));
        } catch (IOException e) {
            App.toast("文件写入失败 {}", e.getMessage());
        }
    }


    public static File getLyricDir(Context ctx) {
        File root = ctx.getDataDir();
        File file = new File(root, LYRIC_FILE);
        FileUtil.mkdir(file);
        return file;
    }

    public static String loadLyric(Context ctx, String musicId) {
        File file = getLyricDir(ctx);

        String main = FileNameUtil.mainName(musicId);
        File lrcFile = new File(file, main + ".lrc");

        if (!lrcFile.exists()) {
            return null;
        }

        return FileUtil.readString(lrcFile, StandardCharsets.UTF_8);
    }


    public static void flushLyric(Context ctx, String musicId, String content) {
        File file = getLyricDir(ctx);

        String main = FileNameUtil.mainName(musicId);
        File lrcFile = new File(file, main + ".lrc");

        try {
            if (!lrcFile.exists()) {
                lrcFile.createNewFile();
            }
            FileUtil.writeString(content, lrcFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            App.toast("文件写入失败 {}", e.getMessage());
        }
    }
}
