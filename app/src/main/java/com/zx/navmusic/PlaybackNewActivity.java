package com.zx.navmusic;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.AsyncTask;
import com.zx.navmusic.common.SeekBarControl;
import com.zx.navmusic.common.bean.LyricLine;
import com.zx.navmusic.databinding.ActivityPlaybackNewBinding;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.strategy.PlayModeStrategy;
import com.zx.navmusic.util.LyricParser;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import cn.hutool.core.util.StrUtil;

/**
 * 说明：
 * 1. Play ↔ Pause 图标 Morph（缩放 + 旋转切换）
 * 2. 播放按钮按压发光回弹（Spring）
 * 3. 模拟频谱抖动（假数据）
 * 4. 光环动态渐变 Shader（随播放变化）
 */
public class PlaybackNewActivity extends AppCompatActivity {

    private ActivityPlaybackNewBinding binding;

    private ObjectAnimator discRotateAnimator;
    private AnimatorSet haloBreathAnimator;
    private ValueAnimator spectrumAnimator;

    private boolean isPlaying = false;
    private final Random random = new Random();

    private RadialGradient haloGradient;
    private ShapeDrawable haloDrawable;

    private static final int MODE_ORDER = 0;
    private static final int MODE_LOOP = 1;
    private static final int MODE_SHUFFLE = 2;

    private int currentMode = MODE_ORDER;

