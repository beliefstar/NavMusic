package com.zx.navmusic.common;

import com.zx.navmusic.service.MusicPlayState;

public class Util {

    public static void parsePlayState(MusicPlayState t) {
        String name = t.name;

        if (name.contains("《") && name.contains("》")) {
            int l = name.indexOf('《');
            int r = name.indexOf('》');
            if (l < r) {
                String artist = name.substring(0, l).trim();
                String title = name.substring(l + 1, r).trim();

                t.name = title;
                t.artist = artist;
            }
        }

        if (name.contains("-")) {
            String[] parts = name.split("-");
            if (parts.length >= 2) {
                String artist = parts[0].trim();
                String part1 = parts[1];
                if (part1.contains(".")) {
                    part1 = part1.substring(0, part1.lastIndexOf('.'));
                }
                String title = part1.trim();

                t.name = title;
                t.artist = artist;
            }
        }
    }
}
