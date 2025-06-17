package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.repository.ApartmentRepository;

public class ApartmentDetailsViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final MutableLiveData<Apartment> apartmentDetails = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToChatWith = new MutableLiveData<>();

    public ApartmentDetailsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public void loadApartmentDetails(String apartmentId) {
        repository.getApartmentDetails(apartmentId)
                .addOnSuccessListener(apartment -> {
                    if (apartment != null) {
                        apartmentDetails.setValue(apartment);
                    } else {
                        apartmentDetails.setValue(null);
                    }
                })
                .addOnFailureListener(e -> apartmentDetails.setValue(null));
    }

    public void setApartmentDetails(Apartment apartment) {
        apartmentDetails.setValue(apartment);
    }

    public LiveData<Apartment> getApartmentDetails() {
        return apartmentDetails;
    }

    public void onMessageOwnerClicked() {
        Apartment apartment = apartmentDetails.getValue();
        if (apartment != null && apartment.getOwnerId() != null && !apartment.getOwnerId().isEmpty()) {
            navigateToChatWith.setValue(apartment.getOwnerId() + "::" + apartment.getId());
        }
    }

    public LiveData<String> getNavigateToChatWith() {
        return navigateToChatWith;
    }
}