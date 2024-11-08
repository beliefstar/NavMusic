package com.zx.navmusic.common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import cn.hutool.core.thread.RejectPolicy;

public class AsyncTask {

    public static final ExecutorService EXECUTOR = new ThreadPoolExecutor(3, 3,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128), RejectPolicy.BLOCK.getValue());


    public static CompletableFuture<Void> run(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, EXECUTOR);
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }
}
