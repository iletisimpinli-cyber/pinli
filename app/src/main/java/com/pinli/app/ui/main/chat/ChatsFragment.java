// File: app/src/main/java/com/pinli/app/ui/main/chat/ChatsFragment.java
package com.pinli.app.ui.main.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pinli.app.databinding.FragmentChatsBinding;

public class ChatsFragment extends Fragment {

    private FragmentChatsBinding vb;
    private ChatsViewModel vm;
    private ChatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vb = FragmentChatsBinding.inflate(inflater, container, false);
        return vb.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(ChatsViewModel.class);

        adapter = new ChatsAdapter(row -> {
            if (row == null || row.chat == null || row.otherUid == null) return;
            Intent i = new Intent(requireContext(), ChatActivity.class);
            i.putExtra(ChatActivity.EXTRA_CHAT_ID, row.chat.id);
            i.putExtra(ChatActivity.EXTRA_OTHER_UID, row.otherUid);
            startActivity(i);
        });

        vb.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        vb.recycler.setAdapter(adapter);

        vm.loading().observe(getViewLifecycleOwner(), l ->
                vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE)
        );

        vm.toast().observe(getViewLifecycleOwner(), e ->
                android.widget.Toast.makeText(requireContext(), e.message, android.widget.Toast.LENGTH_LONG).show()
        );

        vm.rows().observe(getViewLifecycleOwner(), rows -> {
            adapter.submit(rows);
            boolean empty = rows == null || rows.isEmpty();
            vb.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            vb.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        vm.start();
    }

    @Override
    public void onDestroyView() {
        vb = null;
        super.onDestroyView();
    }
}
