package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateProfileViewModel extends ViewModel {

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> profileSaved = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getProfileSaved() {
        return profileSaved;
    }

    public void saveProfile(UserProfile profile) {
        if (profile.getFullName() == null || profile.getFullName().isEmpty()) {
            toastMessage.setValue("שם מלא הוא שדה חובה");
            return;
        }
        if (profile.getAge() <= 0) {
            toastMessage.setValue("גיל חייב להיות מספר חיובי");
            return;
        }
        if (profile.getGender() == null || profile.getGender().isEmpty()) {
            toastMessage.setValue("נא לבחור מגדר");
            return;
        }
        if (profile.getUserType() == null || profile.getUserType().isEmpty()) {
            toastMessage.setValue("נא לבחור סוג משתמש");
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
            return;
        }
        profile.setUserId(userId);
        db.collection("users").document(userId).set(profile)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("פרופיל נשמר בהצלחה");
                    profileSaved.setValue(true);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בשמירת פרופיל: " + e.getMessage());
                    profileSaved.setValue(false);
                });
    }
}