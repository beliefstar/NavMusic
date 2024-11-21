package com.zx.navmusic;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class MediaReceiver extends BroadcastReceiver {
    public static final String ACTION_PLAY_PAUSE = "nav_music_global_action_play_pause";
    public static final String ACTION_PREV = "nav_music_global_action_prev";
    public static final String ACTION_NEXT = "nav_music_global_action_next";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PLAY_PAUSE.equals(action)) {
            // 处理播放/暂停
            intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY_PAUSE);
            context.startForegroundService(intent);
        } else if (ACTION_PREV.equals(action)) {
            // 处理上一曲
            intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PREVIOUS);
            context.startForegroundService(intent);
        } else if (ACTION_NEXT.equals(action)) {
            // 处理下一曲
            intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_NEXT);
            context.startForegroundService(intent);
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            // 蓝牙连接
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            // 蓝牙断开连接
            intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PAUSE);
            context.startForegroundService(intent);
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            // 蓝牙断开连接
            intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PAUSE);
            context.startForegroundService(intent);
        }
    }
}
