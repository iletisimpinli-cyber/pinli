// File: app/src/main/java/com/pinli/app/data/model/UserLocation.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class UserLocation {
    public String uid;
    public double lat;
    public double lng;
    public String geohash;
    public boolean isHidden;
    public long hiddenUntil;
    public long updatedAt;

    public UserLocation() {}
}
