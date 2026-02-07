// File: app/src/main/java/com/pinli/app/ui/main/status/StatusViewModel.java
package com.pinli.app.ui.main.status;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.repo.StatusRepository;

public class StatusViewModel extends ViewModel {
    private final StatusRepository repo = new StatusRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }

    public void setEmoji(@NonNull String emoji) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }
        loading.setValue(true);
        repo.setEmojiStatus(uid, emoji, new StatusRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) { loading.setValue(false); toast.setValue(new UiEvent("OK")); }
            @Override public void onError(@NonNull String message) { loading.setValue(false); toast.setValue(new UiEvent(message)); }
        });
    }

    public void setPhoto(@NonNull Uri uri) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }
        loading.setValue(true);
        repo.setPhotoStatus(uid, uri, new StatusRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) { loading.setValue(false); toast.setValue(new UiEvent("OK")); }
            @Override public void onError(@NonNull String message) { loading.setValue(false); toast.setValue(new UiEvent(message)); }
        });
    }
}
