package com.example.roomatch.viewmodel;

import static androidx.test.InstrumentationRegistry.getContext;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.utils.ChatUtil;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private List<Chat> allChats = new ArrayList<>();

    public void filterChats(String query) {
        List<Chat> filtered = new ArrayList<>();
        String lower = query.toLowerCase();

        for (Chat chat : allChats) {
            String user = chat.getFromUserName() != null ? chat.getFromUserName().toLowerCase() : "";
            String apt = chat.getApartmentName() != null ? chat.getApartmentName().toLowerCase() : "";
            if (user.contains(lower) || apt.contains(lower)) {
                filtered.add(chat);
            }
        }

        chats.setValue(filtered);
    }


    public void loadChats() {
        String me = uid();
        if (me == null) {
            toast.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        chatRepo.getAllChatMessages()  // ← תכף נוסיף את זה בריפוזיטורי
                .addOnSuccessListener(snapshot -> {
                    Map<String, Chat> chatMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Message msg = doc.toObject(Message.class);
                        if (msg == null) continue;

                        String from = msg.getFromUserId();
                        String to = msg.getToUserId();
                        String apt = msg.getApartmentId();

                        if (from == null || to == null || apt == null) continue;

                        boolean involved = me.equals(from) || me.equals(to);
                        if (!involved) continue;

                        // מפתח ייחודי לשיחה: לא משנה מי השולח
                        String chatKey = ChatUtil.generateChatId(from, to, apt);
                        if (!chatMap.containsKey(chatKey)) {
                            Chat chat = new Chat();
                            chat.setFromUserId(!me.equals(from) ? from : to);  // הצד השני!
                            chat.setApartmentId(apt);
                            chat.setLastMessage(msg);
                            chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date(msg.getTimestamp())));
                            chat.setHasUnread(!msg.isRead() && msg.getToUserId().equals(me));
                            chat.setFromUserName(chat.getFromUserId());
                            chat.setApartmentName(apt);
                            chatMap.put(chatKey, chat);
                        }
                    }

                    chats.setValue(new ArrayList<>(chatMap.values()));
                    allChats = new ArrayList<>(chatMap.values());  // ← הוספה חשובה!
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatsFragment", "Error loading chats", e); // חשוב – לא רק e.getMessage()
                    Toast.makeText(getContext(), "שגיאה בטעינת צ'אטים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });    }


    public String getCurrentUserId() {
        return uid();
    }

    public LiveData<String> getToastMessage() {
        return toast;
    }
}