package com.zx.navmusic.ui.notifications;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.zx.navmusic.databinding.FragmentNotificationsBinding;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.impl.CloudMusicProvider;
import com.zx.navmusic.service.impl.LocalMusicProvider;
import com.zx.navmusic.ui.UIFragment;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        UIFragment.initView(getContext(), root);

//        final TextView textView = binding.textNotifications;
//        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        MusicLiveProvider instance = MusicLiveProvider.getInstance();
        if (instance instanceof CloudMusicProvider) {
            CloudMusicProvider provider = (CloudMusicProvider) instance;
            binding.etToken.setText(provider.getToken());
        }
        if (instance instanceof LocalMusicProvider) {
            LocalMusicProvider provider = (LocalMusicProvider) instance;
            binding.cbUseLocalMode.setChecked(provider.getUseLocalMode());
            binding.etBbsToken.setText(provider.bbsToken);
        }

        binding.cbUseLocalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (instance instanceof LocalMusicProvider) {
                LocalMusicProvider provider = (LocalMusicProvider) instance;
                provider.setUseLocalMode(isChecked);
            }
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
                if (instance instanceof LocalMusicProvider) {
                    LocalMusicProvider provider = (LocalMusicProvider) instance;
                    provider.bbsToken = s.toString();
                }
            }
        });

        binding.btnUnlock.setOnClickListener(v -> {
            if (instance instanceof CloudMusicProvider) {
                CloudMusicProvider provider = (CloudMusicProvider) instance;
                provider.unlock();
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