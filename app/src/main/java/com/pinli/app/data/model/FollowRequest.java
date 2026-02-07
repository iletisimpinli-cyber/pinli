// File: app/src/main/java/com/pinli/app/data/model/FollowRequest.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class FollowRequest {
    public String id;
    public String fromUid;
    public String toUid;
    public String status; // "pending", "accepted", "rejected"
    public long createdAt;

    public FollowRequest() {}
}
