package com.example.roomatch.model.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final Context context;

    public ChatRepository(Context context) {
        this.context = context.getApplicationContext(); // שמירה על context בטוח
    }


    public Task<DocumentReference> sendMessage(String chatId, Message message) {
        if (chatId == null || message == null || message.getFromUserId() == null || message.getToUserId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid message parameters"));
        }
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(message.getFromUserId().equals(message.getToUserId()));
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .add(message)
                .addOnSuccessListener(docRef -> {});
    }

    public Task<List<DocumentSnapshot>> getAllGroupChatsForUser(String userId) {
        Task<QuerySnapshot> byMember = db.collection("group_chats")
                .whereArrayContains("memberIds", userId)
                .get();

        Task<QuerySnapshot> byOwner = db.collection("group_chats")
                .whereEqualTo("ownerId", userId)
                .get();

        return Tasks.whenAllSuccess(byMember, byOwner)
                .continueWith(task -> {
                    Set<String> seen = new HashSet<>();
                    List<DocumentSnapshot> combined = new ArrayList<>();

                    for (Object result : task.getResult()) {
                        QuerySnapshot qs = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            if (seen.add(doc.getId())) {
                                combined.add(doc);
                            }
                        }
                    }

                    return combined;
                });
    }



    public Task<DocumentSnapshot> getLastGroupMessage(String groupChatId) {
        return db.collection("group_messages")
                .document(groupChatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        return null;
                    }
                    return task.getResult().getDocuments().get(0);
                });
    }


    private void sendNotificationToRecipient(Message message) {
        db.collection("users").document(message.getToUserId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String token = snapshot.getString("fcmToken");
                    if (token != null) {
                        try {
                            JSONObject notification = new JSONObject();
                            notification.put("to", token);

                            JSONObject body = new JSONObject();
                            body.put("title", "Roomatch");
                            body.put("body", "הודעה חדשה לגבי דירה שלך");
                            notification.put("notification", body);

                            JsonObjectRequest request = new JsonObjectRequest(
                                    Request.Method.POST,
                                    "https://fcm.googleapis.com/fcm/send",
                                    notification,
                                    response -> Log.d("FCM", "Notification sent"),
                                    error -> Log.e("FCM", "Notification error", error)
                            ) {
                                @Override
                                public Map<String, String> getHeaders() {
                                    Map<String, String> headers = new HashMap<>();
                                    headers.put("Authorization", "key=AIzaSyBHvJSGRqykrqDtYB-TN8C4C70OYPi_IMI"); // ⬅️ שים פה את מפתח השרת שלך
                                    headers.put("Content-Type", "application/json");
                                    return headers;
                                }
                            };

                            Volley.newRequestQueue(context).add(request);

                        } catch (Exception e) {
                            Log.e("FCM", "Failed to send notification", e);
                        }
                    }
                });
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
    public Task<QuerySnapshot> getAllChatMessages() {
        return db.collectionGroup("chat").orderBy("timestamp", Query.Direction.DESCENDING).get();
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