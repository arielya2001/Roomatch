package com.example.roomatch.viewmodel;

import static androidx.test.InstrumentationRegistry.getContext;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.ChatListItem;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.utils.ChatUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepo;
    private final UserRepository userRepo;

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    private final MutableLiveData<List<ChatListItem>> chats = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ChatListItem>> getChats() { return chats; }
    private List<ChatListItem> allChats = new ArrayList<>();


    public ChatViewModel(ChatRepository chatRepo, UserRepository userRepo) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
    }

    public LiveData<List<Message>> getMessages() { return messages; }
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

    public void filterChats(String query) {
        List<ChatListItem> filtered = new ArrayList<>();
        String lower = query.toLowerCase();
        for (ChatListItem item : allChats) {
            String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
            String sub = item.getSubText() != null ? item.getSubText().toLowerCase() : "";
            if (title.contains(lower) || sub.contains(lower)) {
                filtered.add(item);
            }
        }
        chats.setValue(filtered);
    }
    public String getCurrentUserId() {
        return uid();
    }


    public void loadChats() {
        String me = uid();
        if (me == null) {
            toast.setValue("שגיאה: משתמש לא מחובר");
            return;
        }

        Log.d("ChatVM", "loadChats(): טוען צ'אטים למשתמש " + me);
        Map<String, Chat> chatMap = new HashMap<>();

        // --- טען צ'אטים פרטיים ---
        chatRepo.getAllChatMessages()
                .addOnSuccessListener(snapshot -> {
                    Log.d("ChatVM", "התקבלו " + snapshot.size() + " הודעות פרטיות");

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Message msg = doc.toObject(Message.class);
                        if (msg == null) continue;

                        String from = msg.getFromUserId();
                        String to = msg.getToUserId();
                        String apt = msg.getApartmentId();

                        if (from == null || to == null || apt == null) continue;

                        boolean involved = me.equals(from) || me.equals(to);
                        if (!involved) continue;

                        String chatKey = ChatUtil.generateChatId(from, to, apt);
                        if (!chatMap.containsKey(chatKey)) {
                            Log.d("ChatVM", "נוסף צ'אט פרטי: " + chatKey);
                            Chat chat = new Chat();
                            chat.setFromUserId(!me.equals(from) ? from : to);
                            chat.setApartmentId(apt);
                            chat.setLastMessage(msg);
                            chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date(msg.getTimestamp())));
                            chat.setHasUnread(!msg.isRead() && msg.getToUserId().equals(me));
                            chat.setFromUserName(chat.getFromUserId());
                            chat.setApartmentName(apt);
                            chat.setType("private");
                            chatMap.put(chatKey, chat);
                        }
                    }

                    // --- טען צ'אטים קבוצתיים ---
                    chatRepo.getAllGroupChatsForUser(me)
                            .addOnSuccessListener(groupDocs -> {
                                Log.d("ChatVM", "התקבלו " + groupDocs.size() + " קבוצות");

                                List<String> groupIds = new ArrayList<>();
                                for (DocumentSnapshot doc : groupDocs) {
                                    String groupId = doc.getId();
                                    String aptId = doc.getString("apartmentId");
                                    String groupName = doc.getString("groupName");

                                    if (groupId == null || aptId == null) {
                                        Log.w("ChatVM", "קבוצת צ'אט עם נתונים חסרים - groupId/aptId חסרים");
                                        continue;
                                    }

                                    Log.d("ChatVM", "נמצאה קבוצה: " + groupId + " לדירה: " + aptId);

                                    Chat chat = new Chat();
                                    chat.setId(groupId);
                                    chat.setApartmentId(aptId);
                                    chat.setFromUserId(null);
                                    chat.setFromUserName(groupName != null ? groupName : "קבוצה");
                                    chat.setApartmentName(aptId);
                                    chat.setType("group");

                                    chatMap.put("group_" + groupId, chat);
                                    groupIds.add(groupId);
                                }

                                if (groupIds.isEmpty()) {
                                    Log.d("ChatVM", "אין קבוצות. מסיים טעינה");
                                    finishLoading(chatMap);
                                    return;
                                }

                                final int[] loaded = {0};
                                for (String groupId : groupIds) {
                                    chatRepo.getLastGroupMessage(groupId)
                                            .addOnSuccessListener(msgDoc -> {
                                                Message last = msgDoc.toObject(Message.class);
                                                if (last != null) {
                                                    Chat chat = chatMap.get("group_" + groupId);
                                                    if (chat != null) {
                                                        Log.d("ChatVM", "הודעה אחרונה לקבוצה " + groupId + ": " + last.getText());
                                                        chat.setLastMessage(last);
                                                        chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date(last.getTimestamp())));
                                                        chat.setHasUnread(!last.isRead() && !last.getFromUserId().equals(me));
                                                    }
                                                } else {
                                                    Log.w("ChatVM", "אין הודעה אחרונה לקבוצה " + groupId);
                                                }

                                                if (++loaded[0] == groupIds.size()) {
                                                    finishLoading(chatMap);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("ChatVM", "שגיאה בשליפת הודעה אחרונה לקבוצה " + groupId, e);
                                                if (++loaded[0] == groupIds.size()) {
                                                    finishLoading(chatMap);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ChatVM", "שגיאה בטעינת קבוצות", e);
                                finishLoading(chatMap);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatVM", "שגיאה בטעינת צ'אטים פרטיים", e);
                    toast.setValue("שגיאה בטעינת צ'אטים: " + e.getMessage());
                });
    }


    private void finishLoading(Map<String, Chat> chatMap) {
        List<ChatListItem> items = new ArrayList<>();
        for (Chat chat : chatMap.values()) {
            Log.d("ChatVM", "נוסף לרשימה סופית: " + chat.getType() + " | " +
                    (chat.getType().equals("group") ? chat.getId() : chat.getFromUserId()) +
                    " | apt: " + chat.getApartmentId());
            items.add(chat); // כי Chat מממש ChatListItem
        }

        // מיון לפי זמן (חדש קודם), כולל טיפול ב-null
        items.sort((a, b) -> {
            Long aTime = a.getTimestamp();
            Long bTime = b.getTimestamp();
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return Long.compare(bTime, aTime); // מיון יורד
        });

        chats.setValue(items);
        allChats = items;

    }



}