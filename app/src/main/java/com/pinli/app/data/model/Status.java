// File: app/src/main/java/com/pinli/app/data/model/Status.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class Status {
    public String id;
    public String uid;
    public String type; // "emoji" or "photo"
    public String emoji;
    public String photoUrl;
    public long createdAt;
    public long expiresAt; // now + 5 hours

    public Status() {}
}
