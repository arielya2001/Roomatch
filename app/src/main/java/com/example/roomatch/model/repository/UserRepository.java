package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

public class UserRepository {

    private static final String TAG = "UserRepository"; // תגית לוג ייחודית
    /* ---------- Firebase ---------- */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private String currentUserName;

    public void loadCurrentUserName() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        currentUserName = profile.getFullName();
                    }
                });
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public Task<String> getUserNameById(String userId) {
        return getUserById(userId).continueWith(task -> {
            UserProfile profile = task.getResult();
            return (profile != null && profile.getFullName() != null) ? profile.getFullName() : "אנונימי";
        });
    }

    public interface RemoveFriendCallback {
        void onComplete(boolean success);
    }




    /* ---------- כללי ---------- */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        String uid = user != null ? user.getUid() : null;
        Log.d(TAG, "getCurrentUserId: User authenticated: " + (user != null) + ", UID: " + uid);
        return uid;
    }

    private String uidOrThrow() {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.e(TAG, "uidOrThrow: No authenticated user");
            throw new IllegalStateException("No authenticated user");
        }
        return uid;
    }

    /* ---------- פרופיל ---------- */
    public Task<DocumentSnapshot> getMyProfile() {
        String uid = getCurrentUserId();
        Log.d(TAG, "getMyProfile: Fetching profile for UID: " + uid);
        if (uid == null) {
            Log.e(TAG, "getMyProfile: User not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("users").document(uid).get();
    }

    public Task<Void> saveMyProfile(String uid, UserProfile profile) {
        if (uid == null || profile == null) {
            Log.e(TAG, "saveMyProfile: Invalid UID or profile, uid: " + uid + ", profile: " + profile);
            return Tasks.forException(new IllegalArgumentException("Invalid UID or profile"));
        }
        Log.d(TAG, "saveMyProfile: Saving profile for UID: " + uid);
        return db.collection("users").document(uid).set(profile);
    }

    /* ---------- הודעות (inbox) ---------- */
    public Task<QuerySnapshot> getInboxMessages() {
        String uid = getCurrentUserId();
        Log.d(TAG, "getInboxMessages: Fetching messages for UID: " + uid);
        if (uid == null) {
            Log.e(TAG, "getInboxMessages: User not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("messages").whereEqualTo("receiverId", uid).get();
    }

    /* ---------- “שותפים” ---------- */
    public Task<QuerySnapshot> getPartners() {
        Log.d(TAG, "getPartners: Fetching all seekers");
        return db.collection("users").whereEqualTo("userType", "seeker").get();
    }

    /* ---------- יוזרים אחרים ---------- */
    public Task<QuerySnapshot> getAllUsersExceptMe() {
        String uid = getCurrentUserId();
        Log.d(TAG, "getAllUsersExceptMe: Fetching users except UID: " + uid);
        if (uid == null) {
            Log.e(TAG, "getAllUsersExceptMe: User not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("users").whereNotEqualTo("uid", uid).get();
    }

    public Task<UserProfile> getUserById(String userId) {
        if (userId == null) {
            Log.e(TAG, "getUserById: User ID is null");
            return Tasks.forException(new IllegalArgumentException("User ID cannot be null"));
        }
        Log.d(TAG, "getUserById: Fetching user with ID: " + userId);
        return db.collection("users").document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().toObject(UserProfile.class);
                    }
                    Log.e(TAG, "getUserById: User not found or error: " + (task.getException() != null ? task.getException().getMessage() : "No data"));
                    return null;
                });
    }

    /* ---------- תמונת פרופיל ---------- */
    public Task<String> uploadProfileImage(Uri imageUri) {
        if (imageUri == null) {
            Log.d(TAG, "uploadProfileImage: No image URI provided");
            return Tasks.forResult(null);
        }
        String filename = "profile_" + uidOrThrow() + "_" + UUID.randomUUID();
        Log.d(TAG, "uploadProfileImage: Uploading image with filename: " + filename);
        StorageReference ref = storage.getReference().child("profile_images/" + filename);
        return ref.putFile(imageUri)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: Upload failed: " + t.getException(), t.getException());
                        throw t.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .continueWith(t -> {
                    if (t.isSuccessful()) {
                        return t.getResult().toString();
                    }
                    Log.e(TAG, "uploadProfileImage: Failed to get download URL: " + t.getException(), t.getException());
                    return null;
                });
    }

    /**
     * שולח בקשת match למשתמש אחר.
     */
    public Task<Void> sendMatchRequest(String fromUserId, String toUserId) {
        Log.d(TAG, "sendMatchRequest: Trying to send match from " + fromUserId + " to " + toUserId);

        // שלב 1: לבדוק אם כבר קיימת בקשה או קשר מאושר
        return db.collection("match_requests")
                .whereIn("status", Arrays.asList("pending", "accepted"))
                .whereIn("fromUserId", Arrays.asList(fromUserId, toUserId))
                .whereIn("toUserId", Arrays.asList(fromUserId, toUserId))
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        Log.e(TAG, "sendMatchRequest: Failed to check existing requests", e);
                        return Tasks.forException(e);
                    }

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String from = doc.getString("fromUserId");
                        String to = doc.getString("toUserId");

                        // בדיקה אם קיימת כבר בקשה בשני הכיוונים
                        if ((from.equals(fromUserId) && to.equals(toUserId)) ||
                                (from.equals(toUserId) && to.equals(fromUserId))) {
                            Log.w(TAG, "sendMatchRequest: Match request already exists between users");
                            return Tasks.forException(new IllegalStateException("כבר קיימת בקשה או קשר"));
                        }
                    }

                    // שלב 2: לשלוח בקשה חדשה
                    Map<String, Object> matchRequest = new HashMap<>();
                    matchRequest.put("fromUserId", fromUserId);
                    matchRequest.put("toUserId", toUserId);
                    matchRequest.put("status", "pending");
                    matchRequest.put("timestamp", System.currentTimeMillis());

                    return db.collection("match_requests")
                            .add(matchRequest)
                            .continueWith(task2 -> {
                                if (!task2.isSuccessful()) {
                                    throw task2.getException();
                                }
                                return null;
                            });
                });
    }
    public void removeFriend(UserProfile profile, RemoveFriendCallback callback) {
        String currentUserId = getCurrentUserId();
        String otherUserId = profile.getUserId();

        if (currentUserId == null || otherUserId == null) {
            callback.onComplete(false);
            return;
        }

        db.collection("match_requests")
                .whereEqualTo("status", "accepted")
                .whereIn("fromUserId", List.of(currentUserId, otherUserId))
                .whereIn("toUserId", List.of(currentUserId, otherUserId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();

                    for (var doc : snapshot.getDocuments()) {
                        String from = doc.getString("fromUserId");
                        String to = doc.getString("toUserId");

                        // וידוא שהמסמך הוא בין שני המשתמשים
                        if (
                                (from.equals(currentUserId) && to.equals(otherUserId)) ||
                                        (from.equals(otherUserId) && to.equals(currentUserId))
                        ) {
                            batch.delete(doc.getReference());
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error deleting friend: " + e.getMessage());
                                callback.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error finding match request: " + e.getMessage());
                    callback.onComplete(false);
                });
    }




    public Task<Boolean> canSendMatchRequest(String fromUserId, String toUserId) {
        return db.collection("match_requests")
                .whereIn("status", List.of("pending", "accepted"))
                .whereIn("fromUserId", List.of(fromUserId, toUserId))
                .whereIn("toUserId", List.of(fromUserId, toUserId))
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "canSendMatchRequest: Error checking existing requests", task.getException());
                        return false;
                    }

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String from = doc.getString("fromUserId");
                        String to = doc.getString("toUserId");

                        // בדיקה אם הבקשה קיימת לשני הכיוונים
                        if ((from.equals(fromUserId) && to.equals(toUserId)) ||
                                (from.equals(toUserId) && to.equals(fromUserId))) {
                            Log.d(TAG, "canSendMatchRequest: Match request already exists between users.");
                            return false; // אי אפשר לשלוח
                        }
                    }

                    return true; // מותר לשלוח
                });
    }


    public void loadFriends(FirestoreCallback<List<UserProfile>> callback) {
        String myUid = getCurrentUserId();
        if (myUid == null) {
            Log.e(TAG, "loadFriends: User not logged in");
            callback.onCallback(new ArrayList<>());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("match_requests")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> friendIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String from = doc.getString("fromUserId");
                        String to = doc.getString("toUserId");

                        if (from == null || to == null) continue;

                        if (from.equals(myUid)) {
                            friendIds.add(to);
                        } else if (to.equals(myUid)) {
                            friendIds.add(from);
                        }
                    }

                    if (friendIds.isEmpty()) {
                        callback.onCallback(new ArrayList<>());
                        return;
                    }

                    List<Task<DocumentSnapshot>> profileTasks = new ArrayList<>();
                    for (String id : friendIds) {
                        profileTasks.add(db.collection("users").document(id).get());
                    }

                    Tasks.whenAllSuccess(profileTasks)
                            .addOnSuccessListener(results -> {
                                List<UserProfile> friends = new ArrayList<>();
                                for (Object result : results) {
                                    if (result instanceof DocumentSnapshot doc && doc.exists()) {
                                        UserProfile profile = doc.toObject(UserProfile.class);
                                        if (profile != null) {
                                            profile.setUserId(doc.getId()); // לא לשכוח!
                                            friends.add(profile);
                                        }
                                    }
                                }
                                callback.onCallback(friends);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "loadFriends: Failed loading user profiles", e);
                                callback.onCallback(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadFriends: Failed loading match requests", e);
                    callback.onCallback(new ArrayList<>());
                });
    }



    public interface FirestoreCallback<T> {
        void onCallback(T data);
    }


}