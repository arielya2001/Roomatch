package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class PartnerViewModel extends ViewModel {

    private static final String TAG = "PartnerViewModel"; // תגית לוג ייחודית
    private final UserRepository repository = new UserRepository();
    private final MutableLiveData<List<UserProfile>> partners = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> showProfileDialog = new MutableLiveData<>();
    private final MutableLiveData<String> showReportDialog = new MutableLiveData<>();

    public LiveData<List<UserProfile>> getPartners() { return partners; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<UserProfile> getShowProfileDialog() { return showProfileDialog; }
    public LiveData<String> getShowReportDialog() { return showReportDialog; }

    public PartnerViewModel() {
        loadPartners();
    }

    private void loadPartners() {
        String uid = repository.getCurrentUserId();
        Log.d(TAG, "Loading partners for userId: " + uid);
        if (uid == null) {
            Log.e(TAG, "User not authenticated, UID is null");
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        repository.getPartners()
                .addOnSuccessListener(query -> {
                    List<UserProfile> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        if (!doc.getId().equals(uid)) {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) {
                                profile.setUserId(doc.getId()); // Set the userId from the document ID
                                list.add(profile);
                            }
                        }
                    }
                    Log.d(TAG, "Successfully loaded " + list.size() + " partners");
                    partners.setValue(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading partners: " + e.getMessage(), e);
                    toastMessage.setValue("שגיאה בטעינת שותפים: " + e.getMessage());
                });
    }

    public void showProfileDialog(UserProfile partner) {
        Log.d(TAG, "Showing profile dialog for: " + (partner != null ? partner.getFullName() : "null"));
        showProfileDialog.setValue(partner);
    }

    public void showReportDialog(String fullName) {
        Log.d(TAG, "Showing report dialog for: " + fullName);
        showReportDialog.setValue(fullName);
    }

    public void sendMatchRequest(UserProfile partner) {
        String currentUserId = repository.getCurrentUserId();
        Log.d(TAG, "Sending match request - Current User ID: " + currentUserId);
        Log.d(TAG, "Sending match request - Partner User ID: " + (partner != null ? partner.getUserId() : "null"));
        if (currentUserId != null && partner != null && partner.getUserId() != null) { // בדיקה מפורטת יותר
            Log.d(TAG, "Sending match request from " + currentUserId + " to " + partner.getUserId());
            repository.sendMatchRequest(currentUserId, partner.getUserId())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Match request sent successfully");
                        toastMessage.setValue("בקשה נשלחה");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sending match request: " + e.getMessage(), e);
                        toastMessage.setValue("שגיאה בשליחת בקשה: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "Failed to send match request - Missing user details: currentUserId=" + currentUserId + ", partnerUserId=" + (partner != null ? partner.getUserId() : "null"));
            toastMessage.setValue("שגיאה: פרטי משתמש חסרים");
        }
    }
}