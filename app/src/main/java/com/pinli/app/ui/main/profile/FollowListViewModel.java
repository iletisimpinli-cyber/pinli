// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowListViewModel.java
package com.pinli.app.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.User;
import com.pinli.app.data.repo.FollowRepository;
import com.pinli.app.data.repo.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FollowListViewModel extends ViewModel {
    private final FollowRepository followRepo = new FollowRepository();
    private final UserRepository userRepo = new UserRepository();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<User>> users = new MutableLiveData<>(new ArrayList<>());
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<List<User>> users() { return users; }
    public LiveData<UiEvent> toast() { return toast; }

    public void load(@NonNull String type) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) {
            toast.setValue(new UiEvent("Auth error"));
            return;
        }
        loading.setValue(true);
        FollowRepository.Callback<Set<String>> cb = new FollowRepository.Callback<Set<String>>() {
            @Override public void onSuccess(@NonNull Set<String> data) {
                loadUsers(new ArrayList<>(data));
            }
            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        };

        if (FollowListActivity.TYPE_FOLLOWING.equals(type)) {
            followRepo.loadFollowing(uid, cb);
        } else {
            followRepo.loadFollowers(uid, cb);
        }
    }

    private void loadUsers(@NonNull List<String> uids) {
        if (uids.isEmpty()) {
            loading.setValue(false);
            users.setValue(new ArrayList<>());
            return;
        }
        List<User> out = new ArrayList<>();
        loadNextUser(uids, 0, out);
    }

    private void loadNextUser(@NonNull List<String> uids, int index, @NonNull List<User> out) {
        if (index >= uids.size()) {
            loading.setValue(false);
            users.setValue(out);
            return;
        }
        String uid = uids.get(index);
        userRepo.getUser(uid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                out.add(data);
                loadNextUser(uids, index + 1, out);
            }

            @Override public void onError(@NonNull String message) {
                loadNextUser(uids, index + 1, out);
            }
        });
    }
}