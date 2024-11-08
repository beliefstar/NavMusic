package com.zx.navmusic.ui.home;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.zx.navmusic.MusicService;
import com.zx.navmusic.R;
import com.zx.navmusic.common.bean.MusicItem;
import com.zx.navmusic.event.NotifyCenter;
import com.zx.navmusic.service.MusicLiveProvider;

import java.util.List;

public class MusicListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final int mResource;
    private List<String> mObjects;

    public MusicListAdapter(Context context, int resource,
                            List<String> objects) {
        mContext = context;
        mResource = resource;
        mObjects = objects;
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
            MusicItem item = MusicLiveProvider.getInstance().getItem(mPosition);
            if (item == null) {
                return true;
            }
            // 弹出删除对话框
            new AlertDialog.Builder(mContext)
                    .setTitle("删除")
                    .setMessage(item.name)
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (NotifyCenter.getMusicPlayState().index == mPosition) {
                            Intent intent = new Intent(mContext, MusicService.class);
                            intent.setAction(MusicService.ACTION_NEXT);
                            mContext.startForegroundService(intent);
                        }
                        MusicLiveProvider.getInstance().remove(mPosition);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        view.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra(MusicService.ACTION_PLAY_INDEX, (int) v.getTag());
            mContext.startForegroundService(intent);
        });

        try {
            TextView text = view.findViewById(R.id.tv_mli);
            text.setText(getItem(position).toString());

        } catch (ClassCastException e) {
            Log.e("MusicListAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "MusicListAdapter requires the resource ID to be a TextView", e);
        }

        return view;
    }
}
