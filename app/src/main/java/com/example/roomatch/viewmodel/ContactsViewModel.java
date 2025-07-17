package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Contact;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class ContactsViewModel extends ViewModel {
    private static final String TAG = "ContactsViewModel";
    private final ApartmentRepository repository;
    private final MutableLiveData<List<Contact>> contacts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ContactsViewModel() {
        this.repository = new ApartmentRepository();
        loadContacts();
    }

    public LiveData<List<Contact>> getContacts() {
        return contacts;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadContacts() {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            Log.d(TAG, "Loading contacts for userId: " + userId);
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (profile != null && profile.getContactIds() != null) {
                            List<Contact> contactList = new ArrayList<>();
                            for (String contactId : profile.getContactIds()) {
                                repository.getUserById(contactId)
                                        .addOnSuccessListener(contactProfile -> {
                                            if (contactProfile != null) {
                                                Contact contact = new Contact();
                                                contact.setUserId(contactId);
                                                contact.setFullName(contactProfile.getFullName());
                                                contactList.add(contact);
                                                contacts.setValue(new ArrayList<>(contactList));
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Error fetching contact " + contactId + ": " + e.getMessage(), e));
                            }
                        } else {
                            contacts.setValue(new ArrayList<>());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user profile: " + e.getMessage(), e);
                        contacts.setValue(new ArrayList<>());
                    });
        } else {
            Log.e(TAG, "Cannot load contacts: userId is null");
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
            contacts.setValue(new ArrayList<>());
        }
    }

    public void createSharedGroup(List<String> memberIds) {
        String currentUserId = repository.getCurrentUserId();
        if (currentUserId != null && !memberIds.isEmpty()) {
            Log.d(TAG, "Creating shared group with memberIds: " + memberIds);
            // נניח שהריפוזיטורי יטפל בשמירה עצמה
            repository.createSharedGroup(memberIds)
                    .addOnSuccessListener(aVoid -> toastMessage.setValue("קבוצה נוצרה בהצלחה"))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating shared group: " + e.getMessage(), e);
                        toastMessage.setValue("שגיאה ביצירת קבוצה: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "Cannot create group: invalid userId or empty selection");
            toastMessage.setValue("שגיאה: בחר לפחות חבר אחד");
        }
    }
}
