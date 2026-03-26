package com.zx.navmusic.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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

        if (binding.ivPlayMusicAlbumImage.getTag(R.id.iv_disc_res_id) == null
                || !StrUtil.equals(binding.ivPlayMusicAlbumImage.getTag(R.id.iv_disc_res_id).toString(), playState.id)) {

            Bitmap album = MusicService.INSTANCE.getAlbum(playState.id);

            Glide.with(this)
                    .load(album)
                    .placeholder(R.drawable.ic_music_plus)  // 加载中显示的图片
                    .error(R.drawable.ic_music_plus)  // 加载失败显示的图片
                    .centerCrop()  // 裁剪方式
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(50)))
                    .into(binding.ivPlayMusicAlbumImage);

            if (album != null) {
                binding.ivPlayMusicAlbumImage.setTag(R.id.iv_disc_res_id, playState.id);
            }
        }

        tvPlayMusicName.setText(playState.name);

        String artistCombo = playState.artist;
        if (StrUtil.isNotBlank(playState.album)) {
            artistCombo += " - " + playState.album;
        }
        binding.tvPlayMusicArtist.setText(artistCombo);

        listAdapter.onChange(MusicLiveProvider.getInstance().getList(), playState.playSwitchStrategy);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NotifyCenter.unregisterListener(this);
        binding = null;
    }

}