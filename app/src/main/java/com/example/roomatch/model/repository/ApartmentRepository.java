package com.example.roomatch.model.repository;

import android.net.Uri;

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
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApartmentRepository {

    private FirebaseFirestore db = null; // לא מאתחל בטעינה ראשונית
    private FirebaseStorage storage = null; // לא מאתחל בטעינה ראשונית
    private FirebaseAuth auth = null; // לא מאתחל בטעינה ראשונית
    private boolean isTestingMode = false;

    public ApartmentRepository() {
        this(false); // delegate לקונסטרקטור הראשי
    }


    // TEST MODE
    public ApartmentRepository(boolean isTestingMode) {
        this.isTestingMode = isTestingMode;
        if (!isTestingMode) {
            this.db = FirebaseFirestore.getInstance();
            this.storage = FirebaseStorage.getInstance();
            this.auth = FirebaseAuth.getInstance();
        }
    }

    /**
     * יוצר מפת דירה עם פרטים מלאים.
     */
    public Map<String, Object> createApartmentMap(String ownerId, String city, String street,
                                                  int houseNumber, int price, int roommatesNeeded,
                                                  String description, String imageUrl) {
        Map<String, Object> apt = new HashMap<>();
        apt.put("ownerId", ownerId);
        apt.put("city", city);
        apt.put("street", street);
        apt.put("houseNumber", houseNumber);
        apt.put("price", price);
        apt.put("roommatesNeeded", roommatesNeeded);
        apt.put("description", description);
        apt.put("imageUrl", imageUrl);
        return apt;
    }

    /**
     * מפרסם דירה חדשה (עם או בלי תמונה).
     */
    public Task<DocumentReference> publishApartment(String ownerId, String city, String street,
                                                    int houseNumber, int price, int roommatesNeeded,
                                                    String description, Uri imageUri) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        Map<String, Object> apt = createApartmentMap(ownerId, city, street, houseNumber, price,
                roommatesNeeded, description, "");
        return uploadApartmentWithImageIfNeeded(apt, imageUri);
    }

    /**
     * מעלה דירה עם תמונה אם יש.
     */
    public Task<DocumentReference> uploadApartmentWithImageIfNeeded(Map<String, Object> apt, Uri imageUri) {
        if (isTestingMode || storage == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם storage לא מאותחל
        }
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
                    apt.put("imageUrl", task.getResult().toString());
                }
                return db.collection("apartments").add(apt);
            });
        } else {
            return db.collection("apartments").add(apt);
        }
    }

    /**
     * שולף את כל הדירות הקיימות.
     */
    public Task<QuerySnapshot> getApartments() {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        return db.collection("apartments").get();
    }

    /**
     * שולף דירות לפי מזהה בעלים.
     */
    public Task<QuerySnapshot> getApartmentsByOwnerId(String ownerId) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        return db.collection("apartments").whereEqualTo("ownerId", ownerId).get();
    }

    /**
     * מעדכן דירה קיימת עם פרטי טקסט ותמונה חדשה אם יש.
     */
    public Task<Void> updateApartment(String apartmentId, String ownerId, String city, String street,
                                      int houseNumber, int price, int roommatesNeeded,
                                      String description, Uri imageUri) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        Map<String, Object> updatedApt = createApartmentMap(ownerId, city, street, houseNumber,
                price, roommatesNeeded, description, "");
        return updateApartment(apartmentId, updatedApt, imageUri);
    }

    /**
     * מעדכן דירה עם מפה מוכנה מראש.
     */
    public Task<Void> updateApartment(String apartmentId, Map<String, Object> updatedApt, Uri imageUri) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
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
                                updatedApt.put("imageUrl", downloadTask.getResult().toString());
                            }
                            return docRef.update(updatedApt);
                        });
            } else {
                return docRef.update(updatedApt);
            }
        });
    }

    /**
     * מוחק דירה לפי מזהה.
     */
    public Task<Void> deleteApartment(String apartmentId) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        return db.collection("apartments").document(apartmentId).delete();
    }

    /**
     * מחזיר את מזהה המשתמש המחובר הנוכחי.
     */
    public String getCurrentUserId() {
        if (isTestingMode || auth == null) {
            return "test-user-id"; // מחזיר מזהה מזויף אם במצב בדיקה או אם auth לא מאותחל
        }
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * שולף דירות ממוינות לפי שדה מסוים.
     */
    public Task<QuerySnapshot> getApartmentsOrderedBy(String field, Query.Direction direction) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        return db.collection("apartments")
                .orderBy(field, direction)
                .get();
    }

    /**
     * שולף את פרטי הדירה לפי מזהה ספציפי.
     */
    public Task<DocumentSnapshot> getApartmentDetails(String apartmentId) {
        if (isTestingMode || db == null) {
            return Tasks.forResult(null); // תגובה מזויפת אם במצב בדיקה או אם db לא מאותחל
        }
        return db.collection("apartments")
                .document(apartmentId)
                .get();
    }
}