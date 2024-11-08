package com.zx.navmusic.common;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;

public class SignatureUtil {

    public static String buildUrl(String url, String token) {
        String nonce = UUID.randomUUID().toString();
        String timestamp = Long.toString(System.currentTimeMillis());
        String signature = buildSignature(nonce, timestamp, token);

        List<String> list = new ArrayList<>();
        list.add("nonce=" + nonce);
        list.add("timestamp=" + timestamp);
        list.add("signature=" + signature);
        String queryParam = String.join("&", list);
        if (url.contains("?")) {
            return url + "&" + queryParam;
        }
        return url + "?" + queryParam;
    }

    public static HttpRequest touchHeader(HttpRequest request, String token) {
        String nonce = UUID.randomUUID().toString();
        String timestamp = Long.toString(System.currentTimeMillis());
        String signature = buildSignature(nonce, timestamp, token);

        return request.header("nonce", nonce)
                .header("timestamp", timestamp)
                .header("signature", signature);
    }

    private static String buildSignature(String nonce, String timestamp, String token) {
        List<String> list = new ArrayList<>();
        list.add(token);
        if (StrUtil.isNotEmpty(nonce)) {
            list.add(nonce);
        }
        if (StrUtil.isNotEmpty(timestamp)) {
            list.add(timestamp);
        }
        Collections.sort(list);
        StringBuilder signatureBuilder = new StringBuilder();
        for (String s : list) {
            signatureBuilder.append(s);
        }
        return DigestUtils.sha256Hex(signatureBuilder.toString());
    }
}
