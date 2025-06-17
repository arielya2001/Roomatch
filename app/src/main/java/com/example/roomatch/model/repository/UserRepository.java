package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    /* ---------- Firebase ---------- */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    /* ---------- כללי ---------- */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    private String uidOrThrow() {
        String uid = getCurrentUserId();
        if (uid == null) throw new IllegalStateException("No authenticated user");
        return uid;
    }

    /* ---------- פרופיל ---------- */
    public Task<DocumentSnapshot> getMyProfile() {
        String uid = getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("users").document(uid).get();
    }


    public Task<Void> saveMyProfile(String uid, UserProfile profile) {
        if (uid == null || profile == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid UID or profile"));
        }
        return db.collection("users").document(uid).set(profile);
    }



    /* ---------- הודעות (inbox) ---------- */
    public Task<QuerySnapshot> getInboxMessages() {
        String uid = getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("messages").whereEqualTo("receiverId", uid).get();
    }

    /* ---------- “שותפים” ---------- */
    public Task<QuerySnapshot> getPartners() {
        return db.collection("users").whereEqualTo("userType", "seeker").get();
    }

    /* ---------- יוזרים אחרים ---------- */
    public Task<QuerySnapshot> getAllUsersExceptMe() {
        String uid = getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("users").whereNotEqualTo("uid", uid).get();
    }

    public Task<UserProfile> getUserById(String userId) {
        if (userId == null) {
            return Tasks.forException(new IllegalArgumentException("User ID cannot be null"));
        }
        return db.collection("users").document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        return task.getResult().toObject(UserProfile.class);
                    }
                    return null;
                });
    }

    /* ---------- תמונת פרופיל ---------- */
    public Task<String> uploadProfileImage(Uri imageUri) {
        if (imageUri == null) {
            return Tasks.forResult(null);
        }
        String filename = "profile_" + uidOrThrow() + "_" + UUID.randomUUID();
        StorageReference ref = storage.getReference().child("profile_images/" + filename);
        return ref.putFile(imageUri)
                .continueWithTask(t -> ref.getDownloadUrl())
                .continueWith(t -> t.getResult().toString());
    }
}