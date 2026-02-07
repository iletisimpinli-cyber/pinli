// File: app/src/main/java/com/pinli/app/data/model/Block.java
package com.pinli.app.data.model;

import androidx.annotation.Keep;

@Keep
public class Block {
    public String id;
    public String blockerUid;
    public String blockedUid;
    public long createdAt;

    public Block() {}
}
