package com.zx.navmusic.ui.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.zx.navmusic.PlaybackNewActivity;
import com.zx.navmusic.config.ConfigCenter;
import com.zx.navmusic.databinding.FragmentSettingsBinding;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.impl.CloudMusicProvider;
import com.zx.navmusic.ui.UIFragment;

import cn.hutool.core.util.NumberUtil;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        UIFragment.initView(getContext(), root);

//        final TextView textView = binding.textNotifications;
//        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            binding.version.setText(packageInfo.versionName);
        } catch (Exception e) {
        }

        MusicLiveProvider instance = MusicLiveProvider.getInstance();
        if (instance instanceof CloudMusicProvider) {
            CloudMusicProvider provider = (CloudMusicProvider) instance;
            binding.etToken.setText(provider.getToken());
        }
        binding.cbUseLocalMode.setChecked(ConfigCenter.isUseLocalMode());
        binding.cbUseNewPlaybackUi.setChecked(ConfigCenter.isUseNewPlaybackUi());
        binding.etBbsToken.setText(ConfigCenter.getBbsToken());
        binding.etFavoriteTep.setText(String.valueOf(ConfigCenter.getFavoriteStep()));

        binding.cbUseLocalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigCenter.change(configData -> configData.useLocalMode = isChecked, getContext());
        });

        binding.cbUseNewPlaybackUi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigCenter.change(configData -> configData.useNewPlaybackUi = isChecked, getContext());
        });

        binding.etToken.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (instance instanceof CloudMusicProvider) {
                    CloudMusicProvider provider = (CloudMusicProvider) instance;
                    provider.setToken(s.toString());
                }
            }
        });

        binding.etBbsToken.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ConfigCenter.change(configData -> configData.bbsToken = s.toString(), getContext());
            }
        });

        binding.etFavoriteTep.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ConfigCenter.change(configData -> configData.favoriteStep = NumberUtil.parseInt(s.toString()), getContext());
            }
        });

        binding.btnUnlock.setOnClickListener(v -> {
            if (instance instanceof CloudMusicProvider) {
                CloudMusicProvider provider = (CloudMusicProvider) instance;
                provider.unlock();
            }
        });

        binding.btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), PlaybackNewActivity.class));
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}