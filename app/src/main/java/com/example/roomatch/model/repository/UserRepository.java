package com.example.roomatch.model.repository;

import android.net.Uri;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserRepository {

    /* ---------- Firebase ---------- */
    private final FirebaseFirestore db      = FirebaseFirestore.getInstance();
    private final FirebaseAuth      auth    = FirebaseAuth.getInstance();
    private final FirebaseStorage   storage = FirebaseStorage.getInstance();

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
        return db.collection("users").document(uidOrThrow()).get();
    }
    // שמירה מגובה - עבור ViewModels שעדיין עובדים עם Map
    public Task<Void> saveMyProfile(Map<String, Object> profile) {
        return db.collection("users").document(uidOrThrow()).set(profile);
    }


    /* ---------- הודעות (inbox) ---------- */
    public Task<QuerySnapshot> getInboxMessages() {
        return db.collectionGroup("chat")
                .whereEqualTo("toUserId", uidOrThrow())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    /* ---------- “שותפים” ---------- */
    public Task<QuerySnapshot> getPartners() {
        return db.collection("users")
                .whereEqualTo("userType", "seeker")
                .whereEqualTo("seekerType", "partner")
                .get();
    }

    /* ---------- יוזרים אחרים ---------- */
    public Task<QuerySnapshot> getAllUsersExceptMe() {
        return db.collection("users")
                .whereNotEqualTo("uid", uidOrThrow())   // נתמך ב‑Firestore
                .get();
    }
    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection("users").document(userId).get();
    }

    /* ---------- (לא חובה) תמונת פרופיל ---------- */
    public Task<String> uploadProfileImage(Uri imageUri) {
        if (imageUri == null) return Tasks.forResult(null);
        String filename = "profile_" + uidOrThrow() + "_" + UUID.randomUUID();
        StorageReference ref = storage.getReference().child("profile_images/" + filename);
        return ref.putFile(imageUri)
                .continueWithTask(t -> ref.getDownloadUrl())
                .continueWith(t -> t.getResult().toString());
    }
}
