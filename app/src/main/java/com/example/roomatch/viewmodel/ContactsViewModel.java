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
import java.util.Arrays;
import java.util.HashSet;
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
        String currentUserId = repository.getCurrentUserId();
        Log.d(TAG, "👤 currentUserId = " + currentUserId);

        if (currentUserId == null) {
            Log.e(TAG, "❌ Cannot load contacts: userId is null");
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
            contacts.setValue(new ArrayList<>());
            return;
        }

        db.collection("match_requests")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Contact> contactList = new ArrayList<>();
                    Log.d(TAG, "🔍 Total match_requests: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "📭 No accepted match_requests found");
                        contacts.setValue(contactList);
                        return;
                    }

                    for (var doc : querySnapshot.getDocuments()) {
                        String senderId = doc.getString("fromUserId");
                        String receiverId = doc.getString("toUserId");
                        String status = doc.getString("status");
                        Log.d(TAG, "📄 match_request -> sender: " + senderId + ", receiver: " + receiverId + ", status: " + status);

                        // רק אם אני אחד מהם
                        if (!currentUserId.equals(senderId) && !currentUserId.equals(receiverId)) {
                            Log.d(TAG, "⛔️ Skipping request, user not involved");
                            continue;
                        }

                        // קבע מי מהשניים הוא החבר (לא אתה)
                        String contactId = currentUserId.equals(senderId) ? receiverId : senderId;
                        Log.d(TAG, "✅ Match found with currentUserId. Getting user: " + contactId);

                        repository.getUserById(contactId)
                                .addOnSuccessListener(contactProfile -> {
                                    if (contactProfile != null) {
                                        Log.d(TAG, "🙋 contactProfile found: " + contactProfile.getFullName());
                                        Contact contact = new Contact();
                                        contact.setUserId(contactId);
                                        contact.setFullName(contactProfile.getFullName());
                                        contact.setLifestyle(contactProfile.getLifestyle());
                                        contact.setInterests(Arrays.asList(contactProfile.getInterests().split(",")));
                                        contactList.add(contact);
                                        contacts.setValue(new ArrayList<>(contactList));  // trigger update
                                    } else {
                                        Log.d(TAG, "🚫 contactProfile is null for userId: " + contactId);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "❌ Error fetching user " + contactId + ": " + e.getMessage(), e)
                                );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading match_requests: " + e.getMessage(), e);
                    contacts.setValue(new ArrayList<>());
                });
    }




    public void createSharedGroup(List<String> memberIds) {
        String currentUserId = repository.getCurrentUserId();

        if (currentUserId == null || memberIds.isEmpty()) {
            Log.e(TAG, "Cannot create group: invalid userId or empty selection");
            toastMessage.setValue("שגיאה: בחר לפחות חבר אחד");
            return;
        }

        // ודא שהיוזר הנוכחי גם כלול בקבוצה
        List<String> fullMemberList = new ArrayList<>(memberIds);
        if (!fullMemberList.contains(currentUserId)) {
            fullMemberList.add(currentUserId);
        }

        Log.d(TAG, "🔍 Checking for existing groups with members: " + fullMemberList);

        db.collection("shared_groups")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        List<String> existingMembers = (List<String>) doc.get("memberIds");

                        if (existingMembers != null && sameMembers(existingMembers, fullMemberList)) {
                            Log.d(TAG, "⚠️ Group already exists with these members: " + existingMembers);
                            toastMessage.setValue("כבר קיימת קבוצה עם אותם חברים");
                            return;
                        }
                    }

                    // אם לא קיימת - צור
                    Log.d(TAG, "✅ Creating shared group with memberIds: " + fullMemberList);
                    repository.createSharedGroup(fullMemberList)
                            .addOnSuccessListener(aVoid ->
                                    toastMessage.setValue("קבוצה נוצרה בהצלחה")
                            )
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error creating shared group: " + e.getMessage(), e);
                                toastMessage.setValue("שגיאה ביצירת קבוצה: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error checking existing groups: " + e.getMessage(), e);
                    toastMessage.setValue("שגיאה בבדיקת קבוצות קיימות");
                });
    }

    private boolean sameMembers(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) return false;
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }


}
