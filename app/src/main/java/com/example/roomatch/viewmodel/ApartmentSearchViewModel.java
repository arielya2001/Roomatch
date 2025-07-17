package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ApartmentSearchViewModel extends ViewModel {
    private final ApartmentRepository repository;
    private final MutableLiveData<List<Apartment>> apartments = new MutableLiveData<>(new ArrayList<>());
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
        repository.getApartments().addOnSuccessListener(snapshot -> {
            List<Apartment> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Apartment apt = doc.toObject(Apartment.class);
                if (apt != null) {
                    apt.setId(doc.getId());
                    list.add(apt);
                }
            }
            apartments.setValue(list);
        }).addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת הדירות"));
    }

    public void applyFilter(String field, boolean ascending) {
        repository.getApartmentsOrderedBy(field, ascending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING)
                .addOnSuccessListener(snapshot -> {
                    List<Apartment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Apartment apt = doc.toObject(Apartment.class);
                        if (apt != null) {
                            apt.setId(doc.getId());
                            list.add(apt);
                        }
                    }
                    apartments.setValue(list);
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בסינון"));
    }

    public void searchApartments(String query) {
        List<Apartment> current = apartments.getValue();
        if (current == null) return;
        List<Apartment> filtered = new ArrayList<>();
        for (Apartment apt : current) {
            if ((apt.getCity() != null && apt.getCity().toLowerCase().contains(query.toLowerCase())) ||
                    (apt.getStreet() != null && apt.getStreet().toLowerCase().contains(query.toLowerCase())) ||
                    (apt.getDescription() != null && apt.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(apt);
            }
        }
        apartments.setValue(filtered);
    }

    public void resetList(List<Apartment> originalList) {
        apartments.setValue(new ArrayList<>(originalList));
    }
}
