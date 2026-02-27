package com.zx.navmusic.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import com.zx.navmusic.common.App;
import com.zx.navmusic.common.Util;
import com.zx.navmusic.common.bean.MusicItem;

import java.io.IOException;

public class MusicPlayer {
    private MediaPlayer mediaPlayer;
    private boolean playerReady = false;
    private boolean autoPlay = false;
    private final Context ctx;

    private Listener listener;


    public MusicPlayer(Context ctx) {
        this.ctx = ctx;
        resetMediaPlayer();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        autoPlay = true;
        if (isReady() && !isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        autoPlay = false;
        if (isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public int getCurrentSeek() {
        if (isReady()) {
            return mediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    public boolean isPlaying() {
        return isReady() && mediaPlayer.isPlaying();
    }

    public void load(MusicItem mi) {
        App.log("player-load {}", mi);
        doPlay(mi);
    }

    public void play(MusicItem mi) {
        autoPlay = true;
        doPlay(mi);
    }

    public boolean isReady() {
        return playerReady;
    }

    public int getCurrentDuration() {
        if (isReady()) {
            return mediaPlayer.getDuration();
        }
        return -1;
    }

    public void seekTo(int progress) {
        if (mediaPlayer != null && isReady()) {
            mediaPlayer.seekTo(progress);
        }
    }

    private void doPlay(MusicItem mi) {
        Uri uri = Util.getFileUri(ctx, mi.name);
        if (uri != null) {
            try {
                playByUri(ctx, uri);
            } catch (Exception ignore) {
            }
        }
    }

    private void playByUri(Context ctx, Uri uri) {
        resetMediaPlayer();
        try {
            mediaPlayer.setDataSource(ctx, uri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            App.log("[MusicPlayer]playByUri Error [{}]", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private synchronized void resetMediaPlayer() {
        playerReady = false;
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setOnPreparedListener(this::onPlayerPrepared);
            mediaPlayer.setOnErrorListener(this::onMediaPlayerError);
            mediaPlayer.setOnCompletionListener(this::onMediaPlayerCompletion);
        } else {
            mediaPlayer.reset();
        }
    }

    private void onPlayerPrepared(MediaPlayer mp) {
        playerReady = true;
        if (autoPlay) {
            start();
        }
        if (listener != null) {
            listener.onReady();
        }
    }

    private void onMediaPlayerCompletion(MediaPlayer mp) {
        if (listener != null) {
            listener.onCompleted();
        }
    }

    private boolean onMediaPlayerError(MediaPlayer mp, int what, int extra) {
        App.toast(App.MainActivity, "播放失败");
        return false;
    }


    public void destroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    public interface Listener {
        void onCompleted();
        void onReady();
    }
}
