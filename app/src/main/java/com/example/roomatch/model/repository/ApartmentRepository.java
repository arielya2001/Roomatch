package com.example.roomatch.model.repository;

import android.net.Uri;
import android.util.Log;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.Contact;
import com.example.roomatch.model.GroupChat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.SharedGroup;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApartmentRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * מפרסם דירה חדשה (עם או בלי תמונה).
     */
    public Task<DocumentReference> publishApartment(Apartment apartment, Uri imageUri) {
        return uploadApartmentWithImageIfNeeded(apartment, imageUri);
    }




    /**
     * מעלה דירה עם תמונה אם יש.
     */
    private Task<DocumentReference> uploadApartmentWithImageIfNeeded(Apartment apartment, Uri imageUri) {
        if (imageUri != null) {
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child("images/" + filename);
            UploadTask uploadTask = ref.putFile(imageUri);
            return uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return ref.getDownloadUrl();
            }).continueWithTask(task -> {
                if (task.isSuccessful()) {
                    apartment.setImageUrl(task.getResult().toString());
                }
                return db.collection("apartments").add(apartment);
            });
        } else {
            return db.collection("apartments").add(apartment);
        }
    }

    /**
     * שולף את כל הדירות הקיימות.
     */
    public Task<List<Apartment>> getApartments() {
        return db.collection("apartments").get().continueWith(task -> {
            List<Apartment> apartments = new ArrayList<>();
            if (task.isSuccessful()) {
                for (DocumentSnapshot doc : task.getResult()) {
                    Apartment apt = doc.toObject(Apartment.class);
                    if (apt != null) {
                        apt.setId(doc.getId());
                        apartments.add(apt);
                    }
                }
            }
            return apartments;
        });
    }

    /**
     * שולף דירות לפי מזהה בעלים.
     */
    public Task<List<Apartment>> getApartmentsByOwnerId(String ownerId) {
        return db.collection("apartments").whereEqualTo("ownerId", ownerId).get()
                .continueWith(task -> {
                    List<Apartment> apartments = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Apartment apt = doc.toObject(Apartment.class);
                            if (apt != null) {
                                apt.setId(doc.getId());
                                apartments.add(apt);
                            }
                        }
                    }
                    return apartments;
                });
    }

    public Task<Apartment> getApartmentById(String apartmentId) {
        return FirebaseFirestore.getInstance()
                .collection("apartments")
                .document(apartmentId)
                .get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        return doc.toObject(Apartment.class);
                    } else {
                        return null;
                    }
                });
    }


    /**
     * מעדכן דירה קיימת עם פרטים חדשים ותמונה חדשה אם יש.
     */
    public Task<Void> updateApartment(String apartmentId, Apartment updatedApartment, Uri imageUri) {
        DocumentReference docRef = db.collection("apartments").document(apartmentId);
        return docRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                throw new IllegalArgumentException("Apartment not found with ID: " + apartmentId);
            }
            if (imageUri != null) {
                String filename = UUID.randomUUID().toString();
                StorageReference ref = storage.getReference().child("images/" + filename);
                return ref.putFile(imageUri)
                        .continueWithTask(uploadTask -> ref.getDownloadUrl())
                        .continueWithTask(downloadTask -> {
                            if (downloadTask.isSuccessful()) {
                                updatedApartment.setImageUrl(downloadTask.getResult().toString());
                            }
                            return docRef.set(updatedApartment);
                        });
            } else {
                return docRef.set(updatedApartment);
            }
        });
    }

    /**
     * מוחק דירה לפי מזהה.
     */
    public Task<Void> deleteApartment(String apartmentId) {
        return db.collection("apartments").document(apartmentId).delete();
    }

    /**
     * מחזיר את מזהה המשתמש המחובר הנוכחי.
     */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * שולף דירות ממוינות לפי שדה מסוים.
     */
    public Task<List<Apartment>> getApartmentsOrderedBy(String field, Query.Direction direction) {
        return db.collection("apartments")
                .orderBy(field, direction)
                .get()
                .continueWith(task -> {
                    List<Apartment> apartments = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Apartment apt = doc.toObject(Apartment.class);
                            if (apt != null) {
                                apt.setId(doc.getId());
                                apartments.add(apt);
                            }
                        }
                    }
                    return apartments;
                });
    }

    /**
     * שולף את פרטי הדירה לפי מזהה ספציפי.
     */
    public Task<Apartment> getApartmentDetails(String apartmentId) {
        return db.collection("apartments")
                .document(apartmentId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Apartment apt = task.getResult().toObject(Apartment.class);
                        if (apt != null) {
                            apt.setId(task.getResult().getId());
                        }
                        return apt;
                    }
                    return null;
                });
    }

    /**
     * שולף קבוצות משותפות עבור המשתמש הנוכחי.
     */
    public Task<List<SharedGroup>> getSharedGroupsForUser(String userId) {
        return db.collection("shared_groups")
                .whereArrayContains("memberIds", userId)
                .get()
                .continueWith(task -> {
                    List<SharedGroup> groups = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            SharedGroup group = doc.toObject(SharedGroup.class);
                            if (group != null) {
                                group.setId(doc.getId());
                                groups.add(group);
                            }
                        }
                    }
                    return groups;
                });
    }
    public Task<String> createGroupChatAndReturnId(String ownerId, String apartmentId, String groupId) {
        return getExistingGroupChatId(groupId, apartmentId).continueWithTask(existingTask -> {
            String existingId = existingTask.getResult();
            if (existingId != null) {
                Log.d("Repository", "🔁 צ'אט קבוצתי כבר קיים: " + existingId);
                return Tasks.forResult(existingId);
            }

            return getGroupMemberIds(groupId).continueWithTask(task -> {
                List<String> memberIds = task.getResult();
                if (memberIds == null) memberIds = new ArrayList<>();

                Map<String, Object> chatData = new HashMap<>();
                chatData.put("groupId", groupId);
                chatData.put("apartmentId", apartmentId);
                chatData.put("memberIds", memberIds);
                chatData.put("ownerId", ownerId);
                chatData.put("createdAt", System.currentTimeMillis());

                return db.collection("group_chats")
                        .add(chatData)
                        .continueWith(createdChatTask -> createdChatTask.getResult().getId());
            });
        });
    }



    /**
     * שולח הודעה בשם קבוצה לבעל הדירה.
     */
    public Task<Void> sendGroupMessage(String ownerId, String apartmentId, String groupId) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderGroupId", groupId);
        message.put("receiverId", ownerId);
        message.put("apartmentId", apartmentId);
        message.put("timestamp", System.currentTimeMillis());
        message.put("content", "הודעה משותפת מהקבוצה " + groupId); // ניתן לשנות לתוכן מותאם

        return db.collection("group_messages")
                .add(message)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return Tasks.forResult(null); // אישור הצלחה
                });
    }
    /**
     * שולף את כל בקשות ה-match הממתינות עבור המשתמש הנוכחי.
     */
    public Task<List<Contact>> getPendingMatchRequests(String userId) {
        return db.collection("match_requests")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .continueWith(task -> {
                    List<Contact> requests = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Contact request = doc.toObject(Contact.class);
                            if (request != null) {
                                request.setUserId(doc.getId()); // שימוש ב-setUserId במקום setId
                                requests.add(request);
                            }
                        }
                    }
                    return requests;
                });
    }

    /**
     * מאשר בקשת match ומעדכן את הסטטוס.
     */
    public Task<Void> approveMatch(String requestId) {
        DocumentReference docRef = db.collection("match_requests").document(requestId);
        return docRef.update("status", "accepted")
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return Tasks.forResult(null); // שימוש ב-Task.completedTask כחלופה (או Tasks.forResult(null))
                });
    }

    /**
     * דוחה בקשת match ומחק אותה.
     */
    public Task<Void> deleteMatchRequest(String requestId) {
        return db.collection("match_requests").document(requestId).delete();
    }

    public Task<Void> approveMatchAndUpdateContacts(String requestId, String currentUserId) {
        return getPendingMatchRequests(currentUserId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    for (Contact request : task.getResult()) {
                        if (request.getUserId().equals(requestId)) {
                            String otherUserId = request.getFromUserId().equals(currentUserId)
                                    ? request.getToUserId() : request.getFromUserId();
                            return approveMatch(requestId)
                                    .continueWithTask(approvalTask -> {
                                        if (!approvalTask.isSuccessful()) {
                                            throw approvalTask.getException();
                                        }
                                        return Tasks.whenAll(
                                                db.collection("users").document(currentUserId)
                                                        .update("contactIds", FieldValue.arrayUnion(otherUserId)),
                                                db.collection("users").document(otherUserId)
                                                        .update("contactIds", FieldValue.arrayUnion(currentUserId))
                                        );
                                    });
                        }
                    }
                    throw new IllegalArgumentException("בקשה לא נמצאה");
                });
    }

    public Task<UserProfile> getUserById(String userId) {
        return db.collection("users")
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().toObject(UserProfile.class);
                    }
                    return null;
                });
    }
    public Task<String> getUserNameById(String userId) {
        return db.collection("users")
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("fullName");
                    }
                    return "אנונימי";
                });
    }


    public Task<Void> createSharedGroup(List<String> memberIds) {
        String creatorId = getCurrentUserId();
        if (creatorId == null || memberIds == null || memberIds.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing userId or members"));
        }

        if (!memberIds.contains(creatorId)) {
            memberIds.add(creatorId);
        }

        List<Task<DocumentSnapshot>> nameTasks = new ArrayList<>();
        for (String id : memberIds) {
            nameTasks.add(db.collection("users").document(id).get());
        }

        return Tasks.whenAllSuccess(nameTasks).continueWithTask(task -> {
            List<String> names = new ArrayList<>();
            for (Object obj : task.getResult()) {
                DocumentSnapshot doc = (DocumentSnapshot) obj;
                String name = doc.getString("fullName");
                if (name != null) {
                    names.add(name);
                }
            }

            // בניית שם הקבוצה
            String groupName;
            if (names.size() == 1) {
                groupName = "קבוצה של " + names.get(0);
            } else if (names.size() == 2) {
                groupName = "קבוצה של " + names.get(0) + " ו" + names.get(1);
            } else {
                String last = names.remove(names.size() - 1);
                groupName = "קבוצה של " + String.join(", ", names) + " ו" + last;
            }

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("memberIds", new ArrayList<>(memberIds));
            groupData.put("creatorId", creatorId);
            groupData.put("createdAt", System.currentTimeMillis());
            groupData.put("name", groupName);

            //  הוספת מנהל
            Map<String, String> roles = new HashMap<>();
            roles.put(creatorId, "admin");
            groupData.put("roles", roles);


            return db.collection("shared_groups").add(groupData).continueWith(t -> null);
        });
    }


    public Task<DocumentSnapshot> getUserProfile(String userId) {
        return db.collection("users").document(userId).get();
    }

    public Task<String> sendGroupMessageAndCreateChat(String ownerId, String apartmentId, String groupId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || ownerId == null || apartmentId == null || groupId == null) {
            return Tasks.forException(new IllegalArgumentException("Missing required parameters"));
        }

        // בדיקה אם כבר קיים group_chat כזה
        return db.collection("group_chats")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("apartmentId", apartmentId)
                .get()
                .continueWithTask(existingChatTask -> {
                    if (!existingChatTask.isSuccessful()) {
                        throw existingChatTask.getException();
                    }

                    // ✅ אם כבר קיים – נחזיר את ה־ID
                    if (!existingChatTask.getResult().isEmpty()) {
                        String existingChatId = existingChatTask.getResult().getDocuments().get(0).getId();
                        return Tasks.forResult(existingChatId);
                    }

                    // ❌ לא קיים – ניצור חדש
                    return getGroupMemberIds(groupId).continueWithTask(task -> {
                        List<String> memberIds = task.getResult();
                        if (memberIds == null) memberIds = new ArrayList<>();

                        Map<String, Object> chatData = new HashMap<>();
                        chatData.put("groupId", groupId);
                        chatData.put("apartmentId", apartmentId);
                        chatData.put("memberIds", memberIds);
                        chatData.put("ownerId", ownerId);
                        chatData.put("createdAt", System.currentTimeMillis());

                        return db.collection("group_chats").add(chatData)
                                .continueWithTask(chatTask -> {
                                    if (!chatTask.isSuccessful()) {
                                        throw chatTask.getException();
                                    }
                                    String newChatId = chatTask.getResult().getId();
                                    return Tasks.forResult(newChatId);
                                });
                    });
                });
    }
    public Task<String> getExistingGroupChatId(String groupId, String apartmentId) {
        return db.collection("group_chats")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("apartmentId", apartmentId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    if (!task.getResult().isEmpty()) {
                        return task.getResult().getDocuments().get(0).getId();
                    } else {
                        return null;
                    }
                });
    }



    public Task<List<GroupChat>> getGroupChatsForUser(String userId) {
        Task<QuerySnapshot> asMemberTask = db.collection("group_chats")
                .whereArrayContains("memberIds", userId)
                .get();

        Task<QuerySnapshot> asOwnerTask = db.collection("group_chats")
                .whereEqualTo("ownerId", userId)
                .get();

        return Tasks.whenAllSuccess(asMemberTask, asOwnerTask).continueWith(task -> {
            List<GroupChat> chats = new ArrayList<>();

            QuerySnapshot memberChatsSnapshot = (QuerySnapshot) task.getResult().get(0);
            QuerySnapshot ownerChatsSnapshot = (QuerySnapshot) task.getResult().get(1);

            for (DocumentSnapshot doc : memberChatsSnapshot) {
                GroupChat chat = doc.toObject(GroupChat.class);
                if (chat != null) {
                    chat.setId(doc.getId());
                    chats.add(chat);
                }
            }

            for (DocumentSnapshot doc : ownerChatsSnapshot) {
                GroupChat chat = doc.toObject(GroupChat.class);
                if (chat != null && chats.stream().noneMatch(c -> c.getId().equals(doc.getId()))) {
                    chat.setId(doc.getId());
                    chats.add(chat);
                }
            }

            return chats;
        });
    }


    public Task<QuerySnapshot> getGroupChatMessagesForGroupAndApartment(String groupId, String apartmentId) {
        return db.collection("group_chats")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("apartmentId", apartmentId)
                .get();
    }

    /**
     * שולף את שם הקבוצה.
     */
    private Task<String> getGroupName(String groupId) {
        return db.collection("shared_groups").document(groupId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().getString("name");
                    }
                    return "קבוצה לא ידועה";
                });
    }

    private Task<List<String>> getGroupMemberIds(String groupId) {
        return db.collection("shared_groups").document(groupId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return (List<String>) task.getResult().get("memberIds");
                    }
                    return new ArrayList<>();
                });
    }

    /**
     * שולף הודעות של צ'אט קבוצתי.
     */
    public Task<List<Message>> getGroupChatMessages(String groupChatId) {
        return db.collection("group_messages")
                .document(groupChatId)
                .collection("chat")
                .orderBy("timestamp") // רק כדי שיהיו מסודרות בזמן
                .get()
                .continueWith(task -> {
                    List<Message> messages = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                message.setId(doc.getId());
                                messages.add(message);
                            }
                        }
                    }
                    return messages;
                });
    }
    private String getOwnerIdForGroupChatSync(String groupChatId) {
        try {
            DocumentSnapshot snapshot = Tasks.await(
                    db.collection("group_chats").document(groupChatId).get()
            );
            if (snapshot.exists()) {
                return snapshot.getString("ownerId");
            }
        } catch (Exception e) {
            Log.e("Repository", "שגיאה בקבלת ownerId", e);
        }
        return null;
    }



    /**
     * שולח הודעה בצ'אט קבוצתי.
     */
    public Task<Void> sendGroupChatMessage(String groupChatId, String userId, String text) {
        // נחזיר Task שמבצע את כל הפעולה
        Task<DocumentSnapshot> groupChatDocTask =
                db.collection("group_chats").document(groupChatId).get();

        return groupChatDocTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            DocumentSnapshot doc = task.getResult();
            String ownerId = doc.getString("ownerId");  // ✅ נשלף ממקום תקני

            if (ownerId == null) {
                throw new Exception("ownerId is missing in group_chat " + groupChatId);
            }

            Map<String, Object> message = new HashMap<>();
            message.put("fromUserId", userId);
            message.put("toUserId", ownerId);
            message.put("text", text);
            message.put("timestamp", System.currentTimeMillis());

            return db.collection("group_messages")
                    .document(groupChatId)
                    .collection("chat")
                    .add(message)
                    .continueWith(innerTask -> {
                        if (!innerTask.isSuccessful()) {
                            throw innerTask.getException();
                        }
                        return null;
                    });
        });
    }


    public Task<Void> reportApartment(String apartmentId, String ownerId, String reason, String details) {
        Map<String, Object> report = new HashMap<>();
        report.put("apartmentId", apartmentId);
        report.put("ownerId", ownerId);
        report.put("reason", reason);
        report.put("details", details);
        report.put("timestamp", System.currentTimeMillis());

        return db.collection("reports").add(report).continueWith(task -> null);
    }





}