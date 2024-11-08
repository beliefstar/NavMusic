package com.zx.navmusic.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.R;
import com.zx.navmusic.common.App;
import com.zx.navmusic.databinding.FragmentHomeBinding;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.event.NotifyListener;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.ui.UIFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

public class HomeFragment extends Fragment implements NotifyListener {

    private FragmentHomeBinding binding;
    private TextView tvPlayMusicName;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        UIFragment.initView(getContext(), root);

        tvPlayMusicName = binding.tvPlayMusicName;
        AtomicReference<String> argMusicId =
                new AtomicReference<>(Optional.ofNullable(getArguments()).map(arg -> arg.getString(App.MUSIC_ID)).orElse(null));

        homeViewModel.getList().observe(getViewLifecycleOwner(), this::renderMusicList);


        renderPlaySwitch();
        binding.lvList.setOnItemClickListener(this::listItemClick);
        binding.btnPlay.setOnClickListener(this::playPauseClick);
        binding.btnPreviousMusic.setOnClickListener(this::previousMusicClick);
        binding.btnNextMusic.setOnClickListener(this::nextMusicClick);
        binding.spPlayMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                playModeItemClick(parent, view, position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 恢复播放组件状态
        NotifyCenter.registerListener(this);
        onMusicStateChange(NotifyCenter.getMusicPlayState());
        App.log("argMusicId: {}", argMusicId.get());
        Context context = getContext();
        if (context != null && StrUtil.isNotBlank(argMusicId.get())) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra(MusicService.ACTION_PLAY_ID, argMusicId.get());
            context.startForegroundService(intent);
        }

        Log.d(App.App_Name, "Home -- onCreate()");
        return root;
    }

    private void playModeItemClick(AdapterView<?> parent, View view, int position, long id) {
        int type = -1;
        switch (position) {
            case 0:
                type = MusicService.PlaySwitchStrategy.LINEAR;
                break;
            case 1:
                type = MusicService.PlaySwitchStrategy.RANDOM;
                break;
            case 2:
                type = MusicService.PlaySwitchStrategy.LOOP;
                break;
        }
        if (type != -1) {
            Context context = getContext();
            if (context != null) {
                Intent intent = new Intent(context, MusicService.class);
                intent.setAction(MusicService.ACTION_PLAY_MODE);
                intent.putExtra(MusicService.ACTION_PLAY_MODE, type);
                context.startForegroundService(intent);
            }
        }
    }

    private void listItemClick(AdapterView<?> parent, View view, int position, long id) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra(MusicService.ACTION_PLAY_INDEX, position);
            context.startForegroundService(intent);
        }
    }

    @Override
    public void onMusicStateChange(MusicPlayState playState) {
        if (playState == null) {
            return;
        }
        tvPlayMusicName.setText(playState.name);

        int icResId = playState.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), icResId, getContext().getTheme());
        binding.btnPlay.setBackground(drawable);
        binding.spPlayMode.setSelection(playState.playSwitchStrategy);
    }


    private void previousMusicClick(View view) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PREVIOUS);
            context.startForegroundService(intent);
        }
    }

    private void nextMusicClick(View view) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_NEXT);
            context.startForegroundService(intent);
        }
    }

    private void playPauseClick(View view) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY_PAUSE);
            context.startForegroundService(intent);
        }
    }

    private void renderPlaySwitch() {
        List<String> lst = Arrays.asList("列表循环", "随机播放", "单曲循环");
        ArrayAdapter<String> aa = new ArrayAdapter<>(getActivity(), R.layout.list_play_mode, lst);
        binding.spPlayMode.setAdapter(aa);
    }

    private void renderMusicList(List<String> lst) {
        FragmentActivity activity = getActivity();
        if (activity != null && CollUtil.isNotEmpty(lst)) {
            App.log("Home-size: {}", lst.size());
            if (binding != null) {
                MusicListAdapter adapter = new MusicListAdapter(activity, R.layout.music_list_item, lst);
                binding.lvList.setAdapter(adapter);
            }
        }
    }


//    private void handleInput(View view) {
//        String input = binding.mInput.getText().toString();
//        binding.mTv.setText("input: " + input);
//
//        if (!PermissionUtils.checkFilePermission(getActivity())) {
//            PermissionUtils.requireFilePermission(getActivity());
//            return;
//        }
//
//        if (!ROOT.exists()) {
//            ROOT.mkdirs();
//        }
//        File dir = ROOT;
//        File file = new File(dir, "aaa.txt");
//        Log.d(getString(R.string.app_name), file.getAbsolutePath());
//        Log.d(getString(R.string.app_name), "dir: %s, file: %s".formatted(dir.exists(), file.exists()));
//
//        try {
//            file.createNewFile();
//            OutputStream out = Files.newOutputStream(file.toPath());
//            IOUtils.copy(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out);
//
//            out.flush();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//    private void handleRead(View view) {
//        if (!PermissionUtils.checkFilePermission(getActivity())) {
//            PermissionUtils.requireFilePermission(getActivity());
//            return;
//        }
//        if (!ROOT.exists()) {
//            ROOT.mkdirs();
//        }
//        File dir = ROOT;
//        File file = new File(dir, "aaa.txt");
//        Log.d(getString(R.string.app_name), file.getAbsolutePath());
//
//        String readStr;
//        try {
//            InputStream in = Files.newInputStream(file.toPath());
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            IOUtils.copy(in, out);
//
//            readStr = new String(out.toByteArray(), StandardCharsets.UTF_8);
//            in.close();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        binding.mReadTv.setText("read: " + readStr);
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        NotifyCenter.unregisterListener(this);
        binding = null;
    }

}