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
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.zx.navmusic.album.AlbumHandler;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.Util;
import com.zx.navmusic.common.bean.LyricLine;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.config.ConfigCenter;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.lyric.LyricHandler;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.MusicPlayer;
import com.zx.navmusic.service.strategy.PlayModeFactory;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

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
    public static final String METADATA_KEY_LYRIC = "android.media.metadata.LYRIC";

    private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleAction(intent);
        }
    };

    private final ConcurrentLinkedDeque<Integer> playedMusicQueue = new ConcurrentLinkedDeque<>();

    private MediaSessionCompat mediaSession;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManager;
    private MusicPlayer musicPlayer;
    private PlayModeStrategy playModeStrategy;
    private AlbumHandler albumHandler;
    private LyricHandler lyricHandler;
    private final Handler btLyricHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean btLyricMark = new AtomicBoolean(false);
    private String currentBtLyric = "";
    private String currentBtMusicId;

    private final Runnable btLyricRunnable = new Runnable() {
        @Override
        public void run() {
            if (btLyricMark.get() || ConfigCenter.isBluetoothLyric()) {
                trySyncBluetoothLyric();
            }
            btLyricHandler.postDelayed(this, 300);
        }
    };

    private final NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onMusicStateChange(MusicPlayState playState) {
            updateMediaSession(playState);
            updateNotification();
        }
    };

    private final BiConsumer<Boolean, Throwable> initer = (res, err) -> {
        if (err != null) {
            App.log("PlayModeStrategy listenInit error: {}", err.getMessage());
        } else {
            App.log("PlayModeStrategy listenInit --> {}", getMusicProvider().count());
            int curPos = playModeStrategy.getCurPos();
            MusicItem item = getMusicProvider().getItem(curPos);
            if (item != null) {
                App.log("PlayModeStrategy listenInit success");
                musicPlayer.load(item);
            }
        }
    };

    private final MusicPlayer.Listener playerListener = new MusicPlayer.Listener() {

        @Override
        public void onCompleted() {
            next();
        }

        @Override
        public void onReady() {
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    };

    public MusicService() {
    }

    @Override
    public void onCreate() {
        App.log("[MusicService] - onCreate");
        INSTANCE = this;
        ConfigCenter.create(this);

        mediaSession = initMediaSession();
        notificationBuilder = initNotificationBuilder();
        musicPlayer = new MusicPlayer(this);
        playModeStrategy = PlayModeFactory.get(PlayModeStrategy.RANDOM);
        albumHandler = new AlbumHandler();
        lyricHandler = new LyricHandler();

        playModeStrategy.listenInit().whenComplete(initer);
        musicPlayer.setListener(playerListener);
        NotifyCenter.registerListener(notifyListener);
        getMusicProvider().init(this);
        initChannel();

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

        btLyricHandler.post(btLyricRunnable);
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

    public void triggerMusicStateChange() {
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
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
                int type = intent.getIntExtra(ACTION_PLAY_MODE, PlayModeStrategy.RANDOM);
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
        playModeStrategy.resetPos(curPos, true);
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
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, App.App_Name, NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }

    private void play(int index) {
        MusicItem item = getMusicProvider().getItem(index);
        if (item != null) {
            beforeMusicChange(item);
            if (!musicPlayer.play(item)) {
                next();
                return;
            }
            playModeStrategy.resetPos(index, false);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private void play() {
        musicPlayer.start();
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
    }

    private void pause() {
        musicPlayer.pause();
        NotifyCenter.onMusicStateChange(buildMusicPlayState());
    }

    private void next() {
        int index = playModeStrategy.next();
        MusicItem item = getMusicProvider().getItem(index);
        if (item != null) {
            beforeMusicChange(item);
            addPlayedQueue();
            musicPlayer.load(item);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private void previous() {
        Integer musicIndex = pollPlayedQueue();
        if (musicIndex == null) {
            next();
            return;
        }

        MusicItem item = getMusicProvider().getItem(musicIndex);
        if (item != null) {
            playModeStrategy.resetPos(musicIndex, false);
            beforeMusicChange(item);
            musicPlayer.load(item);
            NotifyCenter.onMusicStateChange(buildMusicPlayState());
        }
    }

    private MusicPlayState buildMusicPlayState() {
        MusicPlayState musicPlayState = new MusicPlayState();

        musicPlayState.isPlaying = musicPlayer.isPlaying();
        musicPlayState.playSwitchStrategy = playModeStrategy.getType();
        musicPlayState.index = playModeStrategy.getCurPos();
        MusicItem item = getMusicProvider().getItem(playModeStrategy.getCurPos());
        if (item != null) {
            musicPlayState.id = item.id;
            musicPlayState.name = item.name;
            musicPlayState.artist = item.artist;
            musicPlayState.album = item.album;
//            Util.parsePlayState(musicPlayState);
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

    public Bitmap getAlbum(String musicId) {
        return albumHandler.getAlbum(musicId);
    }

    public List<LyricLine> getLyric(String musicId) {
        return lyricHandler.getLyric(musicId);
    }

    private void trySyncBluetoothLyric() {
        if (!ConfigCenter.isBluetoothLyric()) {
            if (StrUtil.isNotBlank(currentBtLyric)) {
                currentBtLyric = "";
                currentBtMusicId = null;
                MusicPlayState playState = buildMusicPlayState();
                updateMediaSession(playState);
            }
            btLyricMark.set(false);
            return;
        }
        btLyricMark.set(true);

        if (!musicPlayer.isReady()) {
            return;
        }

        MusicPlayState playState = buildMusicPlayState();
        if (StrUtil.isBlank(playState.id)) {
            return;
        }

        List<LyricLine> lyrics = getLyric(playState.id);
        int currentIndex = LyricHandler.getCurrentIndex(lyrics, playState.position + 300);
        String lyric = "";
        if (lyrics != null && currentIndex >= 0 && currentIndex < lyrics.size()) {
            lyric = lyrics.get(currentIndex).text;
        }

        boolean sameMusic = StrUtil.equals(currentBtMusicId, playState.id);
        boolean sameLyric = StrUtil.equals(currentBtLyric, lyric);
        if (sameMusic && sameLyric) {
            return;
        }

        currentBtMusicId = playState.id;
        currentBtLyric = lyric;
        updateMediaSession(playState);
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
        boolean enableBluetoothLyric = ConfigCenter.isBluetoothLyric() && StrUtil.isNotBlank(currentBtLyric);

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicPlayState.id)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicPlayState.album)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getAlbum(musicPlayState.id))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicPlayState.duration);

        if (enableBluetoothLyric) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentBtLyric);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    StrUtil.format("{}-{}", musicPlayState.name, musicPlayState.artist));

//            metadataBuilder.putString(METADATA_KEY_LYRIC, currentBtLyric);
            // Some head units ignore LYRIC key and read display/title fields only.
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentBtLyric);
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, musicPlayState.name);
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, currentBtLyric);
        } else {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicPlayState.name);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicPlayState.artist);

