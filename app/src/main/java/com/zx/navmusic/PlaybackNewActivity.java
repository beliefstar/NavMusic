package com.zx.navmusic;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.SeekBarControl;
import com.zx.navmusic.common.bean.LyricLine;
import com.zx.navmusic.databinding.ActivityPlaybackNewBinding;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.lyric.LyricHandler;
import com.zx.navmusic.lyric.LyricParser;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
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

    private AnimatorSet haloBreathAnimator;

    private boolean isPlaying = false;

    private RadialGradient haloGradient;
    private ShapeDrawable haloDrawable;

    private static final int MODE_ORDER = 0;
    private static final int MODE_LOOP = 1;
    private static final int MODE_SHUFFLE = 2;

    private int currentMode = MODE_ORDER;

    private SeekBarControl seekBarControl;
    private ConstraintSet lyricCollapsedSet;
    private ConstraintSet lyricExpandedSet;
    private boolean isLyricExpanded = false;
    private GestureDetector lyricGestureDetector;
    private final NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void onMusicStateChange(MusicPlayState playState) {
            runOnUiThread(() -> render(playState));
        }
    };

    private Handler lyricHandler = new Handler(Looper.getMainLooper());
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

        lyricAdapter = new LyricAdapter();
        binding.rvLyrics.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        );
        binding.rvLyrics.setItemAnimator(null); // 防止高亮闪烁（推荐）
        binding.rvLyrics.setAdapter(lyricAdapter);

        initHaloBreath();
        initHaloShader();
        initPlayButtonSpring();
        initClickEvents();
        initLyricExpandCollapse();

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

        if (isPlaying != musicPlayState.isPlaying) {
            isPlaying = musicPlayState.isPlaying;
            togglePlayState();
        }

        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.LINEAR) {
            setPlayMode(MODE_ORDER);
        }
        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.SINGLE) {
            setPlayMode(MODE_LOOP);
        }
        if (musicPlayState.playSwitchStrategy == PlayModeStrategy.RANDOM) {
            setPlayMode(MODE_SHUFFLE);
        }

        if (binding.ivDisc.getTag(R.id.iv_disc_res_id) == null
                || !StrUtil.equals(binding.ivDisc.getTag(R.id.iv_disc_res_id).toString(), musicPlayState.id)) {
            // 不同歌曲，刷新封面
            Bitmap album = MusicService.INSTANCE.getAlbum(musicPlayState.id);

            Glide.with(this)
                    .load(album)
                    .placeholder(R.drawable.nav_logo)  // 加载中显示的图片
                    .error(R.drawable.nav_logo)  // 加载失败显示的图片
                    .centerCrop()  // 裁剪方式
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(50)))
                    .into(binding.ivDisc);

            if (album != null) {
                binding.ivDisc.setTag(R.id.iv_disc_res_id, musicPlayState.id);
            }
        }

        if (binding.rvLyrics.getTag(R.id.rv_lyric_res_id) == null
                || !StrUtil.equals(binding.rvLyrics.getTag(R.id.rv_lyric_res_id).toString(), musicPlayState.id)) {

            List<LyricLine> lyricLines = MusicService.INSTANCE.getLyric(musicPlayState.id);
            if (CollUtil.isEmpty(lyricLines)) {
                lyricLines = LyricParser.notFount();
            } else {
                binding.rvLyrics.setTag(R.id.rv_lyric_res_id, musicPlayState.id);
            }

            lyricAdapter.changeData(lyricLines);
            binding.rvLyrics.bringToFront();
        }

        binding.tvTitle.setText(musicPlayState.name);
        binding.tvArtist.setText(musicPlayState.artist);
        binding.seekProgress.setMax(musicPlayState.duration);
    }

    private void initClickEvents() {
        binding.btnPlay.setOnClickListener(this::playPauseClick);
        binding.btnPlayPrev.setOnClickListener(this::previousMusicClick);
        binding.btnPlayNext.setOnClickListener(this::nextMusicClick);
        binding.btnModeOrder.setOnClickListener(v -> playModeClick(MODE_ORDER, v));
        binding.btnModeLoop.setOnClickListener(v -> playModeClick(MODE_LOOP, v));
        binding.btnModeShuffle.setOnClickListener(v -> playModeClick(MODE_SHUFFLE, v));
    }

    private void initLyricExpandCollapse() {
        View rootView = binding.getRoot();
        if (!(rootView instanceof ConstraintLayout)) {
            return;
        }

        ConstraintLayout root = (ConstraintLayout) rootView;

        lyricCollapsedSet = new ConstraintSet();
        lyricCollapsedSet.clone(root);

        lyricExpandedSet = new ConstraintSet();
        lyricExpandedSet.clone(root);

        int discSize = dpToPx(72);
        lyricExpandedSet.clear(R.id.iv_disc, ConstraintSet.TOP);
        lyricExpandedSet.clear(R.id.iv_disc, ConstraintSet.BOTTOM);
        lyricExpandedSet.clear(R.id.iv_disc, ConstraintSet.START);
        lyricExpandedSet.clear(R.id.iv_disc, ConstraintSet.END);
        lyricExpandedSet.constrainWidth(R.id.iv_disc, discSize);
        lyricExpandedSet.constrainHeight(R.id.iv_disc, discSize);
        lyricExpandedSet.connect(R.id.iv_disc, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(16));
        lyricExpandedSet.connect(R.id.iv_disc, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dpToPx(16));

        lyricExpandedSet.clear(R.id.tv_title, ConstraintSet.TOP);
        lyricExpandedSet.clear(R.id.tv_title, ConstraintSet.BOTTOM);
        lyricExpandedSet.clear(R.id.tv_title, ConstraintSet.START);
        lyricExpandedSet.clear(R.id.tv_title, ConstraintSet.END);
        lyricExpandedSet.connect(R.id.tv_title, ConstraintSet.TOP, R.id.iv_disc, ConstraintSet.TOP, dpToPx(2));
        lyricExpandedSet.connect(R.id.tv_title, ConstraintSet.START, R.id.iv_disc, ConstraintSet.END, dpToPx(12));
//        lyricExpandedSet.connect(R.id.tv_title, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dpToPx(16));

        lyricExpandedSet.clear(R.id.tv_artist, ConstraintSet.TOP);
        lyricExpandedSet.clear(R.id.tv_artist, ConstraintSet.BOTTOM);
        lyricExpandedSet.clear(R.id.tv_artist, ConstraintSet.START);
        lyricExpandedSet.clear(R.id.tv_artist, ConstraintSet.END);
        lyricExpandedSet.connect(R.id.tv_artist, ConstraintSet.TOP, R.id.tv_title, ConstraintSet.BOTTOM, dpToPx(6));
        lyricExpandedSet.connect(R.id.tv_artist, ConstraintSet.START, R.id.iv_disc, ConstraintSet.END, dpToPx(12));
//        lyricExpandedSet.connect(R.id.tv_artist, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dpToPx(16));

        lyricExpandedSet.setVisibility(R.id.view_halo, View.GONE);
        lyricExpandedSet.setVisibility(R.id.iv_disc, View.VISIBLE);
        lyricExpandedSet.setVisibility(R.id.tv_title, View.VISIBLE);
        lyricExpandedSet.setVisibility(R.id.tv_artist, View.VISIBLE);
        lyricGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleLyricExpand(root);
                return true;
            }
        });
        binding.rvLyrics.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return lyricGestureDetector.onTouchEvent(e);
            }
        });
    }

    private void toggleLyricExpand(ConstraintLayout root) {
        isLyricExpanded = !isLyricExpanded;
        TransitionManager.beginDelayedTransition(root);
        if (isLyricExpanded) {
            lyricExpandedSet.applyTo(root);
        } else {
            lyricCollapsedSet.applyTo(root);
        }
        if (lyricAdapter != null) {
            lyricAdapter.setExpanded(isLyricExpanded);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
//                .rotationBy(180f)
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


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理所有歌词动画
        if (binding.rvLyrics.getAdapter() != null) {
            for (int i = 0; i < binding.rvLyrics.getChildCount(); i++) {
                View view = binding.rvLyrics.getChildAt(i);
                TextView tv = view.findViewById(R.id.tvLyric);
                if (tv != null) {
                    Object animator = tv.getTag(R.id.lyric_gradient_animator);
                    if (animator instanceof ValueAnimator) {
                        ((ValueAnimator) animator).cancel();
                    }
                    tv.clearAnimation();
                }
            }
        }

        NotifyCenter.unregisterListener(notifyListener);
        haloBreathAnimator.cancel();
        seekBarControl.pause();
        lyricHandler.removeCallbacksAndMessages(null);
    }

    private void startFakeLyricProgress() {
        lyricHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                int seek = MusicService.INSTANCE.getCurrentSeek();
                updateLyricByTime(seek + 300); // 提前300ms切换歌词，感觉更自然
                lyricHandler.postDelayed(this, 300);
            }
        }, 300);
    }

    private void updateLyricByTime(long timeMs) {
        if (lyricAdapter.updateLyricByTime(timeMs)) {
            scrollLyricToCenter(lyricAdapter.currentIndex);
        }
    }

    private void scrollLyricToCenter(int index) {
        RecyclerView.LayoutManager lm = binding.rvLyrics.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;

        LinearLayoutManager llm = (LinearLayoutManager) lm;

        int rvHeight = binding.rvLyrics.getHeight();
        int itemHeight = binding.rvLyrics.getChildAt(0) != null
                ? binding.rvLyrics.getChildAt(0).getHeight()
                : 80; // 默认高度

        int offset = rvHeight / 2 - itemHeight / 2;

        App.log("scrollLyricToCenter index={} rvHeight={} itemHeight={} offset={}", index, rvHeight, itemHeight, offset);
        llm.scrollToPositionWithOffset(index, offset);
    }

    class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.VH> {
        int currentIndex = 0;
        private List<LyricLine> lyrics;
        private String signature = "";
        private boolean expanded = false;
        private final float sizeBoostSp = 2f;
        private final float currentSizeSp = 20f;
        private final float normalSizeSp = 16f;

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lyric, parent, false);
            return new VH(v);
        }

        private boolean updateLyricByTime(long timeMs) {
            int currentIndex = LyricHandler.getCurrentIndex(lyricAdapter.lyrics, timeMs);
            if (currentIndex < 0) return false;

            return changeCurrentIndex(currentIndex);
        }

        public void changeData(List<LyricLine> newLyrics) {
            String s = JSON.toJSONString(newLyrics);
            if (StrUtil.equals(s, signature)) {
                return;
            }
            signature = s;
            this.lyrics = newLyrics;
            notifyDataSetChanged();
        }

        public boolean changeCurrentIndex(int newIndex) {
            if (currentIndex == newIndex) return false;

            currentIndex = newIndex;
            notifyDataSetChanged();
            return true;
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            h.tv.setText(lyrics.get(pos).text);

            int diff = Math.abs(pos - currentIndex);
            float boost = expanded ? sizeBoostSp : 0f;

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
                h.tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSizeSp + boost);
                h.tv.setAlpha(1f);

                h.tv.animate()
                    .alpha(1f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(180)
                    .start();

                // 颜色渐变动画
                if (h.tv.getWidth() > 0) {
                    animateTextGradient(h.tv, pos);
                } else {
                    h.tv.post(() -> animateTextGradient(h.tv, pos));
                }

            } else {
                float alpha = Math.max(0.25f, 1f - diff * 0.2f);
                float scale = Math.max(0.9f, 1f - diff * 0.05f);

                h.tv.setTextColor(Color.parseColor("#88FFFFFF"));
                h.tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, normalSizeSp + boost);
                h.tv.setAlpha(alpha);
                h.tv.setScaleX(scale);
                h.tv.setScaleY(scale);
            }
        }

        // 歌词文字渐变动画
        private void animateTextGradient(TextView textView, int position) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();

                int[] colors = {
                        Color.HSVToColor(new float[]{(200 + progress * 60) % 360, 0.9f, 1f}),
                        Color.HSVToColor(new float[]{(260 + progress * 60) % 360, 0.8f, 1f}),
                        Color.HSVToColor(new float[]{(320 + progress * 60) % 360, 0.9f, 1f})
                };

                LinearGradient gradient = new LinearGradient(
                        0, 0, textView.getWidth(), 0,
                        colors,
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP
                );

                textView.getPaint().setShader(gradient);
                textView.invalidate();
            });

            // 保存animator引用以便清理
            textView.setTag(R.id.lyric_gradient_animator, animator);
            animator.start();
        }

        @Override
        public int getItemCount() {
            return lyrics.size();
        }

        public void setExpanded(boolean expanded) {
            if (this.expanded == expanded) return;
            this.expanded = expanded;
            notifyDataSetChanged();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;

            VH(View v) {
                super(v);
                tv = v.findViewById(R.id.tvLyric);

                // 清理动画资源
                tv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {}

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        Object animator = tv.getTag(R.id.lyric_gradient_animator);
                        if (animator instanceof ValueAnimator) {
                            ((ValueAnimator) animator).cancel();
                        }
                    }
                });
            }
        }
    }
}
