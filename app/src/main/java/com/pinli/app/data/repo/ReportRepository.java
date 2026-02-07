// File: app/src/main/java/com/pinli/app/data/repo/ReportRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.util.Time;

import java.util.HashMap;
import java.util.Map;

public class ReportRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public void reportUser(@NonNull String reporterUid, @NonNull String targetUid, @NonNull String reason, Callback<Boolean> cb) {
        String id = db.collection(FirebaseRefs.COL_REPORTS).document().getId();
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("reporterUid", reporterUid);
        data.put("targetType", "user");
        data.put("targetId", targetUid);
        data.put("reason", reason);
        data.put("createdAt", Time.now());

        db.collection(FirebaseRefs.COL_REPORTS).document(id)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }
}
