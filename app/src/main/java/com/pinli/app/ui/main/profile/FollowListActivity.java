// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowListActivity.java
package com.pinli.app.ui.main.profile;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pinli.app.R;
import com.pinli.app.databinding.ActivityFollowListBinding;

public class FollowListActivity extends AppCompatActivity {
    public static final String EXTRA_TYPE = "extra_type";
    public static final String TYPE_FOLLOWERS = "followers";
    public static final String TYPE_FOLLOWING = "following";

    private ActivityFollowListBinding vb;
    private FollowListViewModel vm;
    private FollowListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityFollowListBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.toolbar.setNavigationOnClickListener(v -> finish());

        String type = getIntent() != null ? getIntent().getStringExtra(EXTRA_TYPE) : TYPE_FOLLOWERS;
        boolean isFollowers = TYPE_FOLLOWERS.equals(type);
        vb.toolbar.setTitle(isFollowers ? R.string.followers_title : R.string.following_title);
        vb.tvEmpty.setText(isFollowers ? R.string.followers_empty : R.string.following_empty);

        vm = new ViewModelProvider(this).get(FollowListViewModel.class);
        adapter = new FollowListAdapter();

        vb.recycler.setLayoutManager(new LinearLayoutManager(this));
        vb.recycler.setAdapter(adapter);

        vm.loading().observe(this, l -> vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE));
        vm.toast().observe(this, e -> android.widget.Toast.makeText(this, e.message, android.widget.Toast.LENGTH_LONG).show());
        vm.users().observe(this, users -> {
            adapter.submit(users);
            boolean empty = users == null || users.isEmpty();
            vb.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            vb.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        vm.load(type);
    }
}