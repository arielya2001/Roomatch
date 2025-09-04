package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.ArrayList;
import java.util.List;

public class SeekerApartmentsViewModel extends ViewModel {

    private final ApartmentRepository repository = new ApartmentRepository();
    private final MutableLiveData<List<Apartment>> apartments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<List<Apartment>> getApartments() { return apartments; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void loadApartments() {
        repository.getApartments()
                .addOnSuccessListener(apartmentList -> {
                    apartments.setValue(apartmentList);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בטעינת דירות: " + e.getMessage());
                    apartments.setValue(new ArrayList<>());
                });
    }
}