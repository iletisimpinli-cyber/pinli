// File: app/src/main/java/com/pinli/app/data/model/PlaceSuggestion.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class PlaceSuggestion {
    public String id;
    public String uid;
    public String name;
    public String type;
    public double lat;
    public double lng;
    public String note;
    public String status; // "pending" | "approved" | "rejected"
    public long createdAt;

    public PlaceSuggestion() {}
}

