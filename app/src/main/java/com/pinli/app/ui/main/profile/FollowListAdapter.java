// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowListAdapter.java
package com.pinli.app.ui.main.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pinli.app.data.model.User;
import com.pinli.app.databinding.ItemFollowUserBinding;

import java.util.ArrayList;
import java.util.List;

public class FollowListAdapter extends RecyclerView.Adapter<FollowListAdapter.VH> {
    private final List<User> items = new ArrayList<>();

    public void submit(@NonNull List<User> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFollowUserBinding vb = ItemFollowUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(vb);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User user = items.get(position);
        holder.vb.tvName.setText(user.displayName != null ? user.displayName : "-");
        String bio = user.bio != null ? user.bio : "";
        holder.vb.tvBio.setText(bio.isEmpty() ? "-" : bio);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFollowUserBinding vb;

        VH(ItemFollowUserBinding vb) {
            super(vb.getRoot());
            this.vb = vb;
        }
    }
}