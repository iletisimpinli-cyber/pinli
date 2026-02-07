// File: app/src/main/java/com/pinli/app/data/repo/AuthRepository.java
package com.pinli.app.data.repo;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.pinli.app.core.Result;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.util.Logx;

import java.util.concurrent.TimeUnit;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth = FirebaseRefs.auth();

    public interface CodeSentCallback {
        void onCodeSent(@NonNull String verificationId);
        void onError(@NonNull String message);
    }

    public interface SignInCallback {
        void onSignedIn(@NonNull String uid);
        void onError(@NonNull String message);
    }

    public void sendSms(Activity activity, String phoneE164, CodeSentCallback cb) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneE164)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                auth.signInWithCredential(credential)
                                        .addOnSuccessListener(r -> {
                                            if (r.getUser() != null) cb.onCodeSent("AUTO");
                                        })
                                        .addOnFailureListener(e -> {
                                            Logx.e(TAG, "auto verify failed", e);
                                            cb.onError(e.getMessage() != null ? e.getMessage() : "Auth error");
                                        });
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Logx.e(TAG, "verification failed", e);
                                cb.onError(e.getMessage() != null ? e.getMessage() : "Auth error");
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                cb.onCodeSent(verificationId);
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void verifyCode(String verificationId, String code, SignInCallback cb) {
        PhoneAuthCredential cred = PhoneAuthProvider.getCredential(verificationId, code);
        auth.signInWithCredential(cred)
                .addOnSuccessListener(r -> {
                    if (r.getUser() != null) cb.onSignedIn(r.getUser().getUid());
                    else cb.onError("Auth error");
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "Auth error"));
    }

    public Result<String> currentUid() {
        if (auth.getCurrentUser() == null) return Result.failure("No user");
        return Result.success(auth.getCurrentUser().getUid());
    }

    public void signOut() { auth.signOut(); }
}
