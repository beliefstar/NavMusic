package com.zx.navmusic.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.R;
import com.zx.navmusic.common.App;
import com.zx.navmusic.common.Util;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.config.ConfigCenter;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.service.MusicLiveProvider;
import com.zx.navmusic.service.MusicPlayState;
import com.zx.navmusic.service.strategy.PlayModeStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final Activity mContext;
    private final int mResource;
    private List<MusicItem> mObjects;

    public MusicListAdapter(Activity context, int resource) {
        mContext = context;
        mResource = resource;
        mObjects = new ArrayList<>();
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public Object getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    public void onChange(List<MusicItem> lst, int playMode) {
        if (lst == null || lst.equals(mObjects)) {
            return;
        }
        App.log("MusicListAdapter onChange {}, {}", lst.size(), playMode);
        List<MusicItem> copy = new ArrayList<>(lst);
        if (playMode == PlayModeStrategy.RANDOM && ConfigCenter.isFavoriteSort()) {
            copy.sort((a, b) -> {
                if (a.score.equals(b.score)) {
                    return a.name.compareTo(b.name);
                }
                return b.score - a.score;
            });
        }
        mObjects = copy;
        notifyDataSetChanged();
    }

    private View createViewFromResource(LayoutInflater inflater, int position,
                                        View convertView, ViewGroup parent, int resource) {
        final View view;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        view.setTag(position);

        view.setOnLongClickListener(v -> {
            int mPosition = (int) v.getTag();
            MusicItem item = (MusicItem) getItem(mPosition);
            if (item == null) {
                return true;
            }
            // 弹出删除对话框
            new AlertDialog.Builder(mContext)
                    .setTitle("删除")
                    .setMessage(item.displayName())
                    .setPositiveButton("删除", (dialog, which) -> {
                        MusicPlayState musicPlayState = NotifyCenter.getMusicPlayState();
                        if (musicPlayState != null && Objects.equals(musicPlayState.id, item.id)) {
                            Intent intent = new Intent(mContext, MusicService.class);
                            intent.setAction(MusicService.ACTION_NEXT);
                            mContext.startForegroundService(intent);
                        }
                        MusicLiveProvider.getInstance().remove(item.id);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        view.setOnClickListener(v -> {
            int mPosition = (int) v.getTag();
            MusicItem item = (MusicItem) getItem(mPosition);
            if (item == null) {
                return;
            }
            if (MusicLiveProvider.getInstance().isInitializing(item.id)) {
                App.toast("该曲目正在初始化中，请稍后播放");
                return;
            }
            Intent intent = new Intent(mContext, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra(MusicService.ACTION_PLAY_ID, item.id);
            mContext.startForegroundService(intent);

            Util.navigatePlaying(mContext);
        });

        try {
            MusicItem item = (MusicItem) getItem(position);
            TextView text = view.findViewById(R.id.tv_mli);
            text.setText(item.displayName());

            // 下载进度
            applyDownloadBackground(view, item.download);

            // 喜好度
            ImageView imageView = view.findViewById(R.id.iv_mli_icon);
            int rankRes = item.getRankRes();
            if (rankRes > 0) {
                imageView.setImageResource(rankRes);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }

            // 高亮颜色
            MusicPlayState playState = NotifyCenter.getMusicPlayState();
            if (playState != null && Objects.equals(playState.id, item.id)) {
                text.setTextColor(view.getResources().getColor(R.color.light_blue_900, view.getContext().getTheme()));
                text.setTypeface(null, Typeface.BOLD);
                text.setTextSize(16);
            } else {
                text.setTextColor(view.getResources().getColor(R.color.gray, view.getContext().getTheme()));
                text.setTypeface(null, Typeface.NORMAL);
                text.setTextSize(14);
            }
        } catch (ClassCastException e) {
            Log.e("MusicListAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "MusicListAdapter requires the resource ID to be a TextView", e);
        }

        return view;
    }

    private void applyDownloadBackground(View view, int progress) {

        if (progress <= 0) {
            // 下载完成或未下载：恢复默认背景
            view.setBackground(null);
            return;
        }

        float ratio = progress / 10000f;

        int progressColor = 0x3329B6F6; // 半透明蓝色
        int normalColor = 0x00000000;   // 透明

        int[] colors = new int[]{
                progressColor,
                progressColor,
                normalColor,
                normalColor
        };

        float[] positions = new float[]{
                0f,
                ratio,
                ratio,
                1f
        };

        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        colors);

        drawable.setGradientType(android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT);
        drawable.setColors(colors, positions);

        view.setBackground(drawable);
    }
}
