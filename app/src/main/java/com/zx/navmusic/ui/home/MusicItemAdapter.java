package com.zx.navmusic.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MusicItemAdapter extends RecyclerView.Adapter<MusicItemAdapter.ItemViewHolder> {

    private List<String> items;

    public MusicItemAdapter(List<String> items) {
        this.items = items;
    }

    public void setItems(List<String> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        String item = items.get(position);
        holder.textView.setText(item);

        // 设置长按监听器
        holder.itemView.setOnLongClickListener(v -> {
            // 弹出删除对话框
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("删除项")
                    .setMessage("你确定要删除这个项吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 删除项并通知适配器
                        items.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, items.size());
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    // ViewHolder类
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
