// File: app/src/main/java/com/pinli/app/ui/main/profile/ProfileViewModel.java
package com.pinli.app.ui.main.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.User;
import com.pinli.app.data.model.UserLocation;
import com.pinli.app.data.repo.AuthRepository;
import com.pinli.app.data.repo.FollowRepository;
import com.pinli.app.data.repo.LocationRepository;
import com.pinli.app.data.repo.UserRepository;

import com.google.firebase.firestore.ListenerRegistration;

public class ProfileViewModel extends ViewModel {
    private final AuthRepository authRepo = new AuthRepository();
    private final LocationRepository locationRepo = new LocationRepository();
    private final UserRepository userRepo = new UserRepository();
    private final FollowRepository followRepo = new FollowRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();
    private final LiveEvent<Boolean> signedOut = new LiveEvent<>();

    private final MutableLiveData<UserLocation> myLocation = new MutableLiveData<>(null);
    private final MutableLiveData<User> myUser = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> incomingRequestCount = new MutableLiveData<>(0);

    private ListenerRegistration myLocReg;

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<Boolean> signedOut() { return signedOut; }
    public LiveData<UserLocation> myLocation() { return myLocation; }
    public LiveData<User> myUser() { return myUser; }
    public LiveData<Integer> incomingRequestCount() { return incomingRequestCount; }

    public void start() {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;

        if (myLocReg == null) {
            myLocReg = locationRepo.listenMyLocation(uid, new LocationRepository.NullableCallback<UserLocation>() {
                @Override public void onSuccess(UserLocation data) {
                    myLocation.postValue(data);
                }

                @Override public void onError(@NonNull String message) {
                    toast.postValue(new UiEvent(message));
                }
            });
        }

        // user doc
        userRepo.getUser(uid, new UserRepository.Callback<User>() {
            @Override public void onSuccess(@NonNull User data) {
                myUser.setValue(data);
            }

            @Override public void onError(@NonNull String message) {
                toast.setValue(new UiEvent(message));
            }
        });

        // pending requests count
        refreshIncomingRequestsCount();
    }

    public void refreshIncomingRequestsCount() {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;
        followRepo.loadIncomingRequestsCount(uid, new FollowRepository.Callback<Integer>() {
            @Override public void onSuccess(@NonNull Integer data) { incomingRequestCount.setValue(data); }
            @Override public void onError(@NonNull String message) { /* ignore */ }
        });
    }

    public void setPrivate(boolean isPrivate) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;

        loading.setValue(true);
        userRepo.setPrivate(uid, isPrivate, new UserRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                User u = myUser.getValue();
                if (u != null) {
                    u.isPrivate = isPrivate;
                    myUser.setValue(u);
                }
                toast.setValue(new UiEvent(isPrivate ? "Hesap gizli" : "Hesap açık"));
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void updateProfile(@NonNull String displayName, @NonNull String bio, @NonNull String successMessage) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }

        loading.setValue(true);
        userRepo.setProfile(uid, displayName, bio, new UserRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                User u = myUser.getValue();
                if (u != null) {
                    u.displayName = displayName;
                    u.bio = bio;
                    myUser.setValue(u);
                }
                toast.setValue(new UiEvent(successMessage));
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void updateDisplayName(@NonNull String displayName, @NonNull String successMessage) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }

        loading.setValue(true);
        userRepo.setDisplayName(uid, displayName, new UserRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                User u = myUser.getValue();
                if (u != null) {
                    u.displayName = displayName;
                    myUser.setValue(u);
                }
                toast.setValue(new UiEvent(successMessage));
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void toggleHidden24h(boolean hide) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Auth error")); return; }

        UserLocation ul = myLocation.getValue();
        if (ul == null || ul.geohash == null || ul.geohash.isEmpty()) {
            toast.setValue(new UiEvent("Önce konum ayarlayın"));
            return;
        }
        loading.setValue(true);
        locationRepo.setHidden24h(uid, hide, new LocationRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                toast.setValue(new UiEvent(hide ? "Konum 24 saat gizlendi" : "Konum görünür"));
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void signOut() {
        authRepo.signOut();
        signedOut.setValue(true);
    }

    @Override
    protected void onCleared() {
        if (myLocReg != null) {
            myLocReg.remove();
            myLocReg = null;
        }
        super.onCleared();
    }
}
