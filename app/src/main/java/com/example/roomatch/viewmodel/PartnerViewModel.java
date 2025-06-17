package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PartnerViewModel extends ViewModel {

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
        if (uid == null) {
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
                                list.add(profile);
                            }
                        }
                    }
                    partners.setValue(list);
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת שותפים: " + e.getMessage()));
    }

    public void showProfileDialog(UserProfile partner) {
        showProfileDialog.setValue(partner);
    }

    public void showReportDialog(String fullName) {
        showReportDialog.setValue(fullName);
    }
}