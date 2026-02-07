// File: app/src/main/java/com/pinli/app/data/model/Message.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class Message {
    public String id;
    public String chatId;
    public String fromUid;
    public String text;
    public long createdAt;

    public Message() {}
}
