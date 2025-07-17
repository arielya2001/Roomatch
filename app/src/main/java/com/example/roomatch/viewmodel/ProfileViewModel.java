package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends ViewModel {

    /* ---------- Repository ---------- */
    private final UserRepository repository = new UserRepository();

    /* ---------- LiveData ---------- */
    private final MutableLiveData<Map<String, Object>>         profile       = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, Object>>>   messages      = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String>                      toastMessage  = new MutableLiveData<>();
    private final MutableLiveData<Boolean>                     editRequested = new MutableLiveData<>();

    /* ---------- Getters ---------- */
    public LiveData<Map<String, Object>>       getProfile()      { return profile;       }
    public LiveData<List<Map<String, Object>>> getMessages()     { return messages;      }
    public LiveData<String>                    getToastMessage() { return toastMessage;  }
    public LiveData<Boolean>                   getEditRequested(){ return editRequested; }

    /* ------------------------------------------------------------------ */
    /**  טעינת פרטי‑הפרופיל הנוכחי  */
    public void loadProfile() {
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) profile.setValue(doc.getData());
                    else              toastMessage.setValue("פרופיל לא נמצא");
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /**  טעינת ההודעות (Inbox) של המשתמש  */
    public void loadMessages() {
        repository.getInboxMessages()
                .addOnSuccessListener(q -> {
                    List<Map<String,Object>> list = new ArrayList<>();
                    q.forEach(doc -> list.add(doc.getData()));
                    messages.setValue(list);
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת הודעות: " + e.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    public void requestEditProfile() { editRequested.setValue(true); }
    public void resetEditRequest()   { editRequested.setValue(false); }

    /* ------------------------------------------------------------------ */
    /**  עדכון הפרופיל  */
    public void updateProfile(String fullName, String ageStr,
                              String gender, String lifestyle, String interests) {

        /* ולידציה בסיסית */
        if (fullName == null || fullName.trim().length() < 2) {
            toastMessage.setValue("הכנס שם מלא (לפחות 2 תווים)");
            return;
        }
        Integer age = tryParseInt(ageStr);
        if (age == null || age <= 0) {
            toastMessage.setValue("הכנס גיל תקין (גדול מ‑0)");
            return;
        }

        Map<String,Object> updated = new HashMap<>();
        updated.put("fullName",  fullName.trim());
        updated.put("age",       age);
        updated.put("gender",    gender);
        updated.put("lifestyle", lifestyle);
        updated.put("interests", interests);
        /* נשמור את userType הקיים (אם קיים) */
        Object currentType = profile.getValue() != null ? profile.getValue().get("userType") : "seeker";
        updated.put("userType", currentType);

        repository.saveMyProfile(updated)
                .addOnSuccessListener(v -> {
                    profile.setValue(updated);
                    toastMessage.setValue("פרופיל עודכן בהצלחה!");
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בעדכון פרופיל: " + e.getMessage()));
    }

    /* ---------- Helper ---------- */
    private Integer tryParseInt(String val) {
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return null; }
    }
}
