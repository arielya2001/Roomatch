package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Contact;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MatchRequestsViewModel extends ViewModel {
    private static final String TAG = "MatchRequestsViewModel";
    private final ApartmentRepository repository;
    private final MutableLiveData<List<Contact>> matchRequests = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public MatchRequestsViewModel() {
        this.repository = new ApartmentRepository();
        loadMatchRequests(); // טעינה אוטומטית בעת יצירת ה-ViewModel
    }

    public LiveData<List<Contact>> getMatchRequests() {
        return matchRequests;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadMatchRequests() {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            Log.d(TAG, "Loading match requests for userId: " + userId);
            repository.getPendingMatchRequests(userId)
                    .addOnSuccessListener(requests -> {
                        matchRequests.setValue(requests);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading match requests: " + e.getMessage(), e);
                        toastMessage.setValue("שגיאה בטעינת בקשות: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "Cannot load match requests: userId is null");
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
        }
    }


    public void approveMatchRequest(String requestId) {
        String currentUserId = repository.getCurrentUserId();
        if (currentUserId != null && requestId != null) {
            repository.approveMatchAndUpdateContacts(requestId, currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        toastMessage.setValue("בקשה אושרה והמשתמש נוסף לקשרים");
                        loadMatchRequests();
                    })
                    .addOnFailureListener(e -> {
                        toastMessage.setValue("שגיאה באישור: " + e.getMessage());
                    });
        } else {
            toastMessage.setValue("שגיאה: משתמש או מזהה בקשה חסרים");
        }
    }

    public void rejectMatchRequest(String requestId) {
        if (requestId != null) {
            Log.d(TAG, "Rejecting match request: " + requestId);
            repository.deleteMatchRequest(requestId)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Match request rejected and deleted");
                        toastMessage.setValue("בקשה נדחתה");
                        loadMatchRequests(); // רענון הרשימה לאחר דחייה
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error rejecting match: " + e.getMessage(), e);
                        toastMessage.setValue("שגיאה בדחייה: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "Cannot reject match: requestId is null");
            toastMessage.setValue("שגיאה: מזהה בקשה חסר");
        }
    }
}