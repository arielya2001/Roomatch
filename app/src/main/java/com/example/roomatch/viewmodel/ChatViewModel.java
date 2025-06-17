package com.example.roomatch.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepo;
    private final UserRepository userRepo;

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Chat>> chats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    public ChatViewModel(ChatRepository chatRepo, UserRepository userRepo) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
    }

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<List<Chat>> getChats() { return chats; }
    public LiveData<String> getToast() { return toast; }

    private String uid() { return userRepo.getCurrentUserId(); }

    public void sendMessage(String chatId, String toUserId, String apartmentId, String text) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        Message message = new Message(fromUid, toUserId, text, apartmentId, System.currentTimeMillis());
        chatRepo.sendMessage(chatId, message)
                .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
    }

    public void sendMessageWithImage(String chatId, String toUserId, String apartmentId, String text, Uri imageUri) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        Message message = new Message(fromUid, toUserId, text, apartmentId, System.currentTimeMillis());
        chatRepo.sendMessageWithImage(chatId, message, imageUri)
                .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
    }

    public Query getChatMessagesQuery(String chatId, int limit) {
        return chatRepo.getPaginatedChatMessagesQuery(chatId, limit > 0 ? limit : 20);
    }

    public void markMessagesAsRead(String chatId) {
        String me = uid();
        if (me == null) return;

        chatRepo.markMessagesAsRead(chatId, me)
                .addOnFailureListener(e -> toast.setValue("שגיאה בסימון כנקראו: " + e.getMessage()));
    }

    public void loadChats() {
        String me = uid();
        if (me == null) {
            toast.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        chatRepo.getChatsForUser(me)
                .addOnSuccessListener(snapshot -> {
                    List<Chat> tmp = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat == null) continue;

                        chat.setId(doc.getId());
                        String fromUid = chat.getFromUserId();
                        String aptId = chat.getApartmentId();
                        if (fromUid == null || aptId == null) continue;

                        // ודא שאין כפילות
                        boolean exists = tmp.stream()
                                .anyMatch(c -> c.getFromUserId().equals(fromUid)
                                        && c.getApartmentId().equals(aptId));
                        if (exists) continue;

                        // זמנית: השתמש ב-fromUid ו-aptId כשמות
                        chat.setFromUserName(fromUid);
                        chat.setApartmentName(aptId);
                        tmp.add(chat);
                    }
                    chats.setValue(tmp);
                })
                .addOnFailureListener(e -> toast.setValue("שגיאה בטעינת צ'אטים: " + e.getMessage()));
    }

    public void filterChats(String query) {
        if (query == null) {
            loadChats();
            return;
        }

        String q = query.toLowerCase();
        List<Chat> current = chats.getValue() == null ? new ArrayList<>() : new ArrayList<>(chats.getValue());
        List<Chat> filtered = new ArrayList<>();
        for (Chat c : current) {
            String sender = c.getFromUserName() != null ? c.getFromUserName().toLowerCase() : "";
            String aptName = c.getApartmentName() != null ? c.getApartmentName().toLowerCase() : "";
            if (sender.contains(q) || aptName.contains(q)) {
                filtered.add(c);
            }
        }
        chats.setValue(filtered);
    }

    public String getCurrentUserId() {
        return uid();
    }

    public LiveData<String> getToastMessage() {
        return toast;
    }
}