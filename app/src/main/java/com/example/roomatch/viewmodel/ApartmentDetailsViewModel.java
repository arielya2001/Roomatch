package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.SharedGroup;
import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.ArrayList;
import java.util.List;

public class ApartmentDetailsViewModel extends ViewModel {
    private final ApartmentRepository repository;
    private final MutableLiveData<Apartment> apartmentDetails = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToChatWith = new MutableLiveData<>();
    private final MutableLiveData<List<SharedGroup>> availableGroups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // קונסטרקטור עם הזרקת ApartmentRepository
    public ApartmentDetailsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public LiveData<Apartment> getApartmentDetails() {
        return apartmentDetails;
    }

    public LiveData<String> getNavigateToChatWith() {
        return navigateToChatWith;
    }

    public LiveData<List<SharedGroup>> getAvailableGroups() {
        return availableGroups;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void setApartmentDetails(Apartment apartment) {
        apartmentDetails.setValue(apartment);
    }

    public void onMessageOwnerClicked() {
        Apartment apartment = apartmentDetails.getValue();
        if (apartment != null) {
            String chatKey = apartment.getOwnerId() + "::" + apartment.getId();
            navigateToChatWith.setValue(chatKey);
        } else {
            toastMessage.setValue("שגיאה: פרטי הדירה חסרים");
        }
    }

    public void loadAvailableGroups() {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            repository.getSharedGroupsForUser(userId)
                    .addOnSuccessListener(availableGroups::setValue)
                    .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת קבוצות: " + e.getMessage()));
        } else {
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
        }
    }

    public void sendGroupMessage(Apartment apartment, SharedGroup group) {
        if (apartment != null && group != null && group.getId() != null) {
            repository.sendGroupMessage(apartment.getOwnerId(), apartment.getId(), group.getId())
                    .addOnSuccessListener(aVoid -> toastMessage.setValue("הודעה נשלחה"))
                    .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחת הודעה: " + e.getMessage()));
        } else {
            toastMessage.setValue("שגיאה: פרטים חסרים");
        }
    }

    public void clearNavigation() {
        navigateToChatWith.setValue(null);
    }
}