package com.example.roomatch.model.repository;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatRepository {

    private final FirebaseFirestore db      = FirebaseFirestore.getInstance();
    private final FirebaseStorage   storage = FirebaseStorage.getInstance();
    private final FirebaseAuth      auth    = FirebaseAuth.getInstance();

    /**
     * שולח הודעה בצ'אט ספציפי.
     */
    public Task<DocumentReference> sendMessage(String chatId, String fromUserId, String toUserId,
                                               String apartmentId, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("fromUserId", fromUserId);
        message.put("toUserId", toUserId);
        message.put("text", text);
        message.put("timestamp", System.currentTimeMillis());
        message.put("apartmentId", apartmentId);
        message.put("read", fromUserId.equals(toUserId)); // נקרא אם השולח הוא הנמען
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .add(message);
    }

    /**
     * שולח הודעה עם תמונה (אם קיימת).
     */
    public Task<DocumentReference> sendMessageWithImage(String chatId, String fromUserId, String toUserId,
                                                        String apartmentId, String text, Uri imageUri) {
        Map<String, Object> message = new HashMap<>();
        message.put("fromUserId", fromUserId);
        message.put("toUserId", toUserId);
        message.put("text", text);
        message.put("timestamp", System.currentTimeMillis());
        message.put("apartmentId", apartmentId);
        message.put("read", fromUserId.equals(toUserId));

        if (imageUri != null) {
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child("message_images/" + filename);
            return ref.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .continueWithTask(task -> {
                        if (task.isSuccessful()) {
                            message.put("imageUrl", task.getResult().toString());
                        }
                        return db.collection("messages")
                                .document(chatId)
                                .collection("chat")
                                .add(message);
                    });
        } else {
            return db.collection("messages")
                    .document(chatId)
                    .collection("chat")
                    .add(message);
        }
    }

    /**
     * שולף את כל ההודעות בצ'אט ספציפי עם האזנה בזמן אמת.
     */
    public Query getChatMessagesQuery(String chatId) {
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    /**
     * מסמן הודעות כנקראות עבור משתמש ספציפי.
     */
    public Task<Void> markMessagesAsRead(String chatId, String userId) {
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("read", false)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    Task<Void>[] updateTasks = task.getResult().getDocuments().stream()
                            .map(doc -> doc.getReference().update("read", true))
                            .toArray(Task[]::new);
                    return Tasks.whenAll(updateTasks);
                });
    }

    /**
     * שולף את רשימת הצ'אטים עבור משתמש ספציפי.
     */
    public Task<QuerySnapshot> getChatsForUser(String userId) {
        return db.collectionGroup("chat")
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
    /**
     * שולף הודעות בצ'אט עם הגבלה לפגינציה.
     */
    public Query getPaginatedChatMessagesQuery(String chatId, int limit) {
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit);
    }
}
