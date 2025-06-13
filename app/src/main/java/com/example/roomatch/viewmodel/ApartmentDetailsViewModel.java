package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.HashMap;
import java.util.Map;

public class ApartmentDetailsViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final MutableLiveData<Map<String, Object>> apartmentDetails = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToChatWith = new MutableLiveData<>();

    public ApartmentDetailsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public void loadApartmentDetails(String apartmentId) {
        repository.getApartmentDetails(apartmentId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> data = snapshot.getData();
                        if (data != null) {
                            data.put("apartmentId", snapshot.getId());
                            apartmentDetails.setValue(data);
                        }
                    } else {
                        apartmentDetails.setValue(new HashMap<>());
                    }
                })
                .addOnFailureListener(e -> apartmentDetails.setValue(new HashMap<>()));
    }

    public void setApartmentDetails(Map<String, Object> details) {
        apartmentDetails.setValue(details);
    }

    public LiveData<Map<String, Object>> getApartmentDetails() {
        return apartmentDetails;
    }

    public void onMessageOwnerClicked() {
        if (apartmentDetails.getValue() == null) return;
        String ownerId = (String) apartmentDetails.getValue().get("ownerId");
        String apartmentId = (String) apartmentDetails.getValue().get("apartmentId");

        if (ownerId != null && !ownerId.isEmpty()) {
            navigateToChatWith.setValue(ownerId + "::" + apartmentId);
        }
    }

    public LiveData<String> getNavigateToChatWith() {
        return navigateToChatWith;
    }
}