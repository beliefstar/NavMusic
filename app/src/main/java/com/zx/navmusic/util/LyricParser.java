package com.zx.navmusic.util;

import com.zx.navmusic.common.bean.LyricLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.collection.CollUtil;

public class LyricParser {

    public static List<LyricLine> parseLrc(List<String> lines) {
        if (CollUtil.isEmpty(lines)) {
            lines = new ArrayList<>(Collections.singletonList("[00:00.00]当前没有歌词"));
        }

        List<LyricLine> result = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 跳过元信息
            if (line.matches("\\[\\w+:.*]")) continue;

            // 解析歌词行
            parseLyricLine(line, result);
        }

        // 按时间排序
        Collections.sort(result, Comparator.comparingLong(l -> l.timeMs));

        return result;
    }

    private static void parseLyricLine(String line, List<LyricLine> out) {
        // 匹配 [mm:ss.xx]
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?]");
        Matcher matcher = pattern.matcher(line);

        List<Long> timeList = new ArrayList<>();

        while (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int sec = Integer.parseInt(matcher.group(2));
            int ms = matcher.group(3) != null
                    ? Integer.parseInt(padRight(matcher.group(3)))
                    : 0;

            long timeMs = min * 60_000L + sec * 1000L + ms;
            timeList.add(timeMs);
        }

        // 去掉所有时间戳，剩下的是歌词文本
        String text = line.replaceAll("\\[.*?]", "").trim();

        for (Long t : timeList) {
            out.add(new LyricLine(t, text));
        }
    }

    private static String padRight(String ms) {
        if (ms.length() == 1) return ms + "00";
        if (ms.length() == 2) return ms + "0";
        return ms.substring(0, 3);
    }
}
