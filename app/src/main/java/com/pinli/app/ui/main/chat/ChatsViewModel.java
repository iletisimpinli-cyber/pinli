// File: app/src/main/java/com/pinli/app/ui/main/chat/ChatsViewModel.java
package com.pinli.app.ui.main.chat;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.Chat;
import com.pinli.app.data.model.User;
import com.pinli.app.data.repo.BlockRepository;
import com.pinli.app.data.repo.ChatRepository;
import com.pinli.app.data.repo.UserRepository;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatsViewModel extends ViewModel {

    public static class Row {
        public Chat chat;
        public String otherUid;
        public String otherName;
    }

    private final ChatRepository chatRepo = new ChatRepository();
    private final UserRepository userRepo = new UserRepository();
    private final BlockRepository blockRepo = new BlockRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();
    private final MutableLiveData<List<Row>> rows = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration chatsReg;
    private Set<String> hiddenUids = new HashSet<>();

    private final Map<String, String> nameCache = new HashMap<>();

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<List<Row>> rows() { return rows; }

    public void start() {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;

        loading.setValue(true);

        // load block list first
        blockRepo.loadBlockedRelatedUids(uid, new BlockRepository.Callback<Set<String>>() {
            @Override public void onSuccess(@NonNull Set<String> data) {
                hiddenUids = data != null ? data : new HashSet<>();
                startChatsListener(uid);
            }

            @Override public void onError(@NonNull String message) {
                hiddenUids = new HashSet<>();
                startChatsListener(uid);
            }
        });
    }

    private void startChatsListener(@NonNull String uid) {
        if (chatsReg != null) return;

        chatsReg = chatRepo.listenChats(uid, new ChatRepository.Callback<List<Chat>>() {
            @Override public void onSuccess(@NonNull List<Chat> data) {
                loading.setValue(false);

                List<Row> out = new ArrayList<>();
                for (Chat c : data) {
                    if (c == null) continue;
                    String other = ChatRepository.otherUidFromChat(c, uid);
                    if (other == null) continue;
                    if (hiddenUids.contains(other)) continue;

                    Row r = new Row();
                    r.chat = c;
                    r.otherUid = other;
                    r.otherName = nameCache.get(other);
                    out.add(r);

                    if (r.otherName == null) {
                        fetchName(other);
                    }
                }
                rows.setValue(out);
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    private void fetchName(@NonNull String uid) {
        if (nameCache.containsKey(uid)) return;
        nameCache.put(uid, null);

        userRepo.getUser(uid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                nameCache.put(uid, data.displayName != null ? data.displayName : "User");
                // force refresh
                List<Row> current = rows.getValue();
                if (current != null) rows.setValue(new ArrayList<>(current));
            }

            @Override public void onError(@NonNull String message) {
                nameCache.put(uid, "User");
                List<Row> current = rows.getValue();
                if (current != null) rows.setValue(new ArrayList<>(current));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (chatsReg != null) {
            chatsReg.remove();
            chatsReg = null;
        }
        super.onCleared();
    }
}
