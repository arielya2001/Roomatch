package com.example.roomatch.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.GroupChat;
import com.example.roomatch.model.repository.ApartmentRepository;

import java.util.ArrayList;
import java.util.List;

public class GroupChatsViewModel extends ViewModel {
    private final ApartmentRepository repository = new ApartmentRepository();
    private final MutableLiveData<List<GroupChat>> groupChats = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<GroupChat>> loadGroupChats() {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            repository.getGroupChatsForUser(userId)
                    .addOnSuccessListener(chats -> groupChats.setValue(chats))
                    .addOnFailureListener(e -> groupChats.setValue(new ArrayList<>()));
        }
        return groupChats;
    }
}