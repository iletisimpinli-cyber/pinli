// File: app/src/main/java/com/pinli/app/data/repo/BlockRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.util.Time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    /** Returns uids that I blocked (outgoing blocks). */
    public void loadBlockedUids(@NonNull String uid, Callback<Set<String>> cb) {
        db.collection(FirebaseRefs.COL_BLOCKS)
                .whereEqualTo("blockerUid", uid)
                .get()
                .addOnSuccessListener(qs -> cb.onSuccess(extractBlocked(qs)))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    /** Returns uids that blocked me (incoming blocks). */
    public void loadBlockers(@NonNull String uid, Callback<Set<String>> cb) {
        db.collection(FirebaseRefs.COL_BLOCKS)
                .whereEqualTo("blockedUid", uid)
                .get()
                .addOnSuccessListener(qs -> cb.onSuccess(extractBlockers(qs)))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    /**
     * Returns all uids that should be hidden from my UI:
     * - users I blocked
     * - users who blocked me
     */
    public void loadBlockedRelatedUids(@NonNull String uid, Callback<Set<String>> cb) {
        com.google.android.gms.tasks.Task<QuerySnapshot> t1 = db.collection(FirebaseRefs.COL_BLOCKS).whereEqualTo("blockerUid", uid).get();
        com.google.android.gms.tasks.Task<QuerySnapshot> t2 = db.collection(FirebaseRefs.COL_BLOCKS).whereEqualTo("blockedUid", uid).get();
        Tasks.whenAllSuccess(t1, t2)
                .addOnSuccessListener(list -> {
                    Set<String> out = new HashSet<>();
                    if (list.size() > 0 && list.get(0) instanceof QuerySnapshot) {
                        out.addAll(extractBlocked((QuerySnapshot) list.get(0)));
                    }
                    if (list.size() > 1 && list.get(1) instanceof QuerySnapshot) {
                        out.addAll(extractBlockers((QuerySnapshot) list.get(1)));
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public static class BlockState {
        public boolean iBlocked;
        public boolean blockedMe;
    }

    public void getBlockState(@NonNull String meUid, @NonNull String otherUid, Callback<BlockState> cb) {
        String outId = meUid + "_" + otherUid;
        String inId = otherUid + "_" + meUid;
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> t1 = db.collection(FirebaseRefs.COL_BLOCKS).document(outId).get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> t2 = db.collection(FirebaseRefs.COL_BLOCKS).document(inId).get();
        Tasks.whenAllSuccess(t1, t2)
                .addOnSuccessListener(list -> {
                    BlockState s = new BlockState();
                    if (list.size() > 0 && list.get(0) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        s.iBlocked = ((com.google.firebase.firestore.DocumentSnapshot) list.get(0)).exists();
                    }
                    if (list.size() > 1 && list.get(1) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        s.blockedMe = ((com.google.firebase.firestore.DocumentSnapshot) list.get(1)).exists();
                    }
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    private Set<String> extractBlocked(QuerySnapshot qs) {
        Set<String> out = new HashSet<>();
        qs.getDocuments().forEach(d -> {
            String blockedUid = d.getString("blockedUid");
            if (blockedUid != null) out.add(blockedUid);
        });
        return out;
    }

    private Set<String> extractBlockers(QuerySnapshot qs) {
        Set<String> out = new HashSet<>();
        qs.getDocuments().forEach(d -> {
            String blockerUid = d.getString("blockerUid");
            if (blockerUid != null) out.add(blockerUid);
        });
        return out;
    }

    public void block(@NonNull String blockerUid, @NonNull String blockedUid, Callback<Boolean> cb) {
        String id = blockerUid + "_" + blockedUid;
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("blockerUid", blockerUid);
        data.put("blockedUid", blockedUid);
        data.put("createdAt", Time.now());
        db.collection(FirebaseRefs.COL_BLOCKS).document(id)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void unblock(@NonNull String blockerUid, @NonNull String blockedUid, Callback<Boolean> cb) {
        String id = blockerUid + "_" + blockedUid;
        db.collection(FirebaseRefs.COL_BLOCKS).document(id)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }
}
