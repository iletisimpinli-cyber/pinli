// File: app/src/main/java/com/pinli/app/ui/auth/LoginViewModel.java
package com.pinli.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.repo.AuthRepository;
import com.pinli.app.data.repo.UserRepository;

public class LoginViewModel extends ViewModel {
    private final AuthRepository authRepo = new AuthRepository();
    private final UserRepository userRepo = new UserRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();
    private final MutableLiveData<String> verificationId = new MutableLiveData<>(null);
    private final LiveEvent<Boolean> goMain = new LiveEvent<>();

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<String> verificationId() { return verificationId; }
    public LiveData<Boolean> goMain() { return goMain; }

    public AuthRepository authRepo() { return authRepo; }

    // ✅ Activity dışarıdan loading.setValue çağırmasın diye bunları ekledik
    public void setLoading(boolean isLoading) { loading.setValue(isLoading); }
    public void showError(@NonNull String message) {
        loading.setValue(false);
        toast.setValue(new UiEvent(message));
    }

    public void onSmsSent(@NonNull String vid) {
        verificationId.setValue(vid);
    }

    public void onSignedInEnsureUser(@NonNull String uid) {
        loading.setValue(true);
        String phone = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()
                : "";
        userRepo.ensureUserDoc(uid, phone != null ? phone : "", new UserRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                goMain.setValue(true);
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public boolean isSignedIn() {
        return FirebaseRefs.auth().getCurrentUser() != null;
    }
}
