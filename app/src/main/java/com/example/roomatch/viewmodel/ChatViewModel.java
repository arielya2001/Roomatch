package com.example.roomatch.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {

    /* ---------- Repositories ---------- */
    private final ChatRepository  chatRepo;
    private final UserRepository  userRepo;

    /* ---------- LiveData ---------- */
    private final MutableLiveData<List<Map<String, Object>>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Map<String, Object>>> chats    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String>                    toast    = new MutableLiveData<>();

    public ChatViewModel(ChatRepository chatRepo, UserRepository userRepo) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
    }

    /* getters */
    public LiveData<List<Map<String, Object>>> getMessages() { return messages; }
    public LiveData<List<Map<String, Object>>> getChats()    { return chats; }
    public LiveData<String>                    getToast()    { return toast; }

    private String uid() { return userRepo.getCurrentUserId(); }

    /* ---------- שליחת הודעות ---------- */
    public void sendMessage(String chatId, String toUserId, String apartmentId, String text) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        chatRepo.sendMessage(chatId, fromUid, toUserId, apartmentId, text)
                .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
    }

    public void sendMessageWithImage(String chatId, String toUserId,
                                     String apartmentId, String text, Uri imageUri) {

        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        chatRepo.sendMessageWithImage(chatId, fromUid, toUserId, apartmentId, text, imageUri)
                .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
    }

    /* ---------- זרם הודעות ---------- */
    public Query getChatMessagesQuery(String chatId, int limit) {
        return chatRepo.getPaginatedChatMessagesQuery(chatId, limit > 0 ? limit : 20);
    }

    public void markMessagesAsRead(String chatId) {
        String me = uid();
        if (me == null) return;

        chatRepo.markMessagesAsRead(chatId, me)
                .addOnFailureListener(e -> toast.setValue("שגיאה בסימון כנקראו: " + e.getMessage()));
    }

    /* ---------- רשימת צ'אטים ---------- */
    public void loadChats() {
        String me = uid();
        if (me == null) {
            toast.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        chatRepo.getChatsForUser(me)
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> tmp = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = doc.getData();

                        String fromUid  = (String) data.get("fromUserId");
                        String aptId    = (String) data.get("apartmentId");
                        String lastTxt  = (String) data.get("text");
                        Long   ts       = doc.getLong("timestamp");
                        Boolean readVal = doc.getBoolean("read");

                        if (fromUid == null || aptId == null || ts == null) continue;

                        /* ודא שאין כפילות */
                        boolean exists = tmp.stream()
                                .anyMatch(c -> c.get("fromUserId").equals(fromUid)
                                        && c.get("apartmentId").equals(aptId));
                        if (exists) continue;

                        Map<String, Object> chat = new HashMap<>();
                        chat.put("fromUserId",  fromUid);
                        chat.put("apartmentId", aptId);
                        chat.put("lastMessage", lastTxt);
                        chat.put("timestamp",   ts);
                        chat.put("hasUnread",   readVal != null ? !readVal : true);

                        /* ניתן לשפר אח”כ עם קריאה ל‑UserRepository / ApartmentRepository */
                        chat.put("fromUserName",  fromUid);
                        chat.put("apartmentName", aptId);

                        tmp.add(chat);
                    }
                    chats.setValue(tmp);
                })
                .addOnFailureListener(e -> toast.setValue("שגיאה בטעינת צ'אטים: " + e.getMessage()));
    }

    /* ---------- סינון ---------- */
    public void filterChats(String query) {
        if (query == null) { loadChats(); return; }

        String q = query.toLowerCase();
        List<Map<String, Object>> current = chats.getValue() == null
                ? new ArrayList<>()
                : new ArrayList<>(chats.getValue());

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> c : current) {
            String sender   = ((String) c.get("fromUserName")).toLowerCase();
            String aptName  = ((String) c.get("apartmentName")).toLowerCase();
            if (sender.contains(q) || aptName.contains(q)) filtered.add(c);
        }
        chats.setValue(filtered);
    }

    /** מאפשר לפרגמנט לקבל את UID של המשתמש המחובר. */
    public String getCurrentUserId() {          //  ← חדש
        return uid();
    }

    /** אותו ‎LiveData‎ – אבל בשם שה‑Fragment הישן כבר משתמש בו. */
    public LiveData<String> getToastMessage() { //  ← חדש
        return toast;
    }
}
