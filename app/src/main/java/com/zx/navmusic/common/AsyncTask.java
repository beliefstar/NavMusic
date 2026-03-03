package com.zx.navmusic.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import cn.hutool.core.thread.RejectPolicy;

public class AsyncTask {

    public static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(3, RejectPolicy.BLOCK.getValue());


    public static CompletableFuture<Void> run(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, EXECUTOR);
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }

    public static void delay(Runnable task, long delayMillis) {
        EXECUTOR.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }
}
