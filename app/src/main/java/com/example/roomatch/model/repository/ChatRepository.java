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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

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
        message.setRead(message.getFromUserId().equals(message.getToUserId()));

        Task<String> ensureNameTask = (message.getSenderName() == null || message.getSenderName().trim().isEmpty())
                ? resolveUserName(message.getFromUserId())
                : Tasks.forResult(message.getSenderName());

        return ensureNameTask
                .continueWithTask(nameTask -> {
                    message.setSenderName(nameTask.getResult()); // שם סופי
                    return db.collection("messages")
                            .document(chatId)
                            .collection("chat")
                            .add(message);
                })
                .onSuccessTask(docRef -> {
                    // בונים summary ל-threads
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

    public Task<Void> sendGroupChatMessage(String groupChatId, String fromUserId, String text) {
        if (groupChatId == null || fromUserId == null || text == null || text.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing parameters"));
        }

        long now = System.currentTimeMillis();
        DocumentReference groupChatRef = db.collection("group_chats").document(groupChatId);
        CollectionReference msgsCol = db.collection("group_messages").document(groupChatId).collection("chat");
        DocumentReference newMsgRef = msgsCol.document();

        Task<DocumentSnapshot> chatTask = groupChatRef.get();
        Task<String> senderNameTask = resolveUserName(fromUserId);

        return Tasks.whenAllSuccess(chatTask, senderNameTask).continueWithTask(t -> {
            DocumentSnapshot chatDoc = (DocumentSnapshot) t.getResult().get(0);
            String senderName = (String) t.getResult().get(1);

            if (!chatDoc.exists())
                throw new IllegalStateException("group_chat not found: " + groupChatId);

            String apartmentId = chatDoc.getString("apartmentId");
            String sharedGroupId = chatDoc.getString("groupId");
            String ownerId = chatDoc.getString("ownerId");

            // עטיפות לכתובת כדי שיהיו final
            AtomicReference<String> addressStreetRef = new AtomicReference<>(chatDoc.getString("addressStreet"));
            AtomicReference<String> addressHouseNumberRef = new AtomicReference<>(chatDoc.getString("addressHouseNumber"));
            AtomicReference<String> addressCityRef = new AtomicReference<>(chatDoc.getString("addressCity"));

            @SuppressWarnings("unchecked")
            List<String> memberIds = (List<String>) chatDoc.get("memberIds");
            if (memberIds == null) memberIds = new ArrayList<>();
            if (ownerId != null && !memberIds.contains(ownerId)) memberIds.add(ownerId);

            boolean needAptLookup =
                    (addressStreetRef.get() == null || addressStreetRef.get().trim().isEmpty()) ||
                            (addressCityRef.get() == null || addressCityRef.get().trim().isEmpty()) ||
                            (addressHouseNumberRef.get() == null || addressHouseNumberRef.get().trim().isEmpty());

            List<String> finalMemberIds = memberIds;
            Callable<Task<Void>> commitAction = () -> {
                WriteBatch batch = db.batch();

                // הודעה חדשה
                Map<String, Object> msg = new HashMap<>();
                msg.put("fromUserId", fromUserId);
                msg.put("text", text);
                msg.put("timestamp", now);
                msg.put("senderName", senderName);
                batch.set(newMsgRef, msg);

                // עדכון מידע כללי בצ׳אט
                Map<String, Object> chatSummary = new HashMap<>();
                chatSummary.put("lastMessage", text);
                chatSummary.put("lastMessageSenderName", senderName);
                chatSummary.put("lastMessageTimestamp", now);
                batch.set(groupChatRef, chatSummary, SetOptions.merge());

                // שמירת כתובת בקבוצה אם נוספה
                Map<String, Object> addrPatch = new HashMap<>();
                if (addressStreetRef.get() != null)      addrPatch.put("addressStreet", addressStreetRef.get());
                if (addressHouseNumberRef.get() != null) addrPatch.put("addressHouseNumber", addressHouseNumberRef.get());
                if (addressCityRef.get() != null)        addrPatch.put("addressCity", addressCityRef.get());
                if (!addrPatch.isEmpty()) {
                    batch.set(groupChatRef, addrPatch, SetOptions.merge());
                }

                // threads לכל חבר
                for (String uid : finalMemberIds) {
                    DocumentReference threadRef = db.collection("userChats")
                            .document(uid)
                            .collection("threads")
                            .document(groupChatId);

                    Map<String, Object> thread = new HashMap<>();
                    thread.put("type", "group");
                    thread.put("groupId", sharedGroupId);
                    thread.put("apartmentId", apartmentId);
                    thread.put("addressStreet", addressStreetRef.get());
                    thread.put("addressHouseNumber", addressHouseNumberRef.get());
                    thread.put("addressCity", addressCityRef.get());
                    thread.put("lastMessage", text);
                    thread.put("lastMessageSenderName", senderName);
                    thread.put("lastMessageTimestamp", now);
                    thread.put("timestamp", now);
                    thread.put("hasUnread", !uid.equals(fromUserId));

                    batch.set(threadRef, thread, SetOptions.merge());
                }

                return batch.commit();
            };

            if (needAptLookup && apartmentId != null) {
                return db.collection("apartments").document(apartmentId).get()
                        .continueWithTask(aptTask -> {
                            if (aptTask.isSuccessful() && aptTask.getResult().exists()) {
                                DocumentSnapshot apt = aptTask.getResult();
                                if (addressStreetRef.get() == null || addressStreetRef.get().trim().isEmpty())
                                    addressStreetRef.set(apt.getString("street"));
                                if (addressHouseNumberRef.get() == null || addressHouseNumberRef.get().trim().isEmpty()) {
                                    Long hn = apt.getLong("houseNumber");
                                    addressHouseNumberRef.set(hn != null ? String.valueOf(hn) : null);
                                }
                                if (addressCityRef.get() == null || addressCityRef.get().trim().isEmpty())
                                    addressCityRef.set(apt.getString("city"));
                            }
                            return commitAction.call();
                        });
            } else {
                return commitAction.call();
            }
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

    private Task<String> resolveUserName(String userId) {
        if (userId == null) return Tasks.forResult("אנונימי");
        return db.collection("users").document(userId).get()
                .continueWith(t -> {
                    if (!t.isSuccessful() || !t.getResult().exists()) return userId; // fallback
                    String n = t.getResult().getString("fullName");
                    return (n == null || n.trim().isEmpty()) ? userId : n;
                });
    }

    public Query buildGroupQuery(String groupChatId) {
        return db.collection("group_messages").document(groupChatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Query getPaginatedGroupChatMessagesQuery(String groupChatId, int limit) {
        return db.collection("group_messages").document(groupChatId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit);
    }


}
