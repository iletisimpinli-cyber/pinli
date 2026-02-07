// File: app/src/main/java/com/pinli/app/data/repo/UserRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.User;
import com.pinli.app.util.Time;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public void ensureUserDoc(@NonNull String uid, @NonNull String phone, Callback<Boolean> cb) {
        DocumentReference ref = db.collection(FirebaseRefs.COL_USERS).document(uid);
        ref.get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        cb.onSuccess(true);
                        return;
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("phone", phone);
                    data.put("displayName", "Pinli");
                    data.put("photoUrl", null);
                    data.put("isPrivate", false);
                    data.put("createdAt", Time.now());
                    ref.set(data)
                            .addOnSuccessListener(v -> cb.onSuccess(true))
                            .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void setPrivate(@NonNull String uid, boolean isPrivate, Callback<Boolean> cb) {
        db.collection(FirebaseRefs.COL_USERS).document(uid)
                .update("isPrivate", isPrivate, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void setDisplayName(@NonNull String uid, @NonNull String displayName, Callback<Boolean> cb) {
        db.collection(FirebaseRefs.COL_USERS).document(uid)
                .update("displayName", displayName, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }


    public void getUser(@NonNull String uid, Callback<User> cb) {
        db.collection(FirebaseRefs.COL_USERS).document(uid)
                .get()
                .addOnSuccessListener(s -> {
                    User u = s.toObject(User.class);
                    if (u == null) cb.onError("User not found");
                    else cb.onSuccess(u);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }
}
