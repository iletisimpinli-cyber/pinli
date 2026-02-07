// File: app/src/main/java/com/pinli/app/data/repo/FollowRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.FollowRequest;
import com.pinli.app.util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FollowRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public static class FollowState {
        public boolean iFollow;
        public boolean followsMe;
        public boolean outgoingRequestPending;
        public boolean incomingRequestPending;

        public boolean canMessage() { return iFollow && followsMe; }
    }

    private static String followId(String fromUid, String toUid) { return fromUid + "_" + toUid; }

    public void followPublic(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        String id = followId(fromUid, toUid);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("fromUid", fromUid);
        data.put("toUid", toUid);
        data.put("createdAt", Time.now());
        db.collection(FirebaseRefs.COL_FOLLOWS).document(id)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void unfollow(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        String id = followId(fromUid, toUid);
        db.collection(FirebaseRefs.COL_FOLLOWS).document(id)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void requestFollowPrivate(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        String id = followId(fromUid, toUid);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("fromUid", fromUid);
        data.put("toUid", toUid);
        data.put("status", "pending");
        data.put("createdAt", Time.now());
        db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS).document(id)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void cancelRequest(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        String id = followId(fromUid, toUid);
        db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS).document(id)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void rejectRequest(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        // Simpler MVP: delete request doc.
        cancelRequest(fromUid, toUid, cb);
    }

    public void acceptRequest(@NonNull String fromUid, @NonNull String toUid, Callback<Boolean> cb) {
        // fromUid requested to follow toUid. On accept, create follow(fromUid -> toUid) and remove request.
        String id = followId(fromUid, toUid);

        Map<String, Object> follow = new HashMap<>();
        follow.put("id", id);
        follow.put("fromUid", fromUid);
        follow.put("toUid", toUid);
        follow.put("createdAt", Time.now());

        var reqRef = db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS).document(id);
        var folRef = db.collection(FirebaseRefs.COL_FOLLOWS).document(id);

        Tasks.whenAllSuccess(
                        folRef.set(follow),
                        reqRef.delete()
                )
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void loadIncomingRequests(@NonNull String toUid, Callback<List<FollowRequest>> cb) {
        db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS)
                .whereEqualTo("toUid", toUid)
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .addOnSuccessListener(qs -> {
                    List<FollowRequest> out = new ArrayList<>();
                    qs.getDocuments().forEach(d -> {
                        FollowRequest fr = d.toObject(FollowRequest.class);
                        if (fr != null) out.add(fr);
                    });
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void loadIncomingRequestsCount(@NonNull String toUid, Callback<Integer> cb) {
        db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS)
                .whereEqualTo("toUid", toUid)
                .whereEqualTo("status", "pending")
                .limit(1000)
                .get()
                .addOnSuccessListener(qs -> cb.onSuccess(qs.size()))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void loadFollowing(@NonNull String uid, Callback<Set<String>> cb) {
        db.collection(FirebaseRefs.COL_FOLLOWS)
                .whereEqualTo("fromUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(2000)
                .get()
                .addOnSuccessListener(qs -> {
                    Set<String> out = new HashSet<>();
                    qs.getDocuments().forEach(d -> {
                        String to = d.getString("toUid");
                        if (to != null) out.add(to);
                    });
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void loadFollowers(@NonNull String uid, Callback<Set<String>> cb) {
        db.collection(FirebaseRefs.COL_FOLLOWS)
                .whereEqualTo("toUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(2000)
                .get()
                .addOnSuccessListener(qs -> {
                    Set<String> out = new HashSet<>();
                    qs.getDocuments().forEach(d -> {
                        String from = d.getString("fromUid");
                        if (from != null) out.add(from);
                    });
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void getFollowState(@NonNull String meUid, @NonNull String otherUid, Callback<FollowState> cb) {
        String me_other = followId(meUid, otherUid);
        String other_me = followId(otherUid, meUid);

        var f1 = db.collection(FirebaseRefs.COL_FOLLOWS).document(me_other).get();
        var f2 = db.collection(FirebaseRefs.COL_FOLLOWS).document(other_me).get();
        var r1 = db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS).document(me_other).get();
        var r2 = db.collection(FirebaseRefs.COL_FOLLOW_REQUESTS).document(other_me).get();

        Tasks.whenAllSuccess(f1, f2, r1, r2)
                .addOnSuccessListener(list -> {
                    FollowState s = new FollowState();
                    if (list.size() > 0 && list.get(0) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        s.iFollow = ((com.google.firebase.firestore.DocumentSnapshot) list.get(0)).exists();
                    }
                    if (list.size() > 1 && list.get(1) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        s.followsMe = ((com.google.firebase.firestore.DocumentSnapshot) list.get(1)).exists();
                    }
                    if (list.size() > 2 && list.get(2) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        var ds = (com.google.firebase.firestore.DocumentSnapshot) list.get(2);
                        s.outgoingRequestPending = ds.exists() && "pending".equals(ds.getString("status"));
                    }
                    if (list.size() > 3 && list.get(3) instanceof com.google.firebase.firestore.DocumentSnapshot) {
                        var ds = (com.google.firebase.firestore.DocumentSnapshot) list.get(3);
                        s.incomingRequestPending = ds.exists() && "pending".equals(ds.getString("status"));
                    }
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public static boolean isMutual(Set<String> following, Set<String> followers, String otherUid) {
        return following != null && followers != null && following.contains(otherUid) && followers.contains(otherUid);
    }
}
