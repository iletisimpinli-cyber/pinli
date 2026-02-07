// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowRequestsActivity.java
package com.pinli.app.ui.main.profile;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pinli.app.databinding.ActivityFollowRequestsBinding;

public class FollowRequestsActivity extends AppCompatActivity {

    private ActivityFollowRequestsBinding vb;
    private FollowRequestsViewModel vm;
    private FollowRequestsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityFollowRequestsBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.toolbar.setNavigationOnClickListener(v -> finish());

        vm = new ViewModelProvider(this).get(FollowRequestsViewModel.class);

        adapter = new FollowRequestsAdapter(new FollowRequestsAdapter.Listener() {
            @Override public void onAccept(@androidx.annotation.NonNull com.pinli.app.data.model.FollowRequest fr) { vm.accept(fr); }
            @Override public void onReject(@androidx.annotation.NonNull com.pinli.app.data.model.FollowRequest fr) { vm.reject(fr); }
        });

        vb.recycler.setLayoutManager(new LinearLayoutManager(this));
        vb.recycler.setAdapter(adapter);

        vm.loading().observe(this, l -> vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE));
        vm.toast().observe(this, e -> android.widget.Toast.makeText(this, e.message, android.widget.Toast.LENGTH_LONG).show());
        vm.rows().observe(this, rows -> {
            adapter.submit(rows);
            boolean empty = rows == null || rows.isEmpty();
            vb.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            vb.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        vm.load();
    }
}
