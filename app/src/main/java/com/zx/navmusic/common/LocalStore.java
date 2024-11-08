package com.zx.navmusic.common;

import android.content.Context;

import com.alibaba.fastjson2.JSON;
import com.zx.navmusic.common.bean.MusicItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.io.IoUtil;

public class LocalStore {

    public static final String DATA_FILE = "repo.data";

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
}
