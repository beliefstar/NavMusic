package com.zx.navmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
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
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.MusicPlayer;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;

public class MusicService extends Service {
    public static final int NOTIFICATION_ID = 6528;
    public static final String CHANNEL_ID = "navmusic_notification_channel";

    public static final String ACTION_PLAY = "nav_action_play";
    public static final String ACTION_PLAY_INDEX = "nav_action_play_index";
    public static final String ACTION_PLAY_ID = "nav_action_play_id";
    public static final String ACTION_PAUSE = "nav_action_pause";
    public static final String ACTION_PLAY_PAUSE = "nav_action_play_pause";
    public static final String ACTION_NEXT = "nav_action_next";
    public static final String ACTION_PREVIOUS = "nav_action_previous";
    public static final String ACTION_PLAY_MODE = "nav_action_play_mode";

    public final LinearPlayStrategy linearPlayStrategy = new LinearPlayStrategy();
    public final RandomPlayStrategy randomPlayStrategy = new RandomPlayStrategy();
    public final SingleLoopPlayStrategy singleLoopPlayStrategy = new SingleLoopPlayStrategy();

    private MediaSessionCompat mediaSession;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManager;
    private MusicPlayer musicPlayer;
    private MusicLiveProvider musicProvider;
    private PlaySwitchStrategy playSwitchStrategy;
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
            musicPlayer.load(musicItems.get(0));
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
        musicProvider = MusicLiveProvider.getInstance();
        playSwitchStrategy = randomPlayStrategy;
        musicPlayState = new MusicPlayState();

        musicPlayer.setListener(playerListener);
        musicProvider.observeForever(initer);
        NotifyCenter.registerListener(notifyListener);
        initChannel();
        musicProvider.refresh(this);

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        App.log("[MusicService] - onStartCommand - {}", intent.getAction());
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
                setPlaySwitchStrategy(type);
                break;
            case ACTION_PLAY_PAUSE:
                if (musicPlayer.isPlaying()) {
                    pause();
                } else {
                    play();
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
            index = musicProvider.getIndexById(playId);
            play(index);
            return;
        }
        play();
    }

    private void setPlaySwitchStrategy(int switchStrategyType) {
        int cursIndex = playSwitchStrategy.getCursIndex();
        switch (switchStrategyType) {
            case PlaySwitchStrategy.LINEAR:
                playSwitchStrategy = linearPlayStrategy;
                break;
            case PlaySwitchStrategy.RANDOM:
                playSwitchStrategy = randomPlayStrategy;
                break;
            case PlaySwitchStrategy.LOOP:
                playSwitchStrategy = singleLoopPlayStrategy;
                break;
        }
        playSwitchStrategy.setCursIndex(cursIndex);
    }

    private void updateNotification() {
//        ServiceCompat.startForeground(this, NOTIFICATION_ID, notificationBuilder.build(),
//                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
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
        MusicItem item = musicProvider.getItem(index);
        if (item != null) {
            autoPlay = true;
            musicPlayer.load(item);
            playSwitchStrategy.setCursIndex(index);
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
        int index = playSwitchStrategy.next();
        MusicItem item = musicProvider.getItem(index);
        if (item != null) {
            musicPlayer.load(item);
            playSwitchStrategy.setCursIndex(index);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private void previous() {
        int index = playSwitchStrategy.previous();
        MusicItem item = musicProvider.getItem(index);
        if (item != null) {
            musicPlayer.load(item);
            playSwitchStrategy.setCursIndex(index);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private MusicPlayState buildMusicPlayState() {
        musicPlayState.reset();

        musicPlayState.isPlaying = musicPlayer.isPlaying();
        musicPlayState.playSwitchStrategy = playSwitchStrategy.getType();
        musicPlayState.index = playSwitchStrategy.getCursIndex();
        MusicItem item = musicProvider.getItem(playSwitchStrategy.getCursIndex());
        if (item != null) {
            musicPlayState.name = item.name;
        }
        if (musicPlayer.isReady()) {
            musicPlayState.duration = musicPlayer.getCurrentDuration();
            musicPlayState.position = musicPlayer.getCurrentSeek();
        }
        return musicPlayState;
    }

    private NotificationCompat.Builder initNotificationBuilder() {
        Icon largeIcon = IconCompat.createWithResource(getApplicationContext(), R.drawable.ic_empty_music2).toIcon(getApplicationContext());
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NavMusic")
                .setContentText("")
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
//                .setContentIntent()
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
        });

        // 激活 MediaSession
        mediaSession.setActive(true);
        return mediaSession;
    }

    private void updateMediaSession(MusicPlayState musicPlayState) {
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "")
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
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface PlaySwitchStrategy {
        int LINEAR = 0;
        int RANDOM = 1;
        int LOOP = 2;
        int getType();
        int getCursIndex();
        void setCursIndex(int cursPosition);
        int previous();
        int next();
    }

    private static abstract class AbsPlaySwitchStrategy implements PlaySwitchStrategy {
        protected int position;
        @Override
        public int getCursIndex() {
            return position;
        }

        @Override
        public void setCursIndex(int cursPosition) {
            position = cursPosition;
        }
    }

    public class LinearPlayStrategy extends AbsPlaySwitchStrategy {
        @Override
        public int getType() {
            return LINEAR;
        }

        @Override
        public int previous() {
            return Math.max(position - 1, 0);
        }

        @Override
        public int next() {
            return (position + 1) % musicProvider.count();
        }
    }

    public class RandomPlayStrategy extends AbsPlaySwitchStrategy {
        private final ArrayDeque<Integer> playedQueue = new ArrayDeque<>();
        private final Set<Integer> set = new ConcurrentHashSet<>();
        private final Random random = new Random();
        @Override
        public int getType() {
            return RANDOM;
        }

        @Override
        public void setCursIndex(int cursPosition) {
            super.setCursIndex(cursPosition);
            if (!playedQueue.isEmpty() && playedQueue.peekLast().equals(cursPosition)) {
                App.log("playedQueue: {}", playedQueue);
                return;
            }
            playedQueue.offerLast(cursPosition);
            set.add(cursPosition);
            cut();
            App.log("playedQueue: {}", playedQueue);
        }

        @Override
        public int previous() {
            if (playedQueue.isEmpty()) {
                return next();
            }
            Integer removed = playedQueue.pollLast();
            set.remove(removed);
            if (!playedQueue.isEmpty()) {
                return playedQueue.peekLast();
            }
            return next();
        }

        @Override
        public int next() {
            return randomPosition();
        }

        private void cut() {
            while (playedQueue.size() > 10) {
                playedQueue.pollFirst();
            }
        }

        private int randomPosition() {
            int r;
            do {
                r = random.nextInt(musicProvider.count());
            } while (set.contains(r));
            return r;
        }
    }

    public class SingleLoopPlayStrategy extends AbsPlaySwitchStrategy {
        @Override
        public int getType() {
            return LOOP;
        }

        @Override
        public int previous() {
            return position;
        }

        @Override
        public int next() {
            return position;
        }
    }
}