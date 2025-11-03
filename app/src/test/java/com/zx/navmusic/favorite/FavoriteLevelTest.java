package com.zx.navmusic.favorite;

import com.alibaba.fastjson2.JSON;

import junit.framework.TestCase;

public class FavoriteLevelTest extends TestCase {

    public void testFromScore() {
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(4)));
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(5)));
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(6)));
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(15)));
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(25)));
        System.out.println(JSON.toJSONString(FavoriteLevel.fromScore(35)));
    }
}