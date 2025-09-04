package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.SharedGroup;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SharedGroupsViewModel extends ViewModel {

    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<List<SharedGroup>> sharedGroups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, String>> userNamesMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public SharedGroupsViewModel() {
        this.apartmentRepository = new ApartmentRepository();
        this.userRepository = new UserRepository();
    }

    public LiveData<List<SharedGroup>> getSharedGroups() {
        return sharedGroups;
    }

    public LiveData<Map<String, String>> getUserNamesMap() {
        return userNamesMap;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public String getCurrentUserId() {
        return userRepository.getCurrentUserId();
    }

    public void loadSharedGroups() {
        String userId = userRepository.getCurrentUserId();
        if (userId == null) {
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        apartmentRepository.getSharedGroupsForUser(userId)
                .addOnSuccessListener(groups -> {
                    sharedGroups.setValue(groups);
                    loadUserNamesForGroups(groups); // נטעין את השמות
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת קבוצות: " + e.getMessage()));
    }

    public void loadUserNamesForGroups(List<SharedGroup> groups) {
        Set<String> allUserIds = new HashSet<>();
        for (SharedGroup group : groups) {
            if (group.getMemberIds() != null) {
                allUserIds.addAll(group.getMemberIds());
            }
        }

        Map<String, String> idToName = new HashMap<>();
        for (String uid : allUserIds) {
            userRepository.getUserById(uid).addOnSuccessListener(profile -> {
                if (profile != null) {
                    idToName.put(uid, profile.getFullName());
                    userNamesMap.setValue(new HashMap<>(idToName)); // trigger LiveData update
                }
            });
        }
    }
}
