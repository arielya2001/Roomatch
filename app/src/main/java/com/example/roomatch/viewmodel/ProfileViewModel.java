package com.example.roomatch.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.model.UserSession; // <<— חשוב לייבא
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final UserRepository repository = new UserRepository();

    // מקבלים ישירות את ה־LiveData מתוך ה־UserSession (קאש + האזנה חיה)
    private final LiveData<UserProfile> profile = UserSession.getInstance().getProfileLiveData();

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> editRequested = new MutableLiveData<>();

    private String selectedCity;
    private String selectedStreet;
    private LatLng selectedLocation;

    public LiveData<UserProfile> getProfile() { return profile; }
    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getEditRequested() { return editRequested; }

    /** מפעיל את ה־Session (טעינה ראשונית + מאזין). אין קריאת Firestore ידנית. */
    public void loadProfile() {
        UserSession.getInstance().ensureStarted()
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת פרופיל: " + e.getMessage()));
    }

    public void setSelectedAddress(String city, String street, LatLng location) {
        this.selectedCity = city;
        this.selectedStreet = street;
        this.selectedLocation = location;
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

    public boolean isCurrentUserOwner() {
        UserProfile p = profile.getValue();
        return p != null && "owner".equals(p.getUserType());
    }

    public void resetEditRequest() {
        editRequested.setValue(false);
    }

    /** עדכון פרופיל דרך ה־UserSession (write-through + עדכון קאש/LiveData אוטומטי) */
    public void updateProfile(String fullName, String ageStr, String gender, String lifestyle,
                              String interests, String city, String street, LatLng loc, String description) {

        if (fullName == null || fullName.trim().length() < 2) {
            toastMessage.setValue("הכנס שם מלא (לפחות 2 תווים)");
            return;
        }
        Integer age = tryParseInt(ageStr);
        if (age == null || age <= 0) {
            toastMessage.setValue("הכנס גיל תקין (גדול מ-0)");
            return;
        }
        if (loc == null) {
            toastMessage.setValue("מיקום לא חוקי");
            return;
        }

        UserProfile current = profile.getValue();
        String userType = current != null && current.getUserType() != null ? current.getUserType() : "seeker";

        UserProfile updated = new UserProfile(
                fullName.trim(),
                age,
                gender != null ? gender.trim() : null,
                lifestyle != null ? lifestyle.trim() : null,
                interests != null ? interests.trim() : null,
                userType,
                city,
                street,
                loc.latitude,
                loc.longitude,
                description
        );

        UserSession.getInstance().updateMyProfile(updated)
                .addOnSuccessListener(v -> toastMessage.setValue("פרופיל עודכן בהצלחה!"))
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בעדכון פרופיל: " + e.getMessage()));
    }

    private Integer tryParseInt(String val) {
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return null; }
    }
}
