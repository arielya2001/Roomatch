package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                        try
                        {
                            UserProfile profile = doc.toObject(UserProfile.class);
                            if (profile != null) {
                                if (profile.getUserId() == null) {
                                    // מחיקת הדוקומנט מהקולקשיין
                                    doc.getReference().delete();
                                } else {
                                    users.add(profile);
                                }
                            }
                        }
                        catch (RuntimeException mapEx)
                        {
                            String userType = doc.getString("userType");
                            List<String> contactsIds;
                            String description="";
                            String interests="";
                            String lifestyle="";
                            String selectedCity="";
                            Map<String,Double> selectedLocation = Collections.emptyMap();
                            String selectedStreet="";
                            if(userType=="seeker")
                            {
                                Object contactsIdsRAW = doc.get("contactIds");
                                if(contactsIdsRAW instanceof List)
                                {
                                    @SuppressWarnings("unchecked")
                                    List<String> list = (List<String>) contactsIdsRAW;
                                    contactsIds=list;
                                }
                                description=doc.getString("description");
                                interests = doc.getString("interests");
                                lifestyle = doc.getString("lifestyle");
                                selectedCity = doc.getString("selectedCity");
                                Object selectedLocationRAW = doc.get("contactIds");

                                if(selectedLocationRAW instanceof Map)
                                {
                                    @SuppressWarnings("unchecked")
                                    Map<String,Double> map = (Map<String,Double>) selectedLocationRAW;
                                    selectedLocation=map;
                                }

                                selectedStreet = doc.getString("selectedStreet");

                            }
                            long age = doc.getLong("age");


                            String fullName = doc.getString("fullName");
                            String gender = doc.getString("gender");
                            String userId = doc.getString("userId");
                            UserProfile user = new UserProfile(fullName,(int)age,gender,lifestyle,interests,userType,selectedCity,selectedStreet,selectedLocation.get("latitude"),selectedLocation.get("longitude"),description);
                            users.add(user);
                        }

                    }
                    userList.setValue(users);
                })
                .addOnFailureListener(e -> userList.setValue(new ArrayList<>()));
    }
}