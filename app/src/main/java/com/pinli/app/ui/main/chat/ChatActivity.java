// File: app/src/main/java/com/pinli/app/ui/main/chat/ChatActivity.java
package com.pinli.app.ui.main.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pinli.app.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chatId";
    public static final String EXTRA_OTHER_UID = "otherUid";

    private ActivityChatBinding vb;
    private ChatViewModel vm;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.toolbar.setNavigationOnClickListener(v -> finish());

        String chatId = getIntent() != null ? getIntent().getStringExtra(EXTRA_CHAT_ID) : null;
        String otherUid = getIntent() != null ? getIntent().getStringExtra(EXTRA_OTHER_UID) : null;

        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(otherUid)) {
            finish();
            return;
        }

        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        adapter = new MessageAdapter(vm);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        vb.recycler.setLayoutManager(lm);
        vb.recycler.setAdapter(adapter);

        vm.loading().observe(this, l -> vb.progress.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE));
        vm.toast().observe(this, e -> android.widget.Toast.makeText(this, e.message, android.widget.Toast.LENGTH_LONG).show());
        vm.finish().observe(this, f -> {
            if (Boolean.TRUE.equals(f)) finish();
        });

        vm.title().observe(this, t -> {
            if (t != null) vb.toolbar.setTitle(t);
        });

        vm.messages().observe(this, msgs -> {
            adapter.submit(msgs);
            if (msgs != null && !msgs.isEmpty()) {
                vb.recycler.scrollToPosition(msgs.size() - 1);
            }
        });

        vb.btnSend.setOnClickListener(v -> {
            String text = vb.etMessage.getText() != null ? vb.etMessage.getText().toString() : "";
            if (text.trim().isEmpty()) return;
            vm.send(text);
            vb.etMessage.setText("");
        });

        vm.start(chatId, otherUid);
    }
}
