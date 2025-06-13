package com.example.roomatch.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OwnerApartmentsViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final MutableLiveData<List<Map<String, Object>>> allApartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Map<String, Object>>> filteredApartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> publishSuccess = new MutableLiveData<>();

    public OwnerApartmentsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    // LiveData Getters
    public LiveData<List<Map<String, Object>>> getFilteredApartments() { return filteredApartments; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getPublishSuccess() { return publishSuccess; }

    // Public method to set publishSuccess for testing
    public void setPublishSuccess(Boolean value) {
        publishSuccess.setValue(value);
    }

    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }

    public void loadApartments(String ownerId) {
        repository.getApartmentsByOwnerId(ownerId).addOnSuccessListener(snapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            allApartments.setValue(list);
            filteredApartments.setValue(new ArrayList<>(list));
        }).addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת הדירות"));
    }

    public void applyFilter(String field, boolean ascending) {
        List<Map<String, Object>> apartments = new ArrayList<>(Objects.requireNonNull(allApartments.getValue()));
        apartments.sort((a, b) -> {
            Comparable valA = (Comparable) a.get(field);
            Comparable valB = (Comparable) b.get(field);
            if (valA == null || valB == null) return 0;
            return ascending ? valA.compareTo(valB) : valB.compareTo(valA);
        });
        filteredApartments.setValue(apartments);
    }

    public void resetFilter() {
        filteredApartments.setValue(new ArrayList<>(Objects.requireNonNull(allApartments.getValue())));
    }

    public void searchApartments(String query) {
        List<Map<String, Object>> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (Map<String, Object> apt : Objects.requireNonNull(allApartments.getValue())) {
            if (apt.values().stream().anyMatch(val -> val != null && val.toString().toLowerCase().contains(q))) {
                result.add(apt);
            }
        }
        filteredApartments.setValue(result);
    }

    public void publishApartment(String city, String street, String houseNumStr,
                                 String priceStr, String roommatesStr, String description, Uri imageUri) {
        String validationError = validateInputs(city, street, houseNumStr, priceStr, roommatesStr, description);
        if (validationError != null) {
            toastMessage.setValue(validationError);
            publishSuccess.setValue(false); // עדכון מיידי במקרה של אימות כשל
            return;
        }

        int houseNumber, price, roommatesNeeded;
        try {
            houseNumber = Integer.parseInt(houseNumStr);
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
            if (houseNumber < 0 || price < 0 || roommatesNeeded < 0) {
                toastMessage.setValue("שדות מספריים חייבים להיות חיוביים");
                publishSuccess.setValue(false); // עדכון מיידי במקרה של מספר שלילי
                return;
            }
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false); // עדכון מיידי במקרה של חריגה
            return;
        }

        repository.publishApartment(getCurrentUserId(), city, street, houseNumber, price, roommatesNeeded, description, imageUri)
                .addOnSuccessListener(docRef -> {
                    toastMessage.setValue("הדירה פורסמה");
                    publishSuccess.setValue(true);
                    loadApartments(getCurrentUserId()); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בפרסום: " + e.getMessage());
                    publishSuccess.setValue(false); // עדכון במקרה של כשל אסינכרוני
                });
    }

    public void updateApartment(String apartmentId, String city, String street, String houseNumStr,
                                String priceStr, String roommatesStr, String description, Uri imageUri) {
        String validationError = validateInputs(city, street, houseNumStr, priceStr, roommatesStr, description);
        if (validationError != null) {
            toastMessage.setValue(validationError);
            publishSuccess.setValue(false); // עדכון מיידי במקרה של אימות כשל
            return;
        }

        int houseNumber, price, roommatesNeeded;
        try {
            houseNumber = Integer.parseInt(houseNumStr);
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
            if (houseNumber < 0 || price < 0 || roommatesNeeded < 0) {
                toastMessage.setValue("שדות מספריים חייבים להיות חיוביים");
                publishSuccess.setValue(false); // עדכון מיידי במקרה של מספר שלילי
                return;
            }
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false); // עדכון מיידי במקרה של חריגה
            return;
        }

        repository.updateApartment(apartmentId, getCurrentUserId(), city, street, houseNumber, price, roommatesNeeded, description, imageUri)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("דירה עודכנה בהצלחה");
                    publishSuccess.setValue(true);
                    loadApartments(getCurrentUserId()); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    if (e instanceof IllegalArgumentException) {
                        toastMessage.setValue("שגיאה: הדירה לא נמצאה");
                    } else {
                        toastMessage.setValue("שגיאה בעדכון: " + e.getMessage());
                    }
                    publishSuccess.setValue(false); // עדכון במקרה של כשל אסינכרוני
                });
    }

    public void deleteApartment(String apartmentId) {
        repository.deleteApartment(apartmentId)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("הדירה נמחקה");
                    loadApartments(getCurrentUserId()); // Refresh the list
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה: " + e.getMessage()));
    }

    public Task<QuerySnapshot> getApartments() {
        return repository.getApartments();
    }

    private String validateInputs(String city, String street, String houseNumStr, String priceStr,
                                  String roommatesStr, String description) {
        if (city == null || city.isEmpty() || street == null || street.isEmpty() ||
                houseNumStr == null || houseNumStr.isEmpty() || priceStr == null || priceStr.isEmpty() ||
                roommatesStr == null || roommatesStr.isEmpty() || description == null || description.isEmpty()) {
            return "כל השדות חייבים להיות מלאים";
        }

        try {
            int houseNumber = Integer.parseInt(houseNumStr);
            int price = Integer.parseInt(priceStr);
            int roommates = Integer.parseInt(roommatesStr);

            if (houseNumber < 0 || price < 0 || roommates < 0) {
                return "שדות מספריים חייבים להיות חיוביים";
            }
        } catch (NumberFormatException e) {
            return "מספרים לא תקינים בשדות כמות/מחיר/מספר בית";
        }

        return null;
    }
}