// File: app/src/main/java/com/pinli/app/ui/main/chat/ChatsAdapter.java
package com.pinli.app.ui.main.chat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pinli.app.databinding.ItemChatBinding;

import java.util.ArrayList;
import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.VH> {

    public interface Listener {
        void onClick(@NonNull ChatsViewModel.Row row);
    }

    private final Listener listener;
    private final List<ChatsViewModel.Row> items = new ArrayList<>();

    public ChatsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ChatsViewModel.Row> rows) {
        items.clear();
        if (rows != null) items.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding vb = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(vb);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatsViewModel.Row row = items.get(position);

        String name = row.otherName != null ? row.otherName : (row.otherUid != null ? row.otherUid : "User");
        holder.vb.tvName.setText(name);

        String last = "";
        if (row.chat != null && row.chat.lastMessage != null) last = row.chat.lastMessage;
        holder.vb.tvLast.setText(last);

        holder.itemView.setOnClickListener(v -> {
            if (row != null) listener.onClick(row);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemChatBinding vb;
        VH(ItemChatBinding vb) {
            super(vb.getRoot());
            this.vb = vb;
        }
    }
}
