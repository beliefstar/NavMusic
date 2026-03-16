package com.zx.navmusic.ui.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.zx.navmusic.MusicService;
import com.zx.navmusic.R;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.Util;
import com.zx.navmusic.databinding.FragmentHomeBinding;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.strategy.PlayModeStrategy;
import com.zx.navmusic.ui.UIFragment;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

public class HomeFragment extends Fragment implements NotifyListener {

    private FragmentHomeBinding binding;
    private TextView tvPlayMusicName;

    private ObjectAnimator rotation;
    private MusicListAdapter listAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        UIFragment.initView(getContext(), root);

        tvPlayMusicName = binding.tvPlayMusicName;
        AtomicReference<String> argMusicId =
                new AtomicReference<>(Optional.ofNullable(getArguments()).map(arg -> arg.getString(App.MUSIC_ID)).orElse(null));

        listAdapter = new MusicListAdapter(getActivity(), R.layout.music_list_item);

        MusicLiveProvider.getInstance().observeForever(list -> {
            if (CollUtil.isEmpty(list)) {
                return;
            }
            MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
            int playMode = PlayModeStrategy.LINEAR;
            if (musicPlayState != null) {
                playMode = musicPlayState.playSwitchStrategy;
            }
            listAdapter.onChange(list, playMode);
        });

        binding.lvList.setAdapter(listAdapter);
        binding.tvPlayMusicName.setOnClickListener(v -> {
            Activity ctx = getActivity();
            if (ctx != null) {
                Util.navigatePlaying(ctx);
            }
        });
        initCircularImage();

        // 恢复播放组件状态
        onMusicStateChange(NotifyCenter.getMusicPlayState());
        Context context = getContext();
        if (context != null && StrUtil.isNotBlank(argMusicId.get())) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra(MusicService.ACTION_PLAY_ID, argMusicId.get());
            context.startForegroundService(intent);
        }

        NotifyCenter.registerListener(this);
        return root;
    }

    @Override
    public void onMusicStateChange(MusicPlayState playState) {
        if (playState == null) {
            return;
        }
        tvPlayMusicName.setText(playState.name);

        String artistCombo = playState.artist;
        if (StrUtil.isNotBlank(playState.album)) {
            artistCombo += " - " + playState.album;
        }
        binding.tvPlayMusicArtist.setText(artistCombo);

        if (playState.isPlaying) {
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
        listAdapter.onChange(MusicLiveProvider.getInstance().getList(), playState.playSwitchStrategy);
    }

    private void initCircularImage() {
        ImageView imageView = binding.ivPlayMusicNameImage;

        // 设置圆形图片
        Glide.with(this)
                .load(R.drawable.ic_music_plus)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);

        // 使用 ObjectAnimator 创建旋转动画
        rotation = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
        rotation.setDuration(5000);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setInterpolator(new LinearInterpolator());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NotifyCenter.unregisterListener(this);
        binding = null;
    }

}