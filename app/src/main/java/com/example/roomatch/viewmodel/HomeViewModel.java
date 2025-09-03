package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final UserRepository repository = new UserRepository();
    private final MutableLiveData<String> greeting = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> userList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<String> getGreeting() { return greeting; }
    public LiveData<List<UserProfile>> getUserList() { return userList; }

    public HomeViewModel() {
        loadData();
    }

    private void loadData() {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            greeting.setValue("שלום, משתמש לא מחובר!");
            return;
        }

        // Load greeting
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    UserProfile profile = doc.toObject(UserProfile.class);
                    String name = profile != null ? profile.getFullName() : null;
                    greeting.setValue("שלום, " + (name != null ? name : "משתמש") + "!");
                })
                .addOnFailureListener(e ->
                        greeting.setValue("שלום, משתמש! (שגיאה בטעינה)"));

        // Load other users
        repository.getAllUsersExceptMe()
                .addOnSuccessListener(query -> {
                    List<UserProfile> users = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        UserProfile profile = doc.toObject(UserProfile.class);
                        if (profile != null) {
                            users.add(profile);
                        }
                    }
                    userList.setValue(users);
                })
                .addOnFailureListener(e -> userList.setValue(new ArrayList<>()));
    }
}