package com.zx.navmusic.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.R;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.databinding.FragmentSearchBinding;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicProvider;
import com.zx.navmusic.ui.UIFragment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel searchViewModel;
    private final AtomicBoolean choosing = new AtomicBoolean(false);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        searchViewModel =
                new ViewModelProvider(this).get(SearchViewModel.class);

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        UIFragment.initView(getContext(), root);

        searchViewModel.getData().observeForever(lst -> {
            Optional.ofNullable(getActivity()).ifPresent(ac -> {
                List<String> collect = lst.stream().map(t -> t.name).collect(Collectors.toList());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ac, R.layout.list_item, collect);
                binding.lvSearch.setAdapter(adapter);
            });
        });

        binding.btnSearch.setOnClickListener(this::onSearchClick);
        binding.lvSearch.setOnItemClickListener(this::onItemClick);

        binding.etSearchKeyword.addTextChangedListener(new SearchTextChangeListener(searchViewModel));

        Log.d(App.App_Name, "Dashboard -- onCreate()");
        return root;
    }

    private void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!choosing.compareAndSet(false, true)) {
            App.toast("操作过于频繁");
            return;
        }

        CompletableFuture<MusicItem> future;
        try {
            future = searchViewModel.choose(getActivity(), position);
        } catch (Exception e) {
            App.toast(e.getMessage());
            return;
        } finally {
            choosing.set(false);
        }

        FragmentActivity activity = getActivity();
        if (future.isDone() && activity != null) {
            MusicItem musicItem = null;
            try {
                musicItem = future.get();
            } catch (Exception ignore) {
            }
            if (musicItem != null) {
                Intent intent = new Intent(activity, MusicService.class);
                intent.setAction(MusicService.ACTION_PLAY);
                intent.putExtra(MusicService.ACTION_PLAY_ID, musicItem.id);
                activity.startForegroundService(intent);
            }
        }

        Bundle bundle = new Bundle();
        NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_home, bundle);
    }

    private void onSearchClick(View view) {
        String keyword = binding.etSearchKeyword.getText().toString();
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        searchViewModel.search(getActivity(), keyword);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class SearchTextChangeListener implements TextWatcher {
        SearchViewModel searchViewModel;

        public SearchTextChangeListener(SearchViewModel searchViewModel) {
            this.searchViewModel = searchViewModel;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            searchViewModel.searchLocal(s.toString());
        }
    }
}