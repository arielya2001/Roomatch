package com.example.roomatch.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.GroupChatMessage;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatViewModel extends ViewModel {
    private final ApartmentRepository repository = new ApartmentRepository();
    private final MutableLiveData<List<GroupChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> groupChatId = new MutableLiveData<>();

    public LiveData<List<GroupChatMessage>> loadMessages(String groupChatId) {
        repository.getGroupChatMessages(groupChatId)
                .addOnSuccessListener(messageList -> {
                    Log.d("GroupChatViewModel", "התקבלו " + messageList.size() + " הודעות");

                    List<GroupChatMessage> groupMessages = new ArrayList<>();
                    Map<String, String> userNameCache = new HashMap<>();

                    for (Message msg : messageList) {
                        GroupChatMessage gMsg = new GroupChatMessage();
                        gMsg.setSenderUserId(msg.getFromUserId());
                        gMsg.setSenderId(msg.getFromUserId());
                        gMsg.setContent(msg.getText());
                        gMsg.setTimestamp(msg.getTimestamp());
                        gMsg.setApartmentId(msg.getApartmentId());

                        String senderId = msg.getFromUserId();

                        // אם כבר יש שם בזיכרון
                        if (userNameCache.containsKey(senderId)) {
                            gMsg.setSenderName(userNameCache.get(senderId));
                            groupMessages.add(gMsg);
                        } else {
                            // טען מה-DB ואז עדכן
                            gMsg.setSenderName("טוען..."); // הצג משהו זמני
                            groupMessages.add(gMsg);

                            repository.getUserNameById(senderId).addOnSuccessListener(name -> {
                                userNameCache.put(senderId, name);

                                // עדכן את כל ההודעות עם השם החדש
                                for (GroupChatMessage m : groupMessages) {
                                    if (m.getSenderUserId().equals(senderId)) {
                                        m.setSenderName(name);
                                    }
                                }

                                messages.setValue(new ArrayList<>(groupMessages));
                            }).addOnFailureListener(e -> {
                                Log.e("GroupChatViewModel", "נכשל בשליפת שם ל־" + senderId);
                            });
                        }
                    }

                    // שלח את ההודעות גם אם חלק עוד בלי שמות
                    messages.setValue(groupMessages);
                })

                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בטעינת הודעות: " + e.getMessage());
                });

        return messages;
    }


    public LiveData<String> findGroupChatId(String groupId, String apartmentId) {
        repository.getGroupChatMessagesForGroupAndApartment(groupId, apartmentId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        groupChatId.setValue(querySnapshot.getDocuments().get(0).getId());
                    } else {
                        groupChatId.setValue(null);
                    }
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בחיפוש צ'אט: " + e.getMessage()));
        return groupChatId;
    }

    public void sendMessage(String groupChatId, String text) {
        String userId = repository.getCurrentUserId();
        if (userId != null) {
            repository.sendGroupChatMessage(groupChatId, userId, text)
                    .addOnSuccessListener(aVoid -> loadMessages(groupChatId))
                    .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחת הודעה: " + e.getMessage()));
        }
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }
}
