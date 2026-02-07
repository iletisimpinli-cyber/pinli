// File: app/src/main/java/com/pinli/app/ui/main/map/UserClusterItem.java
package com.pinli.app.ui.main.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class UserClusterItem implements ClusterItem {

    private final LatLng position;
    private final String uid;

    public UserClusterItem(String uid, double lat, double lng) {
        this.uid = uid;
        this.position = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    // ✅ maps-utils yeni sürümlerde zorunlu hale geldi
    @Override
    public Float getZIndex() {
        return 0f;
    }

    public String getUid() {
        return uid;
    }
}
