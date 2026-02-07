// File: app/src/main/java/com/pinli/app/data/repo/LocationRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.UserLocation;
import com.pinli.app.util.Geohash;
import com.pinli.app.util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pinli.app.util.Geo;

public class LocationRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public interface NullableCallback<T> {
        void onSuccess(@Nullable T data);
        void onError(@NonNull String message);
    }

    public void saveSnappedLocation(@NonNull String uid, double rawLat, double rawLng, Callback<Boolean> cb) {
        double sLat = Geo.snapLatLng(rawLat, 200.0);
        double sLng = Geo.snapLng(rawLng, sLat, 200.0);
        String geohash = Geohash.encode(sLat, sLng, 8);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("lat", sLat);
        data.put("lng", sLng);
        data.put("geohash", geohash);
        data.put("isHidden", false);
        data.put("hiddenUntil", 0L);
        data.put("updatedAt", Time.now());

        db.collection(FirebaseRefs.COL_USER_LOCATIONS).document(uid)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    /**
     * Returns user's location doc if exists, otherwise null.
     */
    public void getMyLocation(@NonNull String uid, @NonNull NullableCallback<UserLocation> cb) {
        db.collection(FirebaseRefs.COL_USER_LOCATIONS).document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        cb.onSuccess(null);
                        return;
                    }
                    UserLocation ul = snap.toObject(UserLocation.class);
                    cb.onSuccess(ul);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    /**
     * Live listener for current user's location doc.
     */
    public ListenerRegistration listenMyLocation(@NonNull String uid, @NonNull NullableCallback<UserLocation> cb) {
        return db.collection(FirebaseRefs.COL_USER_LOCATIONS).document(uid)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        cb.onError(err.getMessage() != null ? err.getMessage() : "DB error");
                        return;
                    }
                    if (snap == null || !snap.exists()) {
                        cb.onSuccess(null);
                        return;
                    }
                    UserLocation ul = snap.toObject(UserLocation.class);
                    cb.onSuccess(ul);
                });
    }

    public void setHidden24h(@NonNull String uid, boolean hidden, Callback<Boolean> cb) {
        // Update only if location doc exists (avoid creating incomplete docs without lat/lng/geohash).
        db.collection(FirebaseRefs.COL_USER_LOCATIONS).document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        cb.onError("Önce konum ayarlayın");
                        return;
                    }
                    long until = hidden ? Time.now() + 24L * 60L * 60L * 1000L : 0L;
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("isHidden", hidden);
                    upd.put("hiddenUntil", until);
                    upd.put("updatedAt", Time.now());
                    db.collection(FirebaseRefs.COL_USER_LOCATIONS).document(uid)
                            .update(upd)
                            .addOnSuccessListener(v -> cb.onSuccess(true))
                            .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void loadVisibleInViewport(double centerLat, double centerLng, double radiusMeters, Callback<List<UserLocation>> cb) {
        String precision = Geohash.prefixForRadiusMeters(radiusMeters);
        int p = Integer.parseInt(precision);
        String prefix = Geohash.encode(centerLat, centerLng, p);
        String[] range = Geohash.rangeForPrefix(prefix);

        db.collection(FirebaseRefs.COL_USER_LOCATIONS)
                .orderBy("geohash", Query.Direction.ASCENDING)
                .startAt(range[0])
                .endAt(range[1])
                .limit(800)
                .get()
                .addOnSuccessListener(qs -> {
                    long now = Time.now();
                    List<UserLocation> out = new ArrayList<>();
                    qs.getDocuments().forEach(d -> {
                        UserLocation ul = d.toObject(UserLocation.class);
                        if (ul == null) return;
                        if (ul.isHidden) {
                            if (ul.hiddenUntil > 0 && ul.hiddenUntil <= now) {
                                // UI-side expire: treat as visible but DB still hidden until updated by user action.
                                // For safety, we still keep it hidden in UI unless explicitly toggled off.
                                return;
                            }
                            return;
                        }
                        out.add(ul);
                    });
                    cb.onSuccess(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }
}
