// File: app/src/main/java/com/pinli/app/ui/main/profile/FollowRequestsViewModel.java
package com.pinli.app.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.FollowRequest;
import com.pinli.app.data.model.User;
import com.pinli.app.data.repo.FollowRepository;
import com.pinli.app.data.repo.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowRequestsViewModel extends ViewModel {

    public static class Row {
        public FollowRequest request;
        public User user;
    }

    private final FollowRepository followRepo = new FollowRepository();
    private final UserRepository userRepo = new UserRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();
    private final MutableLiveData<List<Row>> rows = new MutableLiveData<>(new ArrayList<>());

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<List<Row>> rows() { return rows; }

    public void load() {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }

        loading.setValue(true);
        followRepo.loadIncomingRequests(uid, new FollowRepository.Callback<List<FollowRequest>>() {
            @Override public void onSuccess(@NonNull List<FollowRequest> data) {
                if (data.isEmpty()) {
                    loading.setValue(false);
                    rows.setValue(new ArrayList<>());
                    return;
                }

                // fetch user docs for each fromUid with cache
                Map<String, User> cache = new HashMap<>();
                List<Row> out = new ArrayList<>();
                fetchNext(data, 0, cache, out);
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    private void fetchNext(@NonNull List<FollowRequest> reqs, int index,
                           @NonNull Map<String, User> cache,
                           @NonNull List<Row> out) {
        if (index >= reqs.size()) {
            loading.setValue(false);
            rows.setValue(out);
            return;
        }
        FollowRequest fr = reqs.get(index);
        if (fr == null || fr.fromUid == null) {
            fetchNext(reqs, index + 1, cache, out);
            return;
        }
        if (cache.containsKey(fr.fromUid)) {
            Row r = new Row();
            r.request = fr;
            r.user = cache.get(fr.fromUid);
            out.add(r);
            fetchNext(reqs, index + 1, cache, out);
            return;
        }

        userRepo.getUser(fr.fromUid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                cache.put(fr.fromUid, data);
                Row r = new Row();
                r.request = fr;
                r.user = data;
                out.add(r);
                fetchNext(reqs, index + 1, cache, out);
            }

            @Override public void onError(@NonNull String message) {
                // still add row with null user
                Row r = new Row();
                r.request = fr;
                r.user = null;
                out.add(r);
                fetchNext(reqs, index + 1, cache, out);
            }
        });
    }

    public void accept(@NonNull FollowRequest fr) {
        String toUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (toUid == null || fr.fromUid == null) return;

        loading.setValue(true);
        followRepo.acceptRequest(fr.fromUid, toUid, new FollowRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                toast.setValue(new UiEvent("İstek kabul edildi"));
                load();
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void reject(@NonNull FollowRequest fr) {
        String toUid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (toUid == null || fr.fromUid == null) return;

        loading.setValue(true);
        followRepo.rejectRequest(fr.fromUid, toUid, new FollowRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                toast.setValue(new UiEvent("İstek reddedildi"));
                load();
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }
}
