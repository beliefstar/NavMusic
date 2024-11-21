package com.zx.navmusic.ui.search;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zx.navmusic.common.App;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.common.bean.SearchItem;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SearchViewModel extends ViewModel {

    private final MutableLiveData<List<SearchItem>> data;
    private MusicProvider musicProvider;

    public SearchViewModel() {
        data = new MutableLiveData<>(Collections.emptyList());
        musicProvider = MusicLiveProvider.getInstance();
    }

    public LiveData<List<SearchItem>> getData() {
        return data;
    }

    public void search(FragmentActivity activity, String keyword) {
        musicProvider.search(activity, keyword).whenComplete((lst, ex) -> {
            if (ex != null) {
                App.toast("搜索失败 {}", ex.getMessage());
                return;
            }
            if (lst == null) {
                lst = new ArrayList<>();
            }
            data.postValue(lst);
        });
    }


    public void searchLocal(String keyword) {
        List<SearchItem> sis = musicProvider.searchLocal(keyword);

        if (sis == null) {
            sis = Collections.emptyList();
        }

        App.log("searchLocal: {} - {}", keyword, sis);
        data.postValue(sis);
    }

    public CompletableFuture<MusicItem> choose(FragmentActivity activity, int position) {
        SearchItem searchItem = data.getValue().get(position);
//        App.toast("点击了 name:{}, ref: {}", searchItem.name, searchItem.ref);
        return musicProvider.touchMusic(activity, searchItem);
    }
}