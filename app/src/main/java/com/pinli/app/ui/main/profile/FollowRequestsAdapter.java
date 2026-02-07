// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowRequestsAdapter.java
package com.pinli.app.ui.main.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pinli.app.data.model.FollowRequest;
import com.pinli.app.databinding.ItemFollowRequestBinding;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsAdapter extends RecyclerView.Adapter<FollowRequestsAdapter.VH> {

    public interface Listener {
        void onAccept(@NonNull FollowRequest fr);
        void onReject(@NonNull FollowRequest fr);
    }

    private final Listener listener;
    private final List<FollowRequestsViewModel.Row> items = new ArrayList<>();

    public FollowRequestsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<FollowRequestsViewModel.Row> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFollowRequestBinding vb = ItemFollowRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(vb);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FollowRequestsViewModel.Row row = items.get(position);
        FollowRequest fr = row != null ? row.request : null;

        String name = "User";
        if (row != null && row.user != null && row.user.displayName != null) name = row.user.displayName;
        holder.vb.tvName.setText(name);

        holder.vb.btnAccept.setOnClickListener(v -> {
            if (fr != null) listener.onAccept(fr);
        });
        holder.vb.btnReject.setOnClickListener(v -> {
            if (fr != null) listener.onReject(fr);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFollowRequestBinding vb;
        VH(ItemFollowRequestBinding vb) {
            super(vb.getRoot());
            this.vb = vb;
        }
    }
}
