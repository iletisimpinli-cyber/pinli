// File: app/src/main/java/com/pinli/app/data/model/Place.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class Place {
    public String id;
    public String name;
    public String type; // "cami" | "isyeri" | ...
    public double lat;
    public double lng;
    public String geohash;
    public long createdAt;

    public Place() {}
}
