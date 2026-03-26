package com.zx.navmusic.ui.settings;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.zx.navmusic.R;
import com.zx.navmusic.config.ConfigCenter;
import com.zx.navmusic.databinding.FragmentSettingsBinding;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.ui.ThemeHelper;
import com.zx.navmusic.ui.UIFragment;

import cn.hutool.core.util.NumberUtil;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ConfigCenter.ensureCreated(requireContext().getApplicationContext());
        UIFragment.initView(getContext(), root);

        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            binding.version.setText(packageInfo.versionName);
        } catch (Exception e) {
        }

        MusicLiveProvider instance = MusicLiveProvider.getInstance();
//        if (instance instanceof CloudMusicProvider) {
//            CloudMusicProvider provider = (CloudMusicProvider) instance;
//            binding.etToken.setText(provider.getToken());
//        }
//        binding.cbUseLocalMode.setChecked(ConfigCenter.isUseLocalMode());
//        binding.cbUseNewPlaybackUi.setChecked(ConfigCenter.isUseNewPlaybackUi());
        binding.scFavoriteSort.setChecked(ConfigCenter.isFavoriteSort());
        binding.scBluetoothLyric.setChecked(ConfigCenter.isBluetoothLyric());
//        binding.etBbsToken.setText(ConfigCenter.getBbsToken());
        binding.etFavoriteTep.setText(String.valueOf(ConfigCenter.getFavoriteStep()));
        initThemeSelector();

//        binding.cbUseLocalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            ConfigCenter.change(configData -> configData.useLocalMode = isChecked, getContext());
//        });

//        binding.cbUseNewPlaybackUi.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            ConfigCenter.change(configData -> configData.useNewPlaybackUi = isChecked, getContext());
//        });

        binding.scFavoriteSort.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigCenter.change(configData -> configData.favoriteSort = isChecked, getContext());
        });
        binding.scBluetoothLyric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ConfigCenter.change(configData -> configData.bluetoothLyric = isChecked, getContext());
        });

//        binding.etToken.addTextChangedListener(new TextWatcher() {
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                if (instance instanceof CloudMusicProvider) {
//                    CloudMusicProvider provider = (CloudMusicProvider) instance;
//                    provider.setToken(s.toString());
//                }
//            }
//        });
//
//        binding.etBbsToken.addTextChangedListener(new TextWatcher() {
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                ConfigCenter.change(configData -> configData.bbsToken = s.toString(), getContext());
//            }
//        });

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

//        binding.btnUnlock.setOnClickListener(v -> {
//            if (instance instanceof CloudMusicProvider) {
//                CloudMusicProvider provider = (CloudMusicProvider) instance;
//                provider.unlock();
//            }
//        });
//
//        binding.btnJump.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getActivity(), PlaybackNewActivity.class));
//            }
//        });

        return root;
    }

    private void initThemeSelector() {
        int themeType = ConfigCenter.getThemeType();
        int checkedId = R.id.rb_theme_simple;
        if (themeType == ConfigCenter.THEME_TECH) {
            checkedId = R.id.rb_theme_tech;
        } else if (themeType == ConfigCenter.THEME_VIVID) {
            checkedId = R.id.rb_theme_vivid;
        } else if (themeType == ConfigCenter.THEME_DARK) {
            checkedId = R.id.rb_theme_dark;
        }
        binding.rgTheme.check(checkedId);
        binding.rgTheme.setOnCheckedChangeListener((group, id) -> {
            int newType = ConfigCenter.THEME_SIMPLE;
            if (id == R.id.rb_theme_tech) {
                newType = ConfigCenter.THEME_TECH;
            } else if (id == R.id.rb_theme_vivid) {
                newType = ConfigCenter.THEME_VIVID;
            } else if (id == R.id.rb_theme_dark) {
                newType = ConfigCenter.THEME_DARK;
            }
            if (newType == ConfigCenter.getThemeType()) {
                return;
            }
            final int selectedThemeType = newType;
            ConfigCenter.change(configData -> configData.themeType = selectedThemeType, getContext());
            ThemeHelper.applyTheme(requireActivity());
            requireActivity().recreate();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
