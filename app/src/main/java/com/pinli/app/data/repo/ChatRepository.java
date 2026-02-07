// File: app/src/main/java/com/pinli/app/data/repo/ChatRepository.java
package com.pinli.app.data.repo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.pinli.app.data.FirebaseRefs;
import com.pinli.app.data.model.Chat;
import com.pinli.app.data.model.Message;
import com.pinli.app.util.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private final FirebaseFirestore db = FirebaseRefs.db();

    public interface Callback<T> {
        void onSuccess(@NonNull T data);
        void onError(@NonNull String message);
    }

    public static String chatIdFor(@NonNull String a, @NonNull String b) {
        return (a.compareTo(b) < 0) ? (a + "_" + b) : (b + "_" + a);
    }

    public void ensureChat(@NonNull String uidA, @NonNull String uidB, Callback<String> cb) {
        String chatId = chatIdFor(uidA, uidB);
        DocumentReference ref = db.collection(FirebaseRefs.COL_CHATS).document(chatId);
        ref.get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        cb.onSuccess(chatId);
                        return;
                    }
                    long now = Time.now();
                    Map<String, Object> chat = new HashMap<>();
                    chat.put("id", chatId);
                    chat.put("uidA", uidA);
                    chat.put("uidB", uidB);
                    chat.put("participants", Arrays.asList(uidA, uidB));
                    chat.put("createdAt", now);
                    chat.put("lastMessage", "");
                    chat.put("lastAt", now);

                    ref.set(chat)
                            .addOnSuccessListener(v -> cb.onSuccess(chatId))
                            .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public void sendMessage(@NonNull String chatId, @NonNull String uidA, @NonNull String uidB,
                            @NonNull String fromUid, @NonNull String text, Callback<Boolean> cb) {
        long now = Time.now();
        Map<String, Object> chat = new HashMap<>();
        chat.put("id", chatId);
        chat.put("uidA", uidA);
        chat.put("uidB", uidB);
        chat.put("participants", Arrays.asList(uidA, uidB));
        chat.put("lastMessage", text);
        chat.put("lastAt", now);
        if (!text.isEmpty()) chat.put("updatedAt", now);

        String msgId = db.collection(FirebaseRefs.COL_CHATS).document(chatId)
                .collection(FirebaseRefs.COL_MESSAGES).document().getId();

        Map<String, Object> msg = new HashMap<>();
        msg.put("id", msgId);
        msg.put("chatId", chatId);
        msg.put("fromUid", fromUid);
        msg.put("text", text);
        msg.put("createdAt", now);

        Tasks.whenAllSuccess(
                        db.collection(FirebaseRefs.COL_CHATS).document(chatId).set(chat),
                        db.collection(FirebaseRefs.COL_CHATS).document(chatId)
                                .collection(FirebaseRefs.COL_MESSAGES).document(msgId).set(msg)
                )
                .addOnSuccessListener(v -> cb.onSuccess(true))
                .addOnFailureListener(e -> cb.onError(e.getMessage() != null ? e.getMessage() : "DB error"));
    }

    public Query chatsQuery(@NonNull String uid) {
        return db.collection(FirebaseRefs.COL_CHATS)
                .whereArrayContains("participants", uid)
                .orderBy("lastAt", Query.Direction.DESCENDING)
                .limit(200);
    }

    public Query messagesQuery(@NonNull String chatId) {
        return db.collection(FirebaseRefs.COL_CHATS).document(chatId)
                .collection(FirebaseRefs.COL_MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(400);
    }

    public ListenerRegistration listenChats(@NonNull String uid, Callback<List<Chat>> cb) {
        return chatsQuery(uid).addSnapshotListener((qs, e) -> {
            if (e != null) {
                cb.onError(e.getMessage() != null ? e.getMessage() : "DB error");
                return;
            }
            List<Chat> out = new ArrayList<>();
            if (qs != null) {
                qs.getDocuments().forEach(d -> {
                    Chat c = d.toObject(Chat.class);
                    if (c != null) out.add(c);
                });
            }
            cb.onSuccess(out);
        });
    }

    public ListenerRegistration listenMessages(@NonNull String chatId, Callback<List<Message>> cb) {
        return messagesQuery(chatId).addSnapshotListener((qs, e) -> {
            if (e != null) {
                cb.onError(e.getMessage() != null ? e.getMessage() : "DB error");
                return;
            }
            List<Message> out = new ArrayList<>();
            if (qs != null) {
                qs.getDocuments().forEach(d -> {
                    Message m = d.toObject(Message.class);
                    if (m != null) out.add(m);
                });
            }
            cb.onSuccess(out);
        });
    }

    @Nullable
    public static String otherUidFromChat(@NonNull Chat c, @NonNull String meUid) {
        if (c.uidA != null && c.uidB != null) {
            return meUid.equals(c.uidA) ? c.uidB : c.uidA;
        }
        if (c.participants != null) {
            for (String u : c.participants) {
                if (u != null && !u.equals(meUid)) return u;
            }
        }
        return null;
    }
}
