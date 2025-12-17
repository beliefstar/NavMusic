package com.zx.navmusic.common;

import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;

import com.zx.navmusic.MusicService;

import java.util.concurrent.atomic.AtomicBoolean;

public class SeekBarControl implements Runnable, SeekBar.OnSeekBarChangeListener {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean disable = new AtomicBoolean(false);

    private final SeekBar seekBar;

    public SeekBarControl(SeekBar seekBar) {
        this.seekBar = seekBar;
        seekBar.setOnSeekBarChangeListener(this);
    }

    public void pause() {
        if (disable.compareAndSet(true, false)) {
            handler.removeCallbacks(this);
        }
    }

    public void start() {
        if (disable.compareAndSet(false, true)) {
            handler.post(this);
        }
    }

    @Override
    public void run() {
        if (MusicService.INSTANCE != null && disable.get()) {
            int seek = MusicService.INSTANCE.getCurrentSeek();
            seekBar.setProgress(seek);

            // 每100毫秒更新一次
            handler.postDelayed(this, 100);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            MusicService.INSTANCE.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // 用户开始拖动时暂停更新
        pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // 用户停止拖动后恢复更新
        start();
    }
}