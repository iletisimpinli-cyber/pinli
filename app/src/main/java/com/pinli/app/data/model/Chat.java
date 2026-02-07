// File: app/src/main/java/com/pinli/app/data/model/Chat.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

import java.util.List;

@Keep
public class Chat {
    public String id;

    // Backward-compatible fields
    public String uidA;
    public String uidB;

    // Preferred schema: array-contains query
    public List<String> participants;

    public String lastMessage;
    public long lastAt;
    public long createdAt;

    public Chat() {}
}
