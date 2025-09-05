<<<<<<< Updated upstream
package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
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
        Log.d(TAG, "sendMatchRequest: Initiating request from " + fromUserId + " to " + toUserId);
        if (fromUserId == null || toUserId == null) {
            Log.e(TAG, "sendMatchRequest: Invalid user IDs - fromUserId: " + fromUserId + ", toUserId: " + toUserId);
            return Tasks.forException(new IllegalArgumentException("User IDs cannot be null"));
        }

        Map<String, Object> matchRequest = new HashMap<>();
        matchRequest.put("fromUserId", fromUserId);
        matchRequest.put("toUserId", toUserId);
        matchRequest.put("status", "pending");
        matchRequest.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "sendMatchRequest: Adding match request to Firestore");
        return db.collection("match_requests")
                .add(matchRequest)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "sendMatchRequest: Failed to add match request: " + task.getException(), task.getException());
                        throw task.getException();
                    }
                    Log.d(TAG, "sendMatchRequest: Match request added successfully");
                    return Tasks.forResult(null); // אישור הצלחה
                });
    }
=======
package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
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
    public interface FirestoreCallback<T> {
        void onCallback(T data);
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
        Log.d(TAG, "sendMatchRequest: Initiating request from " + fromUserId + " to " + toUserId);
        if (fromUserId == null || toUserId == null) {
            Log.e(TAG, "sendMatchRequest: Invalid user IDs - fromUserId: " + fromUserId + ", toUserId: " + toUserId);
            return Tasks.forException(new IllegalArgumentException("User IDs cannot be null"));
        }

        Map<String, Object> matchRequest = new HashMap<>();
        matchRequest.put("fromUserId", fromUserId);
        matchRequest.put("toUserId", toUserId);
        matchRequest.put("status", "pending");
        matchRequest.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "sendMatchRequest: Adding match request to Firestore");
        return db.collection("match_requests")
                .add(matchRequest)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "sendMatchRequest: Failed to add match request: " + task.getException(), task.getException());
                        throw task.getException();
                    }
                    Log.d(TAG, "sendMatchRequest: Match request added successfully");
                    return Tasks.forResult(null); // אישור הצלחה
                });
    }
>>>>>>> Stashed changes
}