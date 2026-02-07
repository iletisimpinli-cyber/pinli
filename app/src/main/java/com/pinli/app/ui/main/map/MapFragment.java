// File: app/src/main/java/com/pinli/app/ui/main/map/MapFragment.java
package com.pinli.app.ui.main.map;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pinli.app.R;
import com.pinli.app.databinding.FragmentMapBinding;
import com.pinli.app.util.Debouncer;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding vb;
    private MapViewModel vm;

    private GoogleMap map;
    private ClusterManager<UserClusterItem> clusterManager;
    private final Debouncer idleDebounce = new Debouncer();

    private boolean longPressHintShown = false;

    private static final int MARKER_LIMIT = 600;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vb = FragmentMapBinding.inflate(inflater, container, false);
        return vb.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(MapViewModel.class);

        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (smf != null) smf.getMapAsync(this);

        vm.toast().observe(getViewLifecycleOwner(), e -> {
            if (getActivity() != null) {
                android.widget.Toast.makeText(getActivity(), e.message, android.widget.Toast.LENGTH_LONG).show();
            }
        });

        vm.loading().observe(getViewLifecycleOwner(), isLoading ->
                vb.progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE)
        );

        vm.locations().observe(getViewLifecycleOwner(), userLocations -> {
            if (map == null || clusterManager == null) return;
            clusterManager.clearItems();

            List<UserClusterItem> items = new ArrayList<>();
            int added = 0;
            for (var ul : userLocations) {
                if (ul == null || ul.uid == null) continue;
                items.add(new UserClusterItem(ul.uid, ul.lat, ul.lng));
                added++;
                if (added >= MARKER_LIMIT) break;
            }
            clusterManager.addItems(items);
            clusterManager.cluster();
        });

        vm.myLocation().observe(getViewLifecycleOwner(), ul -> {
            // If user has not set a location yet, guide them.
            if (longPressHintShown) return;
            if (ul == null || ul.geohash == null || ul.geohash.isEmpty()) {
                longPressHintShown = true;
                if (getActivity() != null) {
                    android.widget.Toast.makeText(getActivity(), getString(R.string.long_press_hint), android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });

        vm.locationSaved().observe(getViewLifecycleOwner(), e -> {
            if (e == null) return;
            if (getActivity() != null) {
                android.widget.Toast.makeText(getActivity(), getString(R.string.location_saved), android.widget.Toast.LENGTH_SHORT).show();
            }
            // After saving location, refresh markers for current viewport.
            idleDebounce.submit(200, this::loadForCurrentViewport);
            // Recenter to my snapped location if available.
            var ul = vm.myLocation().getValue();
            if (map != null && ul != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ul.lat, ul.lng), 14f));
            }
        });

        vm.refreshBlocked();
        vm.startMyLocationListener();

        vb.btnRecenter.setOnClickListener(v -> {
            if (map == null) return;
            var ul = vm.myLocation().getValue();
            if (ul != null && ul.geohash != null && !ul.geohash.isEmpty()) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ul.lat, ul.lng), 14f));
            } else {
                // Fallback: Istanbul center
                LatLng ist = new LatLng(41.0082, 28.9784);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(ist, 12f));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;

        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(true);

        clusterManager = new ClusterManager<>(requireContext(), map);
        clusterManager.setOnClusterItemClickListener(item -> {
            if (item == null || item.getUid() == null) return true;
            UserBottomSheetDialog.newInstance(item.getUid())
                    .show(getChildFragmentManager(), "user_sheet");
            return true;
        });
        map.setOnCameraIdleListener(() -> {
            clusterManager.onCameraIdle();
            idleDebounce.submit(450, this::loadForCurrentViewport);
        });
        map.setOnMarkerClickListener(clusterManager);

        map.setOnMapLongClickListener(latLng -> {
            if (vb != null) vb.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.location_set_confirm_title)
                    .setMessage(getString(R.string.location_set_confirm_msg))
                    .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                    .setPositiveButton(R.string.ok, (d, w) -> vm.saveMyLocation(latLng.latitude, latLng.longitude))
                    .show();
        });

        // Initial camera: use my saved location if exists, otherwise Istanbul.
        var ul = vm.myLocation().getValue();
        if (ul != null && ul.geohash != null && !ul.geohash.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ul.lat, ul.lng), 14f));
        } else {
            LatLng ist = new LatLng(41.0082, 28.9784);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ist, 12f));
        }
        loadForCurrentViewport();
    }

    private void loadForCurrentViewport() {
        if (map == null) return;
        LatLng center = map.getCameraPosition().target;
        float[] results = new float[1];
        try {
            android.location.Location.distanceBetween(
                    map.getProjection().getVisibleRegion().nearLeft.latitude,
                    map.getProjection().getVisibleRegion().nearLeft.longitude,
                    map.getProjection().getVisibleRegion().farRight.latitude,
                    map.getProjection().getVisibleRegion().farRight.longitude,
                    results
            );
        } catch (Exception ignored) {}

        double diag = results[0] > 0 ? results[0] : 3000.0;
        double radius = Math.max(500.0, Math.min(8000.0, diag / 2.0));

        vm.load(center.latitude, center.longitude, radius);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.refreshBlocked();
            idleDebounce.submit(250, this::loadForCurrentViewport);
        }
    }

    @Override
    public void onDestroyView() {
        idleDebounce.cancel();
        vb = null;
        super.onDestroyView();
    }
}
