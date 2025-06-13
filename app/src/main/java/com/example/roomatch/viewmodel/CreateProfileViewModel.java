package com.example.roomatch.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.R;
import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateProfileViewModel extends ViewModel {

    /* ---------- Repository ---------- */
    private final UserRepository userRepo = new UserRepository();

    /* ---------- LiveData ---------- */
    private final MutableLiveData<String>  toastMessage  = new MutableLiveData<>();
    private final MutableLiveData<Boolean> profileSaved  = new MutableLiveData<>();

    public LiveData<String>  getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getProfileSaved() { return profileSaved; }

    /* ---------- שמירת פרופיל חדש ---------- */
    public void saveProfile(String fullName, String ageStr, int selectedGenderId,
                            boolean clean, boolean smoker, boolean nightOwl, boolean quiet, boolean party,
                            boolean music, boolean sports, boolean travel, boolean cooking, boolean reading,
                            int selectedUserTypeId) {

        /* ----- אימות משתמש ----- */
        String uid = userRepo.getCurrentUserId();
        if (uid == null) { toastMessage.setValue("שגיאה: משתמש לא מחובר"); return; }

        /* ----- ולידציה בסיסית ----- */
        if (fullName.isEmpty() || fullName.length() < 2) {
            toastMessage.setValue("הכנס שם מלא (לפחות 2 תווים)");  return;
        }
        Integer age = tryParseInt(ageStr);
        if (age == null || age <= 0) {
            toastMessage.setValue("הכנס גיל תקין (גדול מ‑0)");      return;
        }

        /* ----- מין ----- */
        String gender;
        if      (selectedGenderId == R.id.radioMale)   gender = "זכר";
        else if (selectedGenderId == R.id.radioFemale) gender = "נקבה";
        else if (selectedGenderId == R.id.radioOther)  gender = "אחר";
        else { toastMessage.setValue("בחר מין"); return; }

        /* ----- סגנון חיים & תחומי עניין ----- */
        List<String> lifestyleList = new ArrayList<>();
        if (clean)    lifestyleList.add("נקי");
        if (smoker)   lifestyleList.add("מעשן");
        if (nightOwl) lifestyleList.add("חיית לילה");
        if (quiet)    lifestyleList.add("שקט");
        if (party)    lifestyleList.add("אוהב מסיבות");
        String lifestyle = TextUtils.join(", ", lifestyleList);

        List<String> interestList = new ArrayList<>();
        if (music)   interestList.add("מוזיקה");
        if (sports)  interestList.add("ספורט");
        if (travel)  interestList.add("טיולים");
        if (cooking) interestList.add("בישול");
        if (reading) interestList.add("קריאה");
        String interests = TextUtils.join(", ", interestList);

        /* ----- סוג משתמש ----- */
        String userType;
        if      (selectedUserTypeId == R.id.radioSeeker) userType = "seeker";
        else if (selectedUserTypeId == R.id.radioOwner)  userType = "owner";
        else { toastMessage.setValue("בחר סוג משתמש"); return; }

        /* ----- בניית map לשמירה ----- */
        Map<String,Object> profile = new HashMap<>();
        profile.put("uid",        uid);        // לשימוש עתידי
        profile.put("fullName",   fullName);
        profile.put("age",        age);
        profile.put("gender",     gender);
        profile.put("lifestyle",  lifestyle);
        profile.put("interests",  interests);
        profile.put("userType",   userType);

        /* ----- שמירה בבסיס הנתונים ----- */
        userRepo.saveMyProfile(profile)
                .addOnSuccessListener(v -> {
                    profileSaved.setValue(true);
                    toastMessage.setValue("הפרופיל נשמר בהצלחה!");
                })
                .addOnFailureListener(e -> {
                    profileSaved.setValue(false);
                    toastMessage.setValue("שגיאה בשמירת הפרופיל: " + e.getMessage());
                });
    }

    /* ---------- helper ---------- */
    private Integer tryParseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
}