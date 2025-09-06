package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdvancedSearchViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final MutableLiveData<List<Apartment>> nearbyApartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private double targetLat = 0;
    private double targetLng = 0;

    public AdvancedSearchViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Apartment>> getNearbyApartments() {
        return nearbyApartments;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void setTargetLocation(double lat, double lng) {
        this.targetLat = lat;
        this.targetLng = lng;
        fetchAndRankApartments();
    }

    public void fetchAndRankApartments() {
        Apartment.setSearchLocation(targetLat, targetLng); // הגדרת מיקום חיפוש עבור כל הדירות

        repository.getApartments()
                .addOnSuccessListener(apartments -> {
                    for (Apartment apt : apartments) {
                        if (apt.getLatitude() != 0 && apt.getLongitude() != 0) {
                            apt.calculateDistanceFromSearchLocation(); // מחשב מרחק לפי המיקום שהוגדר
                        } else {
                            apt.setDistance(Double.MAX_VALUE); // דירות בלי מיקום נשלחות לסוף הרשימה
                        }

                        // Log לעזרה בבדיקה
                        android.util.Log.d("DistanceDebug", "דירה ב-" + apt.getCity() + " מרחק: " + apt.getDistance());
                    }

                    // מיון לפי מרחק
                    Collections.sort(apartments, Comparator.comparingDouble(Apartment::getDistance));

                    // עדכון הרשימה במסך
                    nearbyApartments.setValue(apartments);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בטעינת דירות: " + e.getMessage());
                });
    }

    public void fetchAndSortApartmentsByDistance(LatLng latLng) {
        setTargetLocation(latLng.latitude, latLng.longitude);
    }


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // רדיוס כדוה"א בק"מ
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void reportApartment(Apartment apartment, String reason, String details) {
        repository.reportApartment(apartment.getId(), apartment.getOwnerId(), reason, details)
                .addOnSuccessListener(unused -> toastMessage.setValue("הדיווח נשלח בהצלחה"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחת הדיווח"));
    }
}
