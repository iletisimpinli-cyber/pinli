// File: app/src/main/java/com/pinli/app/ui/main/chat/ChatViewModel.java
package com.pinli.app.ui.main.chat;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.Message;
import com.pinli.app.data.model.User;
import com.pinli.app.data.repo.BlockRepository;
import com.pinli.app.data.repo.ChatRepository;
import com.pinli.app.data.repo.FollowRepository;
import com.pinli.app.data.repo.UserRepository;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepo = new ChatRepository();
    private final FollowRepository followRepo = new FollowRepository();
    private final BlockRepository blockRepo = new BlockRepository();
    private final UserRepository userRepo = new UserRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();
    private final LiveEvent<Boolean> finish = new LiveEvent<>();

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> title = new MutableLiveData<>("Chat");

    private ListenerRegistration msgReg;

    private String meUid;
    private String otherUid;
    private String chatId;

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<Boolean> finish() { return finish; }
    public LiveData<List<Message>> messages() { return messages; }
    public LiveData<String> title() { return title; }

    public void start(@NonNull String chatId, @NonNull String otherUid) {
        this.chatId = chatId;
        this.otherUid = otherUid;

        meUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (meUid == null) {
            toast.setValue(new UiEvent("Auth error"));
            finish.setValue(true);
            return;
        }

        loading.setValue(true);

        // Set title from user doc
        userRepo.getUser(otherUid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                if (data.displayName != null) title.postValue(data.displayName);
            }
            @Override public void onError(@NonNull String message) { /* ignore */ }
        });

        followRepo.getFollowState(meUid, otherUid, new FollowRepository.Callback<FollowRepository.FollowState>() {
            @Override public void onSuccess(@NonNull FollowRepository.FollowState fs) {
                if (!fs.canMessage()) {
                    loading.setValue(false);
                    toast.setValue(new UiEvent("Mesaj için karşılıklı takip gerekli"));
                    finish.setValue(true);
                    return;
                }
                blockRepo.getBlockState(meUid, otherUid, new BlockRepository.Callback<BlockRepository.BlockState>() {
                    @Override public void onSuccess(@NonNull BlockRepository.BlockState bs) {
                        if (bs.iBlocked || bs.blockedMe) {
                            loading.setValue(false);
                            toast.setValue(new UiEvent("Engelli kullanıcı"));
                            finish.setValue(true);
                            return;
                        }
                        startListening();
                    }

                    @Override public void onError(@NonNull String message) {
                        // Still proceed (best-effort), but warn
                        toast.setValue(new UiEvent(message));
                        startListening();
                    }
                });
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
                finish.setValue(true);
            }
        });
    }

    private void startListening() {
        if (msgReg != null) return;
        msgReg = chatRepo.listenMessages(chatId, new ChatRepository.Callback<List<Message>>() {
            @Override public void onSuccess(@NonNull List<Message> data) {
                loading.setValue(false);
                messages.setValue(data);
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void send(@NonNull String text) {
        if (meUid == null || otherUid == null || chatId == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        loading.setValue(true);
        chatRepo.sendMessage(chatId, meUid, otherUid, meUid, trimmed, new ChatRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) { loading.setValue(false); }
            @Override public void onError(@NonNull String message) { loading.setValue(false); toast.setValue(new UiEvent(message)); }
        });
    }

    public boolean isMine(@NonNull Message m) {
        return meUid != null && m != null && meUid.equals(m.fromUid);
    }

    @Override
    protected void onCleared() {
        if (msgReg != null) {
            msgReg.remove();
            msgReg = null;
        }
        super.onCleared();
    }
}
