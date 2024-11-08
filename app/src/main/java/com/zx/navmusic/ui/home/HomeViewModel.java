package com.zx.navmusic.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zx.navmusic.common.App;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<String>> mList;

    public HomeViewModel() {
        App.log("创建了HomeViewModel");
        mList = new MutableLiveData<>(Collections.emptyList());
        MusicLiveProvider.getInstance().observeForever(list -> {
            if (CollUtil.isEmpty(list)) {
                return;
            }
            mList.postValue(list.stream().map(t -> t.name).collect(Collectors.toList()));
        });
    }

    public LiveData<List<String>> getList() {
        return mList;
    }

}