    private SeekBarControl seekBarControl;
    private final NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onMusicStateChange(MusicPlayState playState) {
            render(playState);
        }
    };

    private int currentLyricIndex = 0;
    private String currentMusicId = null;
    private ReentrantLock lyricLock = new ReentrantLock();
    private Handler lyricHandler = new Handler(Looper.getMainLooper());
    private List<LyricLine> lyrics;
    private LyricAdapter lyricAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaybackNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        binding.rvLyrics.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        );
        binding.rvLyrics.setItemAnimator(null); // 防止高亮闪烁（推荐）

        initDiscRotate();
        initHaloBreath();
        initHaloShader();
        initPlayButtonSpring();
        initFakeSpectrum();
        initClickEvents();

        MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
        render(musicPlayState);

        NotifyCenter.registerListener(notifyListener);
        seekBarControl = new SeekBarControl(binding.seekProgress);
        seekBarControl.start();

        startFakeLyricProgress();
    }

    private void render(MusicPlayState musicPlayState) {
        if (musicPlayState == null) {
            return;
        }

        System.out.println("===>: " + JSON.toJSONString(musicPlayState, JSONWriter.Feature.PrettyFormat));

        isPlaying = musicPlayState.isPlaying;
        togglePlayState();

        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.LINEAR) {
            setPlayMode(MODE_ORDER);
        }
        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.SINGLE) {
            setPlayMode(MODE_LOOP);
        }
        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.RANDOM) {
            setPlayMode(MODE_SHUFFLE);
        }

        binding.tvTitle.setText(musicPlayState.name);
        binding.tvArtist.setText(musicPlayState.artist);
        binding.seekProgress.setMax(musicPlayState.duration);

        String musicId = musicPlayState.id;
        if (!StrUtil.equals(currentMusicId, musicId) && lyricLock.tryLock()) {
            App.log("开始加载歌词 before={} musicId={}", currentMusicId, musicId);
            currentMusicId = musicId;
            AsyncTask.run(() -> {
                CompletableFuture<List<String>> future = MusicLiveProvider.getInstance().getItemLyric(musicId);
                future.whenComplete((c, ex) -> {
                    if (ex == null && c != null) {
                        getMainExecutor().execute(() -> {
                            this.lyrics = LyricParser.parseLrc(c);
                            currentLyricIndex = 0;
                            lyricAdapter = new LyricAdapter();
                            binding.rvLyrics.setAdapter(lyricAdapter);
                            binding.rvLyrics.bringToFront();
                        });
                    }
                    lyricLock.unlock();
                });
            });
        }
    }

    private void initClickEvents() {
        binding.btnPlay.setOnClickListener(this::playPauseClick);
        binding.btnPlayPrev.setOnClickListener(this::previousMusicClick);
        binding.btnPlayNext.setOnClickListener(this::nextMusicClick);
        binding.btnModeOrder.setOnClickListener(v -> playModeClick(MODE_ORDER, v));
        binding.btnModeLoop.setOnClickListener(v -> playModeClick(MODE_LOOP, v));
        binding.btnModeShuffle.setOnClickListener(v -> playModeClick(MODE_SHUFFLE, v));
    }

    private void playPauseClick(View view) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY_PAUSE);
        startForegroundService(intent);
    }
    private void previousMusicClick(View view) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_PREVIOUS);
        startForegroundService(intent);
    }

    private void nextMusicClick(View view) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_NEXT);
        startForegroundService(intent);
    }

    private void changePlayMode(int type) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY_MODE);
        intent.putExtra(MusicService.ACTION_PLAY_MODE, type);
        startForegroundService(intent);
    }

    private void playModeClick(int mode, View view) {
        if (currentMode == mode) return;

        if (mode == MODE_ORDER) {
            changePlayMode(PlayModeStrategy.LINEAR);
        } else if (mode == MODE_LOOP) {
            changePlayMode(PlayModeStrategy.SINGLE);
        } else if (mode == MODE_SHUFFLE) {
            changePlayMode(PlayModeStrategy.RANDOM);
        }

        setPlayMode(mode);
        playSpringAnim(view);
    }

    private void playSpringAnim(View view) {
        view.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(80)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .setInterpolator(new OvershootInterpolator(4f))
                                .start()
                )
                .start();
    }


    private void setPlayMode(int mode) {
        currentMode = mode;

        binding.btnModeOrder.setSelected(mode == 0);
        binding.btnModeLoop.setSelected(mode == 1);
        binding.btnModeShuffle.setSelected(mode == 2);

        binding.btnModeOrder.setBackgroundResource(
                mode == 0 ? R.drawable.playback_bg_mode_active : R.drawable.playback_bg_mode_inactive);
        binding.btnModeLoop.setBackgroundResource(
                mode == 1 ? R.drawable.playback_bg_mode_active : R.drawable.playback_bg_mode_inactive);
        binding.btnModeShuffle.setBackgroundResource(
                mode == 2 ? R.drawable.playback_bg_mode_active : R.drawable.playback_bg_mode_inactive);
    }

    /* ---------------- 唱片旋转 ---------------- */
    private void initDiscRotate() {
        discRotateAnimator = ObjectAnimator.ofFloat(binding.ivDisc, View.ROTATION, 0f, 360f);
        discRotateAnimator.setDuration(12000);
        discRotateAnimator.setInterpolator(new LinearInterpolator());
        discRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    /* ---------------- 光环呼吸 ---------------- */
    private void initHaloBreath() {
        ObjectAnimator sx = ObjectAnimator.ofFloat(binding.viewHalo, View.SCALE_X, 1f, 1.12f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(binding.viewHalo, View.SCALE_Y, 1f, 1.12f);
        ObjectAnimator a = ObjectAnimator.ofFloat(binding.viewHalo, View.ALPHA, 0.35f, 0.85f);

        for (ObjectAnimator oa : new ObjectAnimator[]{sx, sy, a}) {
            oa.setRepeatCount(ValueAnimator.INFINITE);
            oa.setRepeatMode(ValueAnimator.REVERSE);
        }

        haloBreathAnimator = new AnimatorSet();
        haloBreathAnimator.setDuration(1800);
        haloBreathAnimator.setInterpolator(new LinearInterpolator());
        haloBreathAnimator.playTogether(sx, sy, a);
        haloBreathAnimator.start();
    }

    /* ---------------- 光环动态 Shader ---------------- */
    private void initHaloShader() {
        haloDrawable = new ShapeDrawable(new OvalShape());
        binding.viewHalo.setBackground(haloDrawable);

        ValueAnimator shaderAnimator = ValueAnimator.ofFloat(0f, 1f);
        shaderAnimator.setDuration(4000);
        shaderAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shaderAnimator.addUpdateListener(animation -> {
            float p = (float) animation.getAnimatedValue();
            int c1 = Color.HSVToColor(new float[]{260 + 60 * p, 0.8f, 1f});
            int c2 = Color.HSVToColor(new float[]{200 + 60 * p, 0.9f, 1f});

            int width = binding.viewHalo.getWidth();
            if (width == 0) {
                return;
            }

            haloGradient = new RadialGradient(
                    binding.viewHalo.getWidth() / 2f,
                    binding.viewHalo.getHeight() / 2f,
                    binding.viewHalo.getWidth() / 2f,
                    new int[]{c1, c2, Color.TRANSPARENT},
                    new float[]{0f, 0.6f, 1f},
                    Shader.TileMode.CLAMP
            );
            haloDrawable.getPaint().setShader(haloGradient);
            binding.viewHalo.invalidate();
        });
        shaderAnimator.start();
    }

    /* ---------------- Play ↔ Pause Morph ---------------- */
    private void togglePlayState() {
        // 图标 Morph（缩放 + 旋转）
        binding.btnPlay.animate()
                .rotationBy(180f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(120)
                .withEndAction(() -> {
                    binding.btnPlay.setImageResource(
                            isPlaying ? R.drawable.playback_ic_pause_glow : R.drawable.playback_ic_play_glow);
                    binding.btnPlay.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start();
                })
                .start();

        if (isPlaying) {
            discRotateAnimator.start();
            spectrumAnimator.start();
        } else {
            discRotateAnimator.pause();
            spectrumAnimator.cancel();
        }
    }

    /* ---------------- 按压发光回弹（Spring） ---------------- */
    private void initPlayButtonSpring() {
        SpringAnimation scaleX = new SpringAnimation(binding.btnPlay, SpringAnimation.SCALE_X, 1f);
        SpringAnimation scaleY = new SpringAnimation(binding.btnPlay, SpringAnimation.SCALE_Y, 1f);

        SpringForce force = new SpringForce(1f);
        force.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        force.setStiffness(SpringForce.STIFFNESS_LOW);

        scaleX.setSpring(force);
        scaleY.setSpring(force);

        binding.btnPlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                scaleX.start();
                scaleY.start();
            }
            return false;
        });
    }

    /* ---------------- 模拟频谱抖动 ---------------- */
    private void initFakeSpectrum() {
        spectrumAnimator = ValueAnimator.ofFloat(0f, 1f);
        spectrumAnimator.setDuration(120);
        spectrumAnimator.setRepeatCount(ValueAnimator.INFINITE);
        spectrumAnimator.addUpdateListener(a -> {
            float energy = 0.9f + random.nextFloat() * 0.3f;
            binding.ivDisc.setScaleX(energy);
            binding.ivDisc.setScaleY(energy);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        discRotateAnimator.cancel();
        haloBreathAnimator.cancel();
        spectrumAnimator.cancel();
        seekBarControl.pause();
    }

    private void startFakeLyricProgress() {
        lyricHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                int seek = MusicService.INSTANCE.getCurrentSeek();
                updateLyricByTime(seek);
                lyricHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void updateLyricByTime(long timeMs) {
        if (lyrics == null || lyrics.isEmpty()) return;

        for (int i = 0; i < lyrics.size(); i++) {
            if (timeMs < lyrics.get(i).timeMs) {
                setCurrentLyric(Math.max(0, i - 1));
                return;
            }
        }
    }

    private void setCurrentLyric(int index) {
        if (index == currentLyricIndex) return;

        currentLyricIndex = index;
        lyricAdapter.currentIndex = index;
        lyricAdapter.notifyDataSetChanged();

        scrollLyricToCenter(index);
    }

    private void scrollLyricToCenter(int index) {
        RecyclerView.LayoutManager lm = binding.rvLyrics.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;

        LinearLayoutManager llm = (LinearLayoutManager) lm;

        int rvHeight = binding.rvLyrics.getHeight();
        int itemHeight = binding.rvLyrics.getChildAt(0) != null
                ? binding.rvLyrics.getChildAt(0).getHeight()
                : 0;

        int offset = rvHeight / 2 - itemHeight / 2;

        App.log("scrollLyricToCenter index={} rvHeight={} itemHeight={} offset={}", index, rvHeight, itemHeight, offset);
        llm.scrollToPositionWithOffset(index, offset);
    }

    class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.VH> {
        int currentIndex = 0;

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lyric, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            h.tv.setText(lyrics.get(pos).text);

            int diff = Math.abs(pos - currentIndex);

            LinearGradient gradient = new LinearGradient(
                    0, 0, h.tv.getWidth(), 0,
                    new int[]{Color.CYAN, Color.MAGENTA},
                    null,
                    Shader.TileMode.CLAMP
            );
            h.tv.getPaint().setShader(gradient);

            if (diff == 0) {
                // 当前歌词
                h.tv.setTextColor(Color.parseColor("#00E5FF"));
                h.tv.setTextSize(20);
//                h.tv.setAlpha(1f);
//                h.tv.setScaleX(1.08f);
//                h.tv.setScaleY(1.08f);

                h.tv.animate()
                    .alpha(1f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(180)
                    .start();

            } else {
                float alpha = Math.max(0.25f, 1f - diff * 0.2f);
                float scale = Math.max(0.9f, 1f - diff * 0.05f);

                h.tv.setTextColor(Color.parseColor("#88FFFFFF"));
                h.tv.setTextSize(16);
                h.tv.setAlpha(alpha);
                h.tv.setScaleX(scale);
                h.tv.setScaleY(scale);
            }
        }

        @Override
        public int getItemCount() {
            return lyrics.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View v) {
                super(v);
                tv = v.findViewById(R.id.tvLyric);
            }
        }
    }
}
