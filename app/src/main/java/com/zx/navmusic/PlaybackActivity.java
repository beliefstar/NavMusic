package com.zx.navmusic;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.zx.navmusic.common.Constants;
import com.zx.navmusic.common.SeekBarControl;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.databinding.ActivityPlaybackBinding;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.Locale;

public class PlaybackActivity extends AppCompatActivity {

    private ActivityPlaybackBinding binding;

    private ObjectAnimator rotation;
    private SeekBarControl seekBarControl;

    private final NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onMusicStateChange(MusicPlayState playState) {
            render(playState);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        binding = ActivityPlaybackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bindEvent();
        initCircularImage();

        MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
        render(musicPlayState);

        NotifyCenter.registerListener(notifyListener);
        seekBarControl = new SeekBarControl(binding.sbPlaySeekBar);
        seekBarControl.start();
    }

    private void bindEvent() {
        binding.btnPlay.setOnClickListener(this::playPauseClick);
        binding.btnPreviousMusic.setOnClickListener(this::previousMusicClick);
        binding.btnNextMusic.setOnClickListener(this::nextMusicClick);
        binding.btnPlayModeLinear.setOnClickListener(v ->
                this.changePlayMode(PlayModeStrategy.LINEAR));
        binding.btnPlayModeSingle.setOnClickListener(v ->
                this.changePlayMode(PlayModeStrategy.SINGLE));
        binding.btnPlayModeRandom.setOnClickListener(v ->
                this.changePlayMode(PlayModeStrategy.RANDOM));

        binding.btnMoveBack.setOnClickListener(this::moveMain);
    }

    private void render(MusicPlayState musicPlayState) {
        if (musicPlayState == null) {
            return;
        }

        // 按钮
        int icResId = musicPlayState.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), icResId, getTheme());
        binding.btnPlay.setBackground(drawable);

        renderButtonActiveColor(binding.btnPlayModeLinear, R.drawable.ic_play_mode_linear,
                musicPlayState.playSwitchStrategy == PlayModeStrategy.LINEAR);

        renderButtonActiveColor(binding.btnPlayModeSingle, R.drawable.ic_play_mode_single,
                musicPlayState.playSwitchStrategy == PlayModeStrategy.SINGLE);

        renderButtonActiveColor(binding.btnPlayModeRandom, R.drawable.ic_play_mode_random,
                musicPlayState.playSwitchStrategy == PlayModeStrategy.RANDOM);

        binding.tvMusicName.setText(musicPlayState.name);
        binding.tvArtistName.setText(musicPlayState.artist);
        binding.sbPlaySeekBar.setMax(musicPlayState.duration);
        binding.tvPlayCurrentDuration.setText(formatTime(musicPlayState.duration));

        MusicItem musicItem = MusicService.INSTANCE.getNextInfo();
        if (musicItem != null) {
            binding.tvNextPlayInfo.setText(getResources().getText(R.string.next_play) + musicItem.name);
        }

        if (musicPlayState.isPlaying) {
            if (rotation.isPaused()) {
                rotation.resume();
            } else {
                rotation.start();
            }
        } else {
            if (rotation.isRunning()) {
                rotation.pause();
            }
        }
    }

    private void renderButtonActiveColor(View view, int icResId, boolean active) {
        int color = active ? Constants.ACTIVE_COLOR : Constants.NORMAL_COLOR;

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), icResId, getTheme());
        if (drawable != null) {
            Drawable mutate = DrawableCompat.wrap(drawable).mutate();
            mutate.setTint(color);
            view.setBackground(drawable);
        }
    }

    private void moveMain(View view) {
        finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    private void initCircularImage() {
        ImageView imageView = binding.ivCircularImage;

        // 设置圆形图片
        Glide.with(this)
                .load(R.drawable.nav_logo)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);

        // 使用 ObjectAnimator 创建旋转动画
        rotation = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
        rotation.setDuration(5000);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setInterpolator(new LinearInterpolator());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotifyCenter.unregisterListener(notifyListener);
        seekBarControl.pause();
        rotation.cancel();
        binding = null;
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}