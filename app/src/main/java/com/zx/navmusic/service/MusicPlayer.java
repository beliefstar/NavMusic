package com.zx.navmusic.service;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import com.zx.navmusic.common.App;
import com.zx.navmusic.common.LocalAudioStore;
import com.zx.navmusic.common.bean.MusicItem;

import java.io.IOException;

public class MusicPlayer {
    private MediaPlayer mediaPlayer;
    private boolean playerReady = false;

    private Listener listener;


    public MusicPlayer() {
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void play() {
        App.log("player-play");
        if (playerReady) {
            App.log("player-start");
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (playerReady) {
            mediaPlayer.pause();
        }
    }

    public void doPlay(Activity activity, MusicItem mi) {
        tryPlayOnCache(activity, mi);
    }

    private boolean tryPlayOnCache(Activity activity, MusicItem mi) {
        Uri uri = getCache(activity, mi);
        if (uri != null) {
            try {
                playByUri(activity, uri);
                return true;
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    private Uri getCache(Activity activity, MusicItem mi) {
        Uri uri = LocalAudioStore.find(activity, mi.name);
        App.log("[MusicPlayer]cache hit! name:[{}]", mi.name);
        return uri;
    }


    private void playByUri(Activity activity, Uri uri) {
        resetMediaPlayer();
        try {
            mediaPlayer.setDataSource(activity, uri);
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

    public int getCurrentSeek() {
        if (isReady()) {
            return mediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    public boolean isPlaying() {
        if (playerReady) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    public void load(MusicItem mi) {
        App.log("player-load {}", mi);
        doPlay(App.MainActivity, mi);
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

    public interface Listener {
        void onCompleted();
        void onReady();
    }
}
