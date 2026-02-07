// File: app/src/main/java/com/pinli/app/ui/main/map/MapViewModel.java
package com.pinli.app.ui.main.map;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pinli.app.core.LiveEvent;
import com.pinli.app.core.UiEvent;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.UserLocation;
import com.pinli.app.data.repo.BlockRepository;
import com.pinli.app.data.repo.LocationRepository;
import com.pinli.app.util.Geo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.firebase.firestore.ListenerRegistration;

public class MapViewModel extends ViewModel {
    private final LocationRepository locationRepo = new LocationRepository();
    private final BlockRepository blockRepo = new BlockRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LiveEvent<UiEvent> toast = new LiveEvent<>();

    private final MutableLiveData<List<UserLocation>> locations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<String>> blocked = new MutableLiveData<>(new HashSet<>());

    private final MutableLiveData<UserLocation> myLocation = new MutableLiveData<>(null);
    private final LiveEvent<UiEvent> locationSaved = new LiveEvent<>();

    private ListenerRegistration myLocReg;

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<UiEvent> toast() { return toast; }
    public LiveData<List<UserLocation>> locations() { return locations; }
    public LiveData<Set<String>> blocked() { return blocked; }
    public LiveData<UserLocation> myLocation() { return myLocation; }
    public LiveData<UiEvent> locationSaved() { return locationSaved; }

    public void startMyLocationListener() {
        if (myLocReg != null) return;
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;
        myLocReg = locationRepo.listenMyLocation(uid, new LocationRepository.NullableCallback<UserLocation>() {
            @Override public void onSuccess(UserLocation data) {
                myLocation.postValue(data);
            }

            @Override public void onError(@NonNull String message) {
                toast.postValue(new UiEvent(message));
            }
        });
    }

    public void saveMyLocation(double rawLat, double rawLng) {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) { toast.setValue(new UiEvent("Giriş hatası")); return; }

        // Precompute snapped values so we can instantly update UI after save.
        double sLat = Geo.snapLatLng(rawLat, 200.0);
        double sLng = Geo.snapLng(rawLng, sLat, 200.0);

        loading.setValue(true);
        locationRepo.saveSnappedLocation(uid, rawLat, rawLng, new LocationRepository.Callback<Boolean>() {
            @Override public void onSuccess(@NonNull Boolean data) {
                loading.setValue(false);
                // Update local state (listener will also fire)
                UserLocation ul = new UserLocation();
                ul.uid = uid;
                ul.lat = sLat;
                ul.lng = sLng;
                ul.isHidden = false;
                ul.hiddenUntil = 0L;
                ul.updatedAt = System.currentTimeMillis();
                myLocation.setValue(ul);
                locationSaved.setValue(new UiEvent("OK"));
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
    }

    public void refreshBlocked() {
        String uid = FirebaseRefs.auth().getCurrentUser() != null ? FirebaseRefs.auth().getCurrentUser().getUid() : null;
        if (uid == null) return;
        blockRepo.loadBlockedRelatedUids(uid, new BlockRepository.Callback<Set<String>>() {
            @Override public void onSuccess(@NonNull Set<String> data) { blocked.setValue(data); }
            @Override public void onError(@NonNull String message) { toast.setValue(new UiEvent(message)); }
        });
    }

    public void load(double centerLat, double centerLng, double radiusMeters) {
        loading.setValue(true);
        locationRepo.loadVisibleInViewport(centerLat, centerLng, radiusMeters, new LocationRepository.Callback<List<UserLocation>>() {
            @Override public void onSuccess(@NonNull List<UserLocation> data) {
                loading.setValue(false);
                Set<String> b = blocked.getValue();
                if (b == null || b.isEmpty()) {
                    locations.setValue(data);
                    return;
                }
                List<UserLocation> filtered = new ArrayList<>();
                for (UserLocation ul : data) {
                    if (ul == null || ul.uid == null) continue;
                    if (!b.contains(ul.uid)) filtered.add(ul);
                }
                locations.setValue(filtered);
            }

            @Override public void onError(@NonNull String message) {
                loading.setValue(false);
                toast.setValue(new UiEvent(message));
            }
        });
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
