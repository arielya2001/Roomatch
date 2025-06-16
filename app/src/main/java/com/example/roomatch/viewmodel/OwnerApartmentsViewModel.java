package com.example.roomatch.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.view.activities.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OwnerApartmentsViewModel extends ViewModel {

    private ApartmentRepository repository;
    private MutableLiveData<List<Map<String, Object>>> allApartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Map<String, Object>>> filteredApartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> publishSuccess = new MutableLiveData<>();

    private boolean isTesting = false;

    public OwnerApartmentsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public void setTestRepository(ApartmentRepository testRepo) {
        this.repository = testRepo;
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
        if (ownerId == null) {
            Log.e("OwnerApartmentsViewModel", "ownerId is null");
            toastMessage.setValue("ownerId is null");
            return;
        }
        Log.d("OwnerApartmentsViewModel", "Loading apartments for ownerId: " + ownerId);
        repository.getApartmentsByOwnerId(ownerId).addOnSuccessListener(snapshot -> {
            Log.d("OwnerApartmentsViewModel", "Snapshot received: " + (snapshot != null ? snapshot.getDocuments().size() : 0) + " documents");
            List<Map<String, Object>> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        list.add(data);
                    }
                }
            }
            allApartments.setValue(list);
            filteredApartments.setValue(new ArrayList<>(list));
            Log.d("OwnerApartmentsViewModel", "Filtered apartments updated with size: " + list.size());
        }).addOnFailureListener(e -> {
            Log.e("OwnerApartmentsViewModel", "Error loading apartments: " + e.getMessage());
            toastMessage.setValue("שגיאה בטעינת הדירות: " + e.getMessage());
        });
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
            publishSuccess.setValue(false);
            return;
        }

        int houseNumber, price, roommatesNeeded;
        try {
            houseNumber = Integer.parseInt(houseNumStr);
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
            if (houseNumber < 0 || price < 0 || roommatesNeeded < 0) {
                toastMessage.setValue("שדות מספריים חייבים להיות חיוביים");
                publishSuccess.setValue(false);
                return;
            }
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false);
            return;
        }

        repository.publishApartment(getCurrentUserId(), city, street, houseNumber, price, roommatesNeeded, description, imageUri)
                .addOnSuccessListener(docRef -> {
                    toastMessage.setValue("הדירה פורסמה");
                    publishSuccess.setValue(true);
                    loadApartments(getCurrentUserId());
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בפרסום: " + e.getMessage());
                    publishSuccess.setValue(false);
                });
    }

    public void updateApartment(String apartmentId, String city, String street, String houseNumStr,
                                String priceStr, String roommatesStr, String description, Uri imageUri) {
        String validationError = validateInputs(city, street, houseNumStr, priceStr, roommatesStr, description);
        if (validationError != null) {
            toastMessage.setValue(validationError);
            publishSuccess.setValue(false);
            return;
        }

        int houseNumber, price, roommatesNeeded;
        try {
            houseNumber = Integer.parseInt(houseNumStr);
            price = Integer.parseInt(priceStr);
            roommatesNeeded = Integer.parseInt(roommatesStr);
            if (houseNumber < 0 || price < 0 || roommatesNeeded < 0) {
                toastMessage.setValue("שדות מספריים חייבים להיות חיוביים");
                publishSuccess.setValue(false);
                return;
            }
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false);
            return;
        }

        repository.updateApartment(apartmentId, getCurrentUserId(), city, street, houseNumber, price, roommatesNeeded, description, imageUri)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("דירה עודכנה בהצלחה");
                    publishSuccess.setValue(true);

                    if (MainActivity.isTestMode) {
                        // עדכון ידני ברשימת הדירות כשאנחנו במצב טסט
                        updateApartmentInList(apartmentId, city, street, houseNumber, price, roommatesNeeded, description);
                    } else {
                        // במצב רגיל טען מחדש מה־Firestore
                        loadApartments(getCurrentUserId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof IllegalArgumentException) {
                        toastMessage.setValue("שגיאה: הדירה לא נמצאה");
                    } else {
                        toastMessage.setValue("שגיאה בעדכון: " + e.getMessage());
                    }
                    publishSuccess.setValue(false);
                });

    }
    public void updateApartmentInList(String apartmentId, String city, String street, int houseNumber,
                                      int price, int roommatesNeeded, String description) {
        List<Map<String, Object>> currentList = filteredApartments.getValue();
        if (currentList == null) {
            Log.w("ViewModel", "⚠ filteredApartments.getValue() returned null");
            return;
        }

        Log.d("ViewModel", "🔍 Starting updateApartmentInList for ID: " + apartmentId);
        Log.d("ViewModel", "📋 Current apartments before update:");
        for (Map<String, Object> apt : currentList) {
            Log.d("ViewModel", " - ID: " + apt.get("id") + ", City: " + apt.get("city"));
        }

        List<Map<String, Object>> updatedList = new ArrayList<>();
        boolean updated = false;

        for (Map<String, Object> apt : currentList) {
            if (apartmentId.equals(apt.get("id"))) {
                Map<String, Object> updatedApt = new HashMap<>(apt);
                updatedApt.put("city", city);
                updatedApt.put("street", street);
                updatedApt.put("houseNumber", houseNumber);
                updatedApt.put("price", price);
                updatedApt.put("roommatesNeeded", roommatesNeeded);
                updatedApt.put("description", description);
                updatedList.add(updatedApt);
                updated = true;

                Log.d("ViewModel", "✅ Updated apartment: " + updatedApt);
            } else {
                updatedList.add(apt);
            }
        }

        filteredApartments.setValue(updatedList);

        Log.d("ViewModel", updated
                ? "🟢 Apartment updated and filteredApartments set."
                : "🔴 No matching apartment ID found – nothing updated.");

        Log.d("ViewModel", "📋 Updated apartments after update:");
        for (Map<String, Object> apt : updatedList) {
            Log.d("ViewModel", " - ID: " + apt.get("id") + ", City: " + apt.get("city"));
        }
    }



    public void deleteApartment(String apartmentId) {
        repository.deleteApartment(apartmentId)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("הדירה נמחקה");
                    loadApartments(getCurrentUserId());
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

    // ב־OwnerApartmentsFragment או ב־OwnerApartmentsViewModel
    public void setTestingConditions(ApartmentRepository testRepo) {
        this.repository = testRepo;
        this.isTesting = true;
        // איפוס הנתונים אם רלוונטי
        this.allApartments.setValue(new ArrayList<>());
        this.filteredApartments.setValue(new ArrayList<>());
    }

    // שיטה חדשה לעדכון filteredApartments מבחוץ
    public void setFilteredApartments(List<Map<String, Object>> apartments) {
        filteredApartments.setValue(apartments);
    }

    public void setDummyApartments(List<Map<String, Object>> dummyApartments) {
        MutableLiveData<List<Map<String, Object>>> apartmentsLiveData = new MutableLiveData<>(dummyApartments);
        this.allApartments = apartmentsLiveData; // משתנה שמקביל ל־getApartments או apartmentsList שלך
    }
    public void addTestApartment(Map<String, Object> dummy) {
        List<Map<String, Object>> currentFiltered = filteredApartments.getValue();
        if (currentFiltered == null) currentFiltered = new ArrayList<>();
        currentFiltered = new ArrayList<>(currentFiltered);
        currentFiltered.add(dummy);
        filteredApartments.setValue(currentFiltered);

        // 👇 הוספה גם לרשימה הראשית!
        List<Map<String, Object>> currentAll = allApartments.getValue();
        if (currentAll == null) currentAll = new ArrayList<>();
        currentAll = new ArrayList<>(currentAll);
        currentAll.add(dummy);
        allApartments.setValue(currentAll);
    }

    public void clearApartmentsForTest() {
        Log.d("ViewModel", "🧹 Clearing test apartments...");
        filteredApartments.setValue(new ArrayList<>());
    }





}