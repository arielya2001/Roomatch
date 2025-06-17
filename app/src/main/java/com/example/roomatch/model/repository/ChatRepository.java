package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public Task<DocumentReference> sendMessage(String chatId, Message message) {
        if (chatId == null || message == null || message.getFromUserId() == null || message.getToUserId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid message parameters"));
        }
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(message.getFromUserId().equals(message.getToUserId()));
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .add(message);
    }

    public Task<DocumentReference> sendMessageWithImage(String chatId, Message message, Uri imageUri) {
        if (chatId == null || message == null || message.getFromUserId() == null || message.getToUserId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid message parameters"));
        }
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(message.getFromUserId().equals(message.getToUserId()));

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
                            message.setImageUrl(task.getResult().toString());
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

    public Query getChatMessagesQuery(String chatId) {
        if (chatId == null) {
            throw new IllegalArgumentException("Chat ID cannot be null");
        }
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public Task<Void> markMessagesAsRead(String chatId, String userId) {
        if (chatId == null || userId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid chatId or userId"));
        }
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

    public Task<QuerySnapshot> getChatsForUser(String userId) {
        if (userId == null) {
            return Tasks.forException(new IllegalArgumentException("User ID cannot be null"));
        }
        return db.collectionGroup("chat")
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    public Query getPaginatedChatMessagesQuery(String chatId, int limit) {
        if (chatId == null) {
            throw new IllegalArgumentException("Chat ID cannot be null");
        }
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit);
    }
}