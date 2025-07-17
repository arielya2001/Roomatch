package com.example.roomatch.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OwnerApartmentsViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final List<Apartment> allApartments = new ArrayList<>(); //הדירות איך שהן נשלפות מהפיירבייס, רשימה פנימית
    private final MutableLiveData<List<Apartment>> filteredApartments = new MutableLiveData<>(new ArrayList<>()); // הרשימה שאני מציג בפועל!
    // משתנה בהתאם לפילטר.. חיפוש/סינון/איפוס
    //Mutable - לאחסן וגם לעדכן בזמן ריצה את כל המסכים - fragments
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(); // שמירת הודעות טקסט שאני מציג למשתמש
    //toastMessage.setValue("הדירה פורסמה")
    private final MutableLiveData<Boolean> publishSuccess = new MutableLiveData<>(); //הצליח או נכשל?
    //אם ההעלאה ל־Firebase הצליחה → publishSuccess.setValue(true)
    //אם נכשלה → publishSuccess.setValue(false)
    //צריך לדעת על הצלחה כדי לנקות טופס/לנווט חזרה וכו'

    private String selectedCity;
    private String selectedStreet;
    private LatLng selectedLocation;


    public OwnerApartmentsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public void setSelectedAddress(String city, String street, LatLng location) {
        this.selectedCity = city;
        this.selectedStreet = street;
        this.selectedLocation = location;
    }


    public LiveData<List<Apartment>> getFilteredApartments() { return filteredApartments; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getPublishSuccess() { return publishSuccess; }

    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }

    public void loadApartments(String ownerId) {
        Log.d("DEBUG", "loadApartments called with ownerId: " + ownerId);
        if (ownerId == null) {
            Log.e("OwnerApartmentsViewModel", "ownerId is null");
            toastMessage.setValue("ownerId is null");
            return;
        }
        repository.getApartmentsByOwnerId(ownerId).addOnSuccessListener(apartments -> {
            Log.d("DEBUG", "Loaded " + apartments.size() + " apartments from repo");
            allApartments.clear();
            allApartments.addAll(apartments);
            filteredApartments.setValue(new ArrayList<>(apartments));
        }).addOnFailureListener(e -> {
            Log.e("OwnerApartmentsViewModel", "Error loading apartments: " + e.getMessage());
            toastMessage.setValue("שגיאה בטעינת הדירות: " + e.getMessage());
        });
    }

    public void applyFilter(String field, boolean ascending) {
        List<Apartment> apartments = new ArrayList<>(allApartments);
        Comparator<Apartment> comparator = (a, b) -> {
            switch (field) {
                case "city":
                    return a.getCity() != null && b.getCity() != null ? a.getCity().compareTo(b.getCity()) : 0;
                case "street":
                    return a.getStreet() != null && b.getStreet() != null ? a.getStreet().compareTo(b.getStreet()) : 0;
                case "houseNumber":
                    return Integer.compare(a.getHouseNumber(), b.getHouseNumber());
                case "price":
                    return Integer.compare(a.getPrice(), b.getPrice());
                case "roommatesNeeded":
                    return Integer.compare(a.getRoommatesNeeded(), b.getRoommatesNeeded());
                default:
                    return 0;
            }
        };
        apartments.sort(ascending ? comparator : comparator.reversed());
        filteredApartments.setValue(apartments);
    }

    public void resetFilter() {
        filteredApartments.setValue(new ArrayList<>(allApartments));
    }

    public void searchApartments(String query) {
        if (query == null || query.trim().isEmpty()) {
            resetFilter();
            return;
        }
        List<Apartment> result = new ArrayList<>();
        String q = query.toLowerCase();
        for (Apartment apt : allApartments) {
            if ((apt.getCity() != null && apt.getCity().toLowerCase().contains(q)) ||
                    (apt.getStreet() != null && apt.getStreet().toLowerCase().contains(q)) ||
                    String.valueOf(apt.getHouseNumber()).contains(q) ||
                    String.valueOf(apt.getPrice()).contains(q) ||
                    String.valueOf(apt.getRoommatesNeeded()).contains(q) ||
                    (apt.getDescription() != null && apt.getDescription().toLowerCase().contains(q))) {
                result.add(apt);
            }
        }
        filteredApartments.setValue(result);
    }

    public void publishApartment(String houseNumStr, String priceStr, String roommatesStr,
                                 String description, Uri imageUri) {
        if (selectedCity == null || selectedStreet == null || selectedLocation == null) {
            toastMessage.setValue("יש לבחור כתובת אוטומטית לפני פרסום");
            publishSuccess.setValue(false);
            return;
        }

        String validationError = validateInputs(selectedCity, selectedStreet, houseNumStr, priceStr, roommatesStr, description);
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
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false);
            return;
        }

        Apartment apartment = new Apartment(
                null,
                getCurrentUserId(),
                selectedCity,
                selectedStreet,
                houseNumber,
                price,
                roommatesNeeded,
                description,
                null,
                selectedLocation.latitude,
                selectedLocation.longitude
        );

        repository.publishApartment(apartment, imageUri)
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
        } catch (NumberFormatException e) {
            toastMessage.setValue("מספרים לא תקינים בשדות כמות/מחיר/מספר בית");
            publishSuccess.setValue(false);
            return;
        }

        Apartment apartment = new Apartment(apartmentId, getCurrentUserId(), city, street, houseNumber, price, roommatesNeeded, description, null);
        repository.updateApartment(apartmentId, apartment, imageUri)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("דירה עודכנה בהצלחה");
                    publishSuccess.setValue(true);
                    loadApartments(getCurrentUserId());
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בעדכון: " + e.getMessage());
                    publishSuccess.setValue(false);
                });
    }

    public void deleteApartment(String apartmentId) {
        repository.deleteApartment(apartmentId)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("הדירה נמחקה");
                    loadApartments(getCurrentUserId());
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה: " + e.getMessage()));
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