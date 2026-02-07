// File: app/src/main/java/com/pinli/app/data/model/User.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class User {
    public String uid;
    public String phone;
    public String displayName;
    public String photoUrl;
    public boolean isPrivate;
    public long createdAt;

    public User() {}
}
