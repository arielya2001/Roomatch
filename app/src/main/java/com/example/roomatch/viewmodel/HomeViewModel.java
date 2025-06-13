package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final UserRepository repository = new UserRepository();

    private final MutableLiveData<String>            greeting = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> userList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<String>            getGreeting() { return greeting; }
    public LiveData<List<UserProfile>> getUserList() { return userList; }

    public HomeViewModel() { loadData(); }

    /* -------------------------------------------------------------------- */
    private void loadData() {

        String uid = repository.getCurrentUserId();
        if (uid == null) { greeting.setValue("שלום, משתמש לא מחובר!"); return; }

        /* ---- ברכת שלום ---- */
        repository.getMyProfile()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullName");
                    greeting.setValue("שלום, " + (name != null ? name : "משתמש") + "!");
                })
                .addOnFailureListener(e ->
                        greeting.setValue("שלום, משתמש! (שגיאה בטעינה)"));

        /* ---- טעינת משתמשים אחרים ---- */
        repository.getAllUsersExceptMe()
                .addOnSuccessListener(query -> {
                    List<UserProfile> users = new ArrayList<>();
                    query.forEach(doc -> {
                        UserProfile u = new UserProfile(
                                doc.getString("fullName"),
                                doc.getLong("age") != null ? doc.getLong("age").intValue() : 0,
                                doc.getString("lifestyle"),
                                doc.getString("interests")
                        );
                        users.add(u);
                    });
                    userList.setValue(users);
                })
                .addOnFailureListener(e -> userList.setValue(new ArrayList<>()));
    }
}
