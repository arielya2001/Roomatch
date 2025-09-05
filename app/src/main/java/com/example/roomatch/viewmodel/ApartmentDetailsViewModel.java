package com.example.roomatch.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.SharedGroup;
import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApartmentDetailsViewModel extends ViewModel {
    private final ApartmentRepository repository;
    private final MutableLiveData<Apartment> apartmentDetails = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToChatWith = new MutableLiveData<>();
    private final MutableLiveData<List<SharedGroup>> availableGroups = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // קונסטרקטור עם הזרקת ApartmentRepository
    public ApartmentDetailsViewModel(ApartmentRepository repository) {
        this.repository = repository;
    }

    public LiveData<Apartment> getApartmentDetails() {
        return apartmentDetails;
    }

    public LiveData<String> getNavigateToChatWith() {
        return navigateToChatWith;
    }

    public LiveData<List<SharedGroup>> getAvailableGroups() {
        return availableGroups;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void setApartmentDetails(Apartment apartment) {
        apartmentDetails.setValue(apartment);
    }

    public void onMessageOwnerClicked() {
        Apartment apartment = apartmentDetails.getValue();
        if (apartment != null) {
            String chatKey = apartment.getOwnerId() + "::" + apartment.getId();
            navigateToChatWith.setValue(chatKey);
        } else {
            toastMessage.setValue("שגיאה: פרטי הדירה חסרים");
        }
    }

    public void loadAvailableGroups() {
        String userId = repository.getCurrentUserId();
        Log.d("ApartmentDetailsVM", "🚀 Starting loadAvailableGroups()");
        Log.d("ApartmentDetailsVM", "Current user ID: " + userId);

        if (userId != null) {
            repository.getSharedGroupsForUser(userId)
                    .addOnSuccessListener(groups -> {
                        Log.d("ApartmentDetailsVM", "✅ Loaded " + groups.size() + " groups from Firestore");

                        List<SharedGroup> adminGroups = new ArrayList<>();
                        for (SharedGroup group : groups) {
                            Map<String, String> roles = group.getRoles();
                            Log.d("ApartmentDetailsVM", "Group: " + group.getName());
                            Log.d("ApartmentDetailsVM", " - Creator: " + group.getCreatorId());
                            Log.d("ApartmentDetailsVM", " - Roles map: " + roles);

                            boolean isAdmin = userId.equals(group.getCreatorId()) ||
                                    (roles != null && "admin".equals(roles.get(userId)));

                            Log.d("ApartmentDetailsVM", " - isAdmin = " + isAdmin);

                            if (isAdmin) {
                                adminGroups.add(group);
                            }
                        }

                        Log.d("ApartmentDetailsVM", "✅ Final adminGroups count: " + adminGroups.size());
                        availableGroups.setValue(adminGroups);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ApartmentDetailsVM", "❌ Error loading groups: " + e.getMessage());
                        toastMessage.setValue("שגיאה בטעינת קבוצות: " + e.getMessage());
                    });
        } else {
            Log.e("ApartmentDetailsVM", "❌ userId is null – user not logged in?");
            toastMessage.setValue("שגיאה: משתמש לא מחובר");
        }
    }

    public void sendGroupMessageAndCreateChat(Apartment apartment, SharedGroup group) {
        if (apartment != null && group != null && group.getId() != null) {
            String ownerId = apartment.getOwnerId();
            String apartmentId = apartment.getId();
            String groupId = group.getId();

            repository.sendGroupMessageAndCreateChat(ownerId, apartmentId, groupId)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ApartmentDetailsVM", "✅ Group chat created successfully");
                        toastMessage.setValue("צ'אט קבוצתי נוצר בהצלחה");

                        // נבצע עכשיו יצירה + קבלת groupChatId אמיתי לניווט נכון
                        repository.createGroupChatAndReturnId(ownerId, apartmentId, groupId)
                                .addOnSuccessListener(groupChatId -> {
                                    navigateToChatWith.setValue(groupChatId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ApartmentDetailsVM", "❌ Error retrieving groupChatId: " + e.getMessage());
                                    toastMessage.setValue("שגיאה באיתור הצ'אט הקבוצתי: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ApartmentDetailsVM", "❌ Error creating group chat: " + e.getMessage());
                        toastMessage.setValue("שגיאה ביצירת צ'אט: " + e.getMessage());
                    });
        } else {
            toastMessage.setValue("שגיאה: פרטים חסרים");
        }
    }


    private void navigateToGroupChat(String groupId, String apartmentId) {
        // ניתוב זמני עם groupId ו-apartmentId; נצטרך לחפש את groupChatId
        // (נניח ש-GroupChatFragment יטפל בחיפוש ה-groupChatId)
        String chatKey = groupId + "::" + apartmentId;
        navigateToChatWith.setValue(chatKey);
    }

    public void clearNavigation() {
        navigateToChatWith.setValue(null);
    }
}