//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, musicPlayState.name);
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, musicPlayState.artist);
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "");
        }

        mediaSession.setMetadata(metadataBuilder.build());

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
        btLyricHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(intentReceiver);
        musicPlayer.destroy();
        mediaSession.release();
    }

    public void beforeMusicChange(MusicItem in) {
        try {
            MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
            if (musicPlayState == null || musicPlayState.index < 0 || !musicPlayer.isReady()) {
                return;
            }
            int duration = musicPlayer.getCurrentDuration();
            int seek = musicPlayer.getCurrentSeek();
            MusicItem out = getMusicProvider().getItem(musicPlayState.index);
            if (out == null) {
                return;
            }
            onMusicChange(out, in, seek, duration);
        } catch (Exception e) {
            App.log("beforeMusicChange-error {}", e.getMessage());
        }
    }

    public void onMusicChange(MusicItem out, MusicItem in, int seek, int duration) {
        // 切换音乐时调用
        double percent = (duration > 0) ? (double) seek / duration : 0;
        if (percent == 0) {
            return;
        }
        if (percent >= 0.95) {
            out.score += 1;
        }
        App.log("onMusicChange out:{}, score:{}", out.name, out.score);
        MusicLiveProvider.getInstance().refresh();
    }

    private void addPlayedQueue() {
        MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
        if (musicPlayState == null || musicPlayState.index < 0 || !musicPlayer.isReady()) {
            return;
        }
        playedMusicQueue.addLast(musicPlayState.index);
        if (playedMusicQueue.size() > 100) {
            playedMusicQueue.pollFirst();
        }
        App.log("playedMusicQueue-add: {}", playedMusicQueue);
    }

    private Integer pollPlayedQueue() {
        if (playedMusicQueue.isEmpty()) return null;

        Integer index = playedMusicQueue.pollLast();
        App.log("playedMusicQueue-poll: {}, {}", index, playedMusicQueue);
        return index;
    }
}
