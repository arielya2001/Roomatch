package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.Contact;
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




}