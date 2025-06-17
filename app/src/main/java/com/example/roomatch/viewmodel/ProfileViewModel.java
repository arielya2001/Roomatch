package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final UserRepository repository = new UserRepository();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> editRequested = new MutableLiveData<>();

    public LiveData<UserProfile> getProfile() { return profile; }
    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getEditRequested() { return editRequested; }

    public void loadProfile() {
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        UserProfile userProfile = doc.toObject(UserProfile.class);
                        profile.setValue(userProfile);
                    } else {
                        toastMessage.setValue("פרופיל לא נמצא");
                    }
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
    }

    public void loadMessages() {
        repository.getInboxMessages()
                .addOnSuccessListener(query -> {
                    List<Message> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            list.add(message);
                        }
                    }
                    messages.setValue(list);
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת הודעות: " + e.getMessage()));
    }

    public void requestEditProfile() {
        editRequested.setValue(true);
    }

    public void resetEditRequest() {
        editRequested.setValue(false);
    }

    public void updateProfile(String fullName, String ageStr, String gender, String lifestyle, String interests) {
        if (fullName == null || fullName.trim().length() < 2) {
            toastMessage.setValue("הכנס שם מלא (לפחות 2 תווים)");
            return;
        }
        Integer age = tryParseInt(ageStr);
        if (age == null || age <= 0) {
            toastMessage.setValue("הכנס גיל תקין (גדול מ-0)");
            return;
        }

        UserProfile updated = new UserProfile(
                fullName.trim(),
                age,
                gender != null ? gender.trim() : null,
                lifestyle != null ? lifestyle.trim() : null,
                interests != null ? interests.trim() : null,
                profile.getValue() != null ? profile.getValue().getUserType() : "seeker"
        );

        repository.saveMyProfile(repository.getCurrentUserId(), updated)
                .addOnSuccessListener(v -> {
                    profile.setValue(updated);
                    toastMessage.setValue("פרופיל עודכן בהצלחה!");
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בעדכון פרופיל: " + e.getMessage()));
    }

    private Integer tryParseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}