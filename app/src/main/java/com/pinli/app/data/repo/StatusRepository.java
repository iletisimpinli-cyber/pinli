// File: app/src/main/java/com/pinli/app/data/repo/StatusRepository.java
package com.pinli.app.data.repo;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.util.Time;

import java.util.HashMap;
import java.util.Map;

public class StatusRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public void setEmojiStatus(@NonNull String uid, @NonNull String emoji, Callback<Boolean> cb) {
        long now = Time.now();
        String id = uid + "_" + now;
        Map<String, Object> s = new HashMap<>();
        s.put("id", id);
        s.put("uid", uid);
        s.put("type", "emoji");
        s.put("emoji", emoji);
        s.put("photoUrl", null);
        s.put("createdAt", now);
        s.put("expiresAt", now + 5L * 60L * 60L * 1000L);

        db.collection(FirebaseRefs.COL_STATUSES).document(id)
                .set(s)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void setPhotoStatus(@NonNull String uid, @NonNull Uri localUri, Callback<Boolean> cb) {
        long now = Time.now();
        String id = uid + "_" + now;
        StorageReference ref = FirebaseRefs.storage().getReference()
                .child("statusPhotos")
                .child(uid)
                .child(id + ".jpg");

        ref.putFile(localUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(url -> {
                            Map<String, Object> s = new HashMap<>();
                            s.put("id", id);
                            s.put("uid", uid);
                            s.put("type", "photo");
                            s.put("emoji", null);
                            s.put("photoUrl", url.toString());
                            s.put("createdAt", now);
                            s.put("expiresAt", now + 5L * 60L * 60L * 1000L);

                            db.collection(FirebaseRefs.COL_STATUSES).document(id)
                                    .set(s)
                                    .addOnSuccessListener(v -> cb.onSuccess(true))
                                    .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
                        })
                        .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "Upload error")))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "Upload error"));
    }
}
