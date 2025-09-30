package com.zx.navmusic;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.Observer;

import com.zx.navmusic.common.App;
import com.zx.navmusic.common.Util;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.MusicPlayer;
import com.zx.navmusic.service.strategy.PlayModeFactory;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.List;

import cn.hutool.core.util.StrUtil;

public class MusicService extends Service {
    public static final int NOTIFICATION_ID = 6528;
    public static final String CHANNEL_ID = "navmusic_notification_channel";

    public static MusicService INSTANCE;

    public static final String ACTION_PLAY = "nav_action_play";
    public static final String ACTION_PLAY_INDEX = "nav_action_play_index";
    public static final String ACTION_PLAY_ID = "nav_action_play_id";
    public static final String ACTION_PAUSE = "nav_action_pause";
    public static final String ACTION_PLAY_PAUSE = "nav_action_play_pause";
    public static final String ACTION_NEXT = "nav_action_next";
    public static final String ACTION_PREVIOUS = "nav_action_previous";
    public static final String ACTION_PLAY_MODE = "nav_action_play_mode";

    private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleAction(intent);
        }
    };

    private MediaSessionCompat mediaSession;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManager;
    private MusicPlayer musicPlayer;
    private PlayModeStrategy playModeStrategy;
    private MusicPlayState musicPlayState;
    private boolean init = false;
    private boolean autoPlay = false;

    private final NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onMusicStateChange(MusicPlayState playState) {
            updateMediaSession(playState);
            updateNotification();
        }
    };

    private final Observer<List<MusicItem>> initer = musicItems -> {
        if (musicItems == null) {
            return;
        }
        if (!init && !musicItems.isEmpty()) {
            int curPos = playModeStrategy.getCurPos();
            musicPlayer.load(musicItems.get(curPos));
        }
        init = true;
    };

    private final MusicPlayer.Listener playerListener = new MusicPlayer.Listener() {

        @Override
        public void onCompleted() {
            next();
        }

        @Override
        public void onReady() {
            if (autoPlay) {
                App.log("onReady-play");
                play();
            }
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    };

    public MusicService() {
    }

    @Override
    public void onCreate() {
        App.log("[MusicService] - onCreate");
        mediaSession = initMediaSession();
        notificationBuilder = initNotificationBuilder();
        musicPlayer = new MusicPlayer();
        playModeStrategy = PlayModeFactory.get(PlayModeStrategy.RANDOM);
        musicPlayState = new MusicPlayState();

        musicPlayer.setListener(playerListener);
        NotifyCenter.registerListener(notifyListener);
        initChannel();

        MusicLiveProvider musicProvider = getMusicProvider();
        musicProvider.observeForever(initer);
        musicProvider.refresh(this);

        INSTANCE = this;

        // 注册广播事件
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_PLAY_MODE);
        intentFilter.addAction(ACTION_PLAY_PAUSE);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(intentReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        App.log("[MusicService] - onStartCommand - {}", intent.getAction());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        handleAction(intent);
        updateNotification();
        return START_NOT_STICKY;
    }

    private void handleAction(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (StrUtil.isBlank(action)) {
            return;
        }
        App.log("[MusicService] - handleAction - {}", action);
        switch (action) {
            case ACTION_PLAY:
                handlePlayAction(intent);
                break;
            case ACTION_PAUSE:
                pause();
                break;
            case ACTION_NEXT:
                next();
                break;
            case ACTION_PREVIOUS:
                previous();
                break;
            case ACTION_PLAY_MODE:
                int type = intent.getIntExtra(ACTION_PLAY_MODE, 0);
                setPlayModeStrategy(type);
                break;
            case ACTION_PLAY_PAUSE:
                if (musicPlayer.isPlaying()) {
                    pause();
                } else {
                    play();
                }
                break;
            case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                if (musicPlayer.isPlaying()) {
                    pause();
                }
        }
    }

    private void handlePlayAction(Intent intent) {
        int index = intent.getIntExtra(ACTION_PLAY_INDEX, -1);
        if (index >= 0) {
            play(index);
            return;
        }
        String playId = intent.getStringExtra(ACTION_PLAY_ID);
        if (StrUtil.isNotBlank(playId)) {
            index = getMusicProvider().getIndexById(playId);
            play(index);
            return;
        }
        play();
    }

    private MusicLiveProvider getMusicProvider() {
        return MusicLiveProvider.getInstance();
    }

    private void setPlayModeStrategy(int switchStrategyType) {
        if (switchStrategyType == playModeStrategy.getType()) {
            return;
        }
        int curPos = playModeStrategy.getCurPos();
        playModeStrategy = PlayModeFactory.get(switchStrategyType);
        playModeStrategy.resetPos(curPos);
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
    }

    private void updateNotification() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void initChannel() {
        notificationManager = NotificationManagerCompat.from(this);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "NavMusic", NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }

    private void play(int index) {
        MusicItem item = getMusicProvider().getItem(index);
        if (item != null) {
            autoPlay = true;
            musicPlayer.load(item);
            playModeStrategy.resetPos(index);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private void play() {
        if (musicPlayer.isPlaying()) {
            return;
        }
        autoPlay = true;
        musicPlayer.play();
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
    }

    private void pause() {
        if (!musicPlayer.isPlaying()) {
            autoPlay = false;
            return;
        }
        musicPlayer.pause();
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
    }

    private void next() {
        int index = playModeStrategy.next();
        MusicItem item = getMusicProvider().getItem(index);
        if (item != null) {
            musicPlayer.load(item);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private void previous() {
        int index = playModeStrategy.previous();
        MusicItem item = getMusicProvider().getItem(index);
        if (item != null) {
            musicPlayer.load(item);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private MusicPlayState buildMusicPlayState() {
        musicPlayState.reset();

        musicPlayState.isPlaying = musicPlayer.isPlaying();
        musicPlayState.playSwitchStrategy = playModeStrategy.getType();
        musicPlayState.index = playModeStrategy.getCurPos();
        MusicItem item = getMusicProvider().getItem(playModeStrategy.getCurPos());
        if (item != null) {
            musicPlayState.name = item.name;
            Util.parsePlayState(musicPlayState);
        }
        if (musicPlayer.isReady()) {
            musicPlayState.duration = musicPlayer.getCurrentDuration();
            musicPlayState.position = musicPlayer.getCurrentSeek();
        }
        return musicPlayState;
    }

    public int getCurrentSeek() {
        int seek = musicPlayer.getCurrentSeek();
        return Math.max(seek, 0);
    }

    public MusicItem getNextInfo() {
        int idx = playModeStrategy.peekNext();
        return getMusicProvider().getItem(idx);
    }

    private NotificationCompat.Builder initNotificationBuilder() {
        Icon largeIcon = IconCompat.createWithResource(getApplicationContext(), R.drawable.ic_empty_music2).toIcon(getApplicationContext());

        PendingIntent clickIntent = PendingIntent.getActivity(this, 0, Util.intentPlaying(this), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NavMusic")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_settings)
                .setLargeIcon(largeIcon)
                .setContentIntent(clickIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
    }

    private MediaSessionCompat initMediaSession() {
        MediaSessionCompat mediaSession = new MediaSessionCompat(this, App.App_Name);

        // 设置控制器回调，处理播放控制
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                // 处理播放
                App.log("MediaSessionCompat-onPlay");
                play();
            }

            @Override
            public void onPause() {
                // 处理暂停
                App.log("MediaSessionCompat-onPause");
                pause();
            }

            @Override
            public void onSkipToNext() {
                // 处理下一曲
                next();
            }

            @Override
            public void onSkipToPrevious() {
                // 处理上一曲
                previous();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int)pos);
            }
        });

        // 激活 MediaSession
        mediaSession.setActive(true);
        return mediaSession;
    }

    private void updateMediaSession(MusicPlayState musicPlayState) {
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicPlayState.artist)
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, getAlbumArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicPlayState.name)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicPlayState.duration)
//                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getQueuePosition() + 1)
//                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getQueue().length)
//                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenreName())
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .build());

        int state = musicPlayState.isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, musicPlayState.position, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS | ACTION_SEEK_TO)
                .build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void seekTo(int progress) {
        musicPlayer.seekTo(progress);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(intentReceiver);
        musicPlayer.destroy();
        mediaSession.release();
    }
}