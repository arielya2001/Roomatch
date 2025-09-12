package com.example.roomatch.model.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.util.*;

/**
 * Repository אחראי על:
 * 1) כתיבה/קריאה להודעות (messages/{chatId}/chat)
 * 2) תחזוקת סיכומי צ'אט קלים לכל משתמש (userChats/{uid}/threads/{chatId})
 * 3) קבוצות (group_chats, group_messages) – כולל עדכון threads לכל חברי הקבוצה
 */
public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final Context context;

    public ChatRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // ===========================
    // ========== Threads =========
    // ===========================

    /** מחזיר שאילתה לפגינציה של threads לרשימת הצ'אטים של המשתמש */
    public Query getUserChatThreadsPage(String userId, int pageSize, @Nullable DocumentSnapshot after) {
        CollectionReference col = db.collection("userChats").document(userId).collection("threads");
        Query q = col.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING).limit(pageSize);
        if (after != null) q = q.startAfter(after);
        return q;
    }

    /** עדכון/הכנסה של מסמך thread ל-userChats/{uid}/threads/{chatId} */
    public Task<Void> upsertThread(String userId, String chatId, Map<String, Object> payload) {
        return db.collection("userChats").document(userId)
                .collection("threads").document(chatId)
                .set(payload, SetOptions.merge());
    }

    /** בניית payload למסמך thread מתוך הודעה */
    private Map<String, Object> buildThreadSummaryFromMessage(
            String chatId,
            String type, // "private" | "group"
            Message m,
            @Nullable String apartmentId,
            @Nullable String addressStreet,
            @Nullable String addressHouseNumber,
            @Nullable String addressCity,
            @Nullable List<String> participants // לרשימות ו־אינדוקס עתידי
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", chatId);
        map.put("type", type);
        if (apartmentId != null) map.put("apartmentId", apartmentId);
        if (addressStreet != null) map.put("addressStreet", addressStreet);
        if (addressHouseNumber != null) map.put("addressHouseNumber", addressHouseNumber);
        if (addressCity != null) map.put("addressCity", addressCity);

        map.put("lastMessage", m.getText());
        map.put("lastMessageSenderId", m.getFromUserId());
        map.put("lastMessageSenderName", m.getSenderName());
        map.put("lastMessageTimestamp", m.getTimestamp()); // millis
        map.put("timestamp", m.getTimestamp()); // לשמירה כפולה לשימוש ב־ViewModels קיימים

        if (participants != null) map.put("participants", participants);

        // שדה חישובי "hasUnread" נשמר פרטנית לכל משתמש (נגדיר בעת upsertThread עבור כל יעד)
        return map;
    }

    // ===========================
    // ======== Messages ==========
    // ===========================

    public Task<DocumentReference> sendMessage(String chatId, Message message) {
        if (chatId == null || message == null || message.getFromUserId() == null || message.getToUserId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid message parameters"));
        }
        long now = System.currentTimeMillis();
        message.setTimestamp(now);
        // הודעה "נקראה" רק אם שולח=נמען (נדיר), אחרת לא נקראה
        message.setRead(message.getFromUserId().equals(message.getToUserId()));

        // כתיבה להודעות
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .add(message)
                .onSuccessTask(docRef -> {
                    // עדכון threads: לשולח hasUnread=false, לנמען hasUnread=true
                    Map<String, Object> threadPayload = buildThreadSummaryFromMessage(
                            chatId,
                            "private",
                            message,
                            message.getApartmentId(),
                            message.getAddressStreet(),
                            message.getAddressHouseNumber(),
                            message.getAddressCity(),
                            Arrays.asList(message.getFromUserId(), message.getToUserId())
                    );

                    Map<String, Object> forSender = new HashMap<>(threadPayload);
                    forSender.put("hasUnread", false);

                    Map<String, Object> forReceiver = new HashMap<>(threadPayload);
                    forReceiver.put("hasUnread", true);

                    Task<Void> t1 = upsertThread(message.getFromUserId(), chatId, forSender);
                    Task<Void> t2 = upsertThread(message.getToUserId(), chatId, forReceiver);

                    // (אופציונלי) שליחת התראה
                    sendNotificationToRecipient(message);

                    return Tasks.whenAll(t1, t2).onSuccessTask(v -> Tasks.forResult(docRef));
                });
    }

    public Task<DocumentReference> sendMessageWithImage(String chatId, Message message, Uri imageUri) {
        if (chatId == null || message == null || message.getFromUserId() == null || message.getToUserId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid message parameters"));
        }
        long now = System.currentTimeMillis();
        message.setTimestamp(now);
        message.setRead(message.getFromUserId().equals(message.getToUserId()));

        if (imageUri != null) {
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child("message_images/" + filename);
            return ref.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .continueWithTask(task -> {
                        if (task.isSuccessful()) message.setImageUrl(task.getResult().toString());
                        return db.collection("messages").document(chatId).collection("chat").add(message);
                    })
                    .onSuccessTask(docRef -> {
                        Map<String, Object> threadPayload = buildThreadSummaryFromMessage(
                                chatId,
                                "private",
                                message,
                                message.getApartmentId(),
                                message.getAddressStreet(),
                                message.getAddressHouseNumber(),
                                message.getAddressCity(),
                                Arrays.asList(message.getFromUserId(), message.getToUserId())
                        );

                        Map<String, Object> forSender = new HashMap<>(threadPayload);
                        forSender.put("hasUnread", false);

                        Map<String, Object> forReceiver = new HashMap<>(threadPayload);
                        forReceiver.put("hasUnread", true);

                        Task<Void> t1 = upsertThread(message.getFromUserId(), chatId, forSender);
                        Task<Void> t2 = upsertThread(message.getToUserId(), chatId, forReceiver);

                        sendNotificationToRecipient(message);

                        return Tasks.whenAll(t1, t2).onSuccessTask(v -> Tasks.forResult(docRef));
                    });
        } else {
            return db.collection("messages")
                    .document(chatId)
                    .collection("chat")
                    .add(message)
                    .onSuccessTask(docRef -> {
                        Map<String, Object> threadPayload = buildThreadSummaryFromMessage(
                                chatId,
                                "private",
                                message,
                                message.getApartmentId(),
                                message.getAddressStreet(),
                                message.getAddressHouseNumber(),
                                message.getAddressCity(),
                                Arrays.asList(message.getFromUserId(), message.getToUserId())
                        );

                        Map<String, Object> forSender = new HashMap<>(threadPayload);
                        forSender.put("hasUnread", false);

                        Map<String, Object> forReceiver = new HashMap<>(threadPayload);
                        forReceiver.put("hasUnread", true);

                        Task<Void> t1 = upsertThread(message.getFromUserId(), chatId, forSender);
                        Task<Void> t2 = upsertThread(message.getToUserId(), chatId, forReceiver);

                        sendNotificationToRecipient(message);

                        return Tasks.whenAll(t1, t2).onSuccessTask(v -> Tasks.forResult(docRef));
                    });
        }
    }

    /** שאילתת הודעות לצ'אט מסוים (ל־Live query במסך הצ'אט אם רוצים) */
    public Query getChatMessagesQuery(String chatId) {
        if (chatId == null) throw new IllegalArgumentException("Chat ID cannot be null");
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    /** סימון הודעות פרטיות כנקראו (toUserId == userId && read=false) */
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
                    if (!task.isSuccessful()) throw task.getException();
                    List<Task<Void>> updates = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        updates.add(doc.getReference().update("read", true));
                    }
                    return Tasks.whenAll(updates);
                })
                .onSuccessTask(v -> {
                    // עדכון hasUnread=false ב-thread של המשתמש
                    Map<String, Object> patch = new HashMap<>();
                    patch.put("hasUnread", false);
                    return upsertThread(userId, chatId, patch);
                });
    }

    /** פגינציה להודעות בצ'אט (ASC) */
    public Query getPaginatedChatMessagesQuery(String chatId, int limit) {
        if (chatId == null) throw new IllegalArgumentException("Chat ID cannot be null");
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit);
    }

    /** שאילתה ל־"Load more" בהודעות (DESC, עם startAfter) */
    public Query buildQuery(String chatId) {
        return db.collection("messages")
                .document(chatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // ===========================
    // ========= Groups ===========
    // ===========================

    public Task<DocumentSnapshot> getGroupChatById(String groupChatId) {
        return db.collection("group_chats").document(groupChatId).get();
    }

    public Task<List<DocumentSnapshot>> getAllGroupChatsForUser(String userId) {
        Task<QuerySnapshot> byMember = db.collection("group_chats")
                .whereArrayContains("memberIds", userId).get();

        Task<QuerySnapshot> byOwner = db.collection("group_chats")
                .whereEqualTo("ownerId", userId).get();

        return Tasks.whenAllSuccess(byMember, byOwner)
                .continueWith(task -> {
                    Set<String> seen = new HashSet<>();
                    List<DocumentSnapshot> combined = new ArrayList<>();
                    for (Object result : task.getResult()) {
                        QuerySnapshot qs = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            if (seen.add(doc.getId())) combined.add(doc);
                        }
                    }
                    return combined;
                });
    }

    public Task<Void> markGroupMessagesAsRead(String groupChatId, String userId) {
        return db.collection("group_messages")
                .document(groupChatId)
                .collection("chat")
                .whereArrayContains("readBy", userId) // שים לב: זה יחזיר כבר נקראו; בד"כ תרצה whereNotIn / בדיקה ידנית
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<Task<Void>> updates = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        List<String> readBy = (List<String>) doc.get("readBy");
                        if (readBy == null || !readBy.contains(userId)) {
                            updates.add(doc.getReference().update("readBy", FieldValue.arrayUnion(userId)));
                        }
                    }
                    return Tasks.whenAll(updates);
                })
                .onSuccessTask(v -> {
                    // עדכון hasUnread=false ב-thread של המשתמש
                    Map<String, Object> patch = new HashMap<>();
                    patch.put("hasUnread", false);
                    return upsertThread(userId, groupChatId, patch);
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
                    if (!task.isSuccessful() || task.getResult().isEmpty()) return null;
                    return task.getResult().getDocuments().get(0);
                });
    }

    /** עדכון threads לכל חברי קבוצה אחרי שנשלחה הודעה בקבוצה */
    public Task<Void> upsertGroupThreadsAfterMessage(String groupId, Message message, List<String> memberIds,
                                                     @Nullable String apartmentId,
                                                     @Nullable String addressStreet,
                                                     @Nullable String addressHouseNumber,
                                                     @Nullable String addressCity) {

        Map<String, Object> payload = buildThreadSummaryFromMessage(
                groupId,
                "group",
                message,
                apartmentId, addressStreet, addressHouseNumber, addressCity,
                memberIds
        );

        List<Task<Void>> writes = new ArrayList<>();
        for (String uid : memberIds) {
            Map<String, Object> perUser = new HashMap<>(payload);
            boolean isSender = uid.equals(message.getFromUserId());
            perUser.put("hasUnread", !isSender); // לשולח false, לאחרים true
            writes.add(upsertThread(uid, groupId, perUser));
        }
        return Tasks.whenAll(writes);
    }

    // ===========================
    // ===== Notifications =======
    // ===========================

    private void sendNotificationToRecipient(Message message) {
        // ⚠️ אל תשמור מפתח שרת FCM באפליקציית לקוח. זה לצורכי דמו בלבד.
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
                                    headers.put("Authorization", "key=YOUR-SERVER-KEY"); // החלף בצד שרת בלבד
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

    // ===========================
    // ======= Legacy / Misc =====
    // ===========================

    /** (לא בשימוש כעת): כל ההודעות מכל הצ'אטים – כבד, השארתי לתאימות זמנית */
    public Task<QuerySnapshot> getAllChatMessages() {
        return db.collectionGroup("chat").orderBy("timestamp", Query.Direction.DESCENDING).get();
    }

    /** (Legacy) כל ההודעות למשתמש – לא בשימוש לרשימת צ'אטים אחרי המעבר ל-threads */
    public Task<QuerySnapshot> getChatsForUser(String userId) {
        if (userId == null) return Tasks.forException(new IllegalArgumentException("User ID cannot be null"));
        return db.collectionGroup("chat")
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }
}
