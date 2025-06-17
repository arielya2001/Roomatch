package com.example.roomatch.model.repository;

import android.net.Uri;

import com.example.roomatch.model.Apartment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
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
}