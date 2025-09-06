package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ApartmentSearchViewModel extends ViewModel {
    private final ApartmentRepository repository;
    private final MutableLiveData<List<Apartment>> apartments = new MutableLiveData<>(new ArrayList<>());
    private final List<Apartment> allApartments = new ArrayList<>();

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public ApartmentSearchViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Apartment>> getApartments() {
        return apartments;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadApartments() {
        repository.getApartments()
                .addOnSuccessListener(list -> {
                    allApartments.clear();
                    allApartments.addAll(list);
                    apartments.setValue(new ArrayList<>(list));
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת הדירות"));
    }


    public void applyFilter(String field, boolean ascending) {
        repository.getApartmentsOrderedBy(field, ascending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING)
                .addOnSuccessListener(list -> apartments.setValue(list))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בסינון"));
    }

    public void searchApartments(String query) {
        if (query == null || query.trim().isEmpty()) {
            resetFilter();
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<Apartment> filtered = new ArrayList<>();

        for (Apartment apt : allApartments) {
            String searchable = (
                    (apt.getCity() != null ? apt.getCity() : "") + " " +
                            (apt.getStreet() != null ? apt.getStreet() : "") + " " +
                            (apt.getDescription() != null ? apt.getDescription() : "") + " " +
                            apt.getPrice() + " " +
                            apt.getRoommatesNeeded()
            ).toLowerCase();

            if (searchable.contains(lowerQuery)) {
                filtered.add(apt);
            }
        }

        apartments.setValue(filtered);
    }


    public void reportApartment(Apartment apartment, String reason, String details) {
        repository.reportApartment(apartment.getId(), apartment.getOwnerId(), reason, details)
                .addOnSuccessListener(unused -> toastMessage.setValue("הדיווח נשלח בהצלחה"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחת הדיווח"));
    }



    public void resetFilter() {
        apartments.setValue(new ArrayList<>(allApartments));
    }

}

