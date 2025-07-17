package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Apartment;
import com.example.roomatch.model.GroupChat;
import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatsViewModel extends ViewModel {

    private final ApartmentRepository repository = new ApartmentRepository();
    private final MutableLiveData<List<GroupChat>> groupChats = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Map<String, String>> apartmentIdToAddressMap = new MutableLiveData<>(new HashMap<>());

    public LiveData<List<GroupChat>> loadGroupChats() {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            repository.getGroupChatsForUser(userId)
                    .addOnSuccessListener(chats -> {
                        groupChats.setValue(chats);
                        loadApartmentAddressesForChats(chats); // נטען את הכתובות מיד אחרי
                    })
                    .addOnFailureListener(e -> groupChats.setValue(new ArrayList<>()));
        }
        return groupChats;
    }

    public LiveData<Map<String, String>> getApartmentIdToAddressMap() {
        return apartmentIdToAddressMap;
    }

    public void loadApartmentAddressesForChats(List<GroupChat> chats) {
        List<String> apartmentIds = new ArrayList<>();
        for (GroupChat chat : chats) {
            String apartmentId = chat.getApartmentId();
            if (apartmentId != null && !apartmentIds.contains(apartmentId)) {
                apartmentIds.add(apartmentId);
            }
        }

        Map<String, String> tempMap = new HashMap<>();
        for (String apartmentId : apartmentIds) {
            repository.getApartmentDetails(apartmentId)
                    .addOnSuccessListener(apartment -> {
                        if (apartment != null) {
                            String address = "עיר: " + apartment.getCity() +
                                    ", רחוב: " + apartment.getStreet() +
                                    ", מס' בית: " + apartment.getHouseNumber();
                            tempMap.put(apartmentId, address);
                            apartmentIdToAddressMap.postValue(new HashMap<>(tempMap));
                        }
                    });
        }
    }
}
