// File: app/src/main/java/com/pinli/app/data/FirebaseRefs.java
package com.pinli.app.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public final class FirebaseRefs {
    private FirebaseRefs() {}

    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    public static FirebaseStorage storage() { return FirebaseStorage.getInstance(); }

    public static final String COL_USERS = "users";
    public static final String COL_USER_LOCATIONS = "userLocations";
    public static final String COL_PLACES = "places";
    public static final String COL_PLACE_SUGGESTIONS = "placeSuggestions";
    public static final String COL_STATUSES = "statuses";
    public static final String COL_FOLLOW_REQUESTS = "followRequests";
    public static final String COL_FOLLOWS = "follows";
    public static final String COL_CHATS = "chats";
    public static final String COL_MESSAGES = "messages";
    public static final String COL_BLOCKS = "blocks";
    public static final String COL_REPORTS = "reports";
}
