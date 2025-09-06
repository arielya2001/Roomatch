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
import com.example.roomatch.model.GroupChat;
import com.example.roomatch.model.GroupChatListItem;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.utils.ChatUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

    private final ApartmentRepository apartmentRepo;


    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    private final MutableLiveData<List<ChatListItem>> chats = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<ChatListItem>> getChats() {
        return chats;
    }

    private List<ChatListItem> allChats = new ArrayList<>();


    public ChatViewModel(UserRepository userRepo, ChatRepository chatRepo, ApartmentRepository apartmentRepo) {
        this.userRepo = userRepo;
        this.chatRepo = chatRepo;
        this.apartmentRepo = apartmentRepo;
    }


    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<String> getToast() {
        return toast;
    }

    private String uid() {
        return userRepo.getCurrentUserId();
    }

    public void sendMessage(String chatId, String toUserId, String apartmentId, String text) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        // טען את פרטי הדירה לפי apartmentId
        apartmentRepo.getApartmentById(apartmentId).addOnSuccessListener(apartment -> {
            if (apartment == null) {
                toast.setValue("שגיאה: לא נמצאה דירה");
                return;
            }

            Message message = new Message(fromUid, toUserId, text, apartmentId, System.currentTimeMillis());
            message.setSenderName(userRepo.getCurrentUserName());

            // 👇 הוסף כתובת מהדירה להודעה
            message.setAddressStreet(apartment.getStreet());
            message.setAddressHouseNumber(String.valueOf(apartment.getHouseNumber()));
            message.setAddressCity(apartment.getCity());

            // שלח את ההודעה
            chatRepo.sendMessage(chatId, message)
                    .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                    .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
        }).addOnFailureListener(e -> toast.setValue("שגיאה בטעינת הדירה: " + e.getMessage()));
    }


    public void sendMessageWithImage(String chatId, String toUserId, String apartmentId, String text, Uri imageUri) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        Message message = new Message(fromUid, toUserId, text, apartmentId, System.currentTimeMillis());
        message.setSenderName(userRepo.getCurrentUserName()); // ✅ הוסף שורה זו
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
                            chat.setToUserId(me.equals(from) ? to : from); // 💡 זה הצד השני
                            chat.setApartmentId(apt);
                            chat.setLastMessage(msg);
                            chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date(msg.getTimestamp())));
                            chat.setHasUnread(!msg.isRead() && msg.getToUserId().equals(me));
                            chat.setFromUserName(chat.getFromUserId());
                            chat.setApartmentName(apt);
                            chat.setAddressStreet(msg.getAddressStreet());
                            chat.setAddressHouseNumber(msg.getAddressHouseNumber());
                            chat.setAddressCity(msg.getAddressCity());
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
                                                        List<String> readBy = last.getReadBy();
                                                        boolean read = readBy != null && readBy.contains(me);
                                                        chat.setHasUnread(!read && !last.getFromUserId().equals(me));
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
        Log.d("ChatVM", "finishLoading: כמות בצ'אט מאפ: " + chatMap.size());

        Map<String, String> userNameCache = new HashMap<>();
        List<Task<Void>> pendingNameTasks = new ArrayList<>();
        String currentUserId = userRepo.getCurrentUserId();

        Task<Void> myNameTask = userRepo.getUserNameById(currentUserId)
                .addOnSuccessListener(name -> {
                    userNameCache.put(currentUserId, name);
                }).continueWith(task -> null);
        pendingNameTasks.add(myNameTask);


        for (Chat chat : chatMap.values()) {
            String type = chat.getType();
            Log.d("ChatVM", "🔄 סוג: " + type + " | מזהה: " + chat.getId() + " | דירה: " + chat.getApartmentId());

            if ("group".equals(type)) {
                // ⚙ יצירת אובייקט קבוצתי
                GroupChat groupChat = new GroupChat();
                groupChat.setId(chat.getId());
                groupChat.setGroupId(chat.getId());
                groupChat.setApartmentId(chat.getApartmentId());
                groupChat.setGroupName(chat.getTitle());

                GroupChatListItem groupItem = new GroupChatListItem(groupChat);

// טען כתובת הדירה מה-Repository והכנס לפריט
                apartmentRepo.getApartmentDetails(chat.getApartmentId())
                        .addOnSuccessListener(apartment -> {
                            if (apartment != null) {
                                groupItem.setAddressStreet(apartment.getStreet());
                                groupItem.setAddressHouseNumber(String.valueOf(apartment.getHouseNumber()));
                                groupItem.setAddressCity(apartment.getCity());

                                // נוסיף גם את בעל הדירה לשמות המשתתפים
                                String ownerId = apartment.getOwnerId();

                                chatRepo.getGroupChatById(chat.getId())
                                        .addOnSuccessListener(doc -> {
                                            List<String> memberIds = (List<String>) doc.get("memberIds");
                                            if (memberIds == null) memberIds = new ArrayList<>();

                                            if (ownerId != null && !memberIds.contains(ownerId)) {
                                                memberIds.add(ownerId);
                                            }
                                            if (!memberIds.contains(currentUserId)) {
                                                memberIds.add(currentUserId);
                                            }

                                            List<String> participantNames = new ArrayList<>();
                                            List<Task<Void>> nameTasks = new ArrayList<>();
                                            for (String memberId : memberIds) {
                                                if (userNameCache.containsKey(memberId)) {
                                                    participantNames.add(userNameCache.get(memberId));
                                                } else {
                                                    Task<Void> t = userRepo.getUserNameById(memberId)
                                                            .addOnSuccessListener(name -> {
                                                                userNameCache.put(memberId, name);
                                                                participantNames.add(name);
                                                            }).continueWith(task -> null);
                                                    nameTasks.add(t);
                                                }
                                            }

                                            Tasks.whenAllComplete(nameTasks).addOnSuccessListener(v -> {
                                                groupItem.setParticipantsString(String.join(", ", participantNames));
                                                chats.setValue(new ArrayList<>(allChats));
                                            });
                                        });
                            }
                        });



                groupItem.setAddressStreet(chat.getAddressStreet());
                groupItem.setAddressHouseNumber(chat.getAddressHouseNumber());
                groupItem.setAddressCity(chat.getAddressCity());

                Message last = chat.getLastMessageObj();
                if (last != null) {
                    groupChat.setLastMessage(last.getText());
                    groupChat.setLastMessageTimestamp(chat.getTimestamp());
                    groupChat.setLastMessageObject(last);

                    String senderId = last.getFromUserId();
                    if (senderId != null) {
                        if (last.getSenderName() != null) {
                            groupItem.setLastMessageSenderName(last.getSenderName());
                        } else if (userNameCache.containsKey(senderId)) {
                            String name = userNameCache.get(senderId);
                            last.setSenderName(name);
                            groupItem.setLastMessageSenderName(name);
                        } else {
                            groupItem.setLastMessageSenderName("אנונימי");
                            Task<Void> t = userRepo.getUserNameById(senderId)
                                    .addOnSuccessListener(name -> {
                                        userNameCache.put(senderId, name);
                                        last.setSenderName(name);
                                        groupItem.setLastMessageSenderName(name);
                                    }).continueWith(task -> null);
                            pendingNameTasks.add(t);
                        }
                    }
                    groupItem.setHasUnread(chat.isHasUnread());
                }

                Log.d("ChatVM", "✅ נוסף: GroupChatListItem | מאת: " + groupItem.getLastMessageSenderName()
                        + " | תוכן: " + groupChat.getLastMessage());
                items.add(groupItem);
            } else {
                // צ'אט פרטי
                Message last = chat.getLastMessageObj();
                if (last != null) {
                    String senderId = last.getFromUserId();
                    if (senderId != null) {
                        if (last.getSenderName() == null && !userNameCache.containsKey(senderId)) {
                            Task<Void> t = userRepo.getUserNameById(senderId)
                                    .addOnSuccessListener(name -> {
                                        userNameCache.put(senderId, name);
                                        last.setSenderName(name);
                                        chat.setFromUserName(name);
                                    }).continueWith(task -> null);
                            pendingNameTasks.add(t);
                        } else if (userNameCache.containsKey(senderId)) {
                            last.setSenderName(userNameCache.get(senderId));
                            chat.setFromUserName(userNameCache.get(senderId));
                        }
                    }
                }

                String from = chat.getFromUserId();
                String to = chat.getToUserId();
                if (from == null || to == null) {
                    Log.w("ChatVM", "❗ מזהים חסרים בצ'אט פרטי – דילוג");
                    continue;
                }

                String otherId = currentUserId.equals(from) ? to : from;
                if (userNameCache.containsKey(otherId)) {
                    String myName = userNameCache.getOrDefault(currentUserId, "אני");
                    String otherName = userNameCache.getOrDefault(otherId, "אנונימי");

                    chat.setParticipantsString(myName + ", " + otherName);

                } else {
                    Task<Void> t1 = null;
                    if (!userNameCache.containsKey(currentUserId)) {
                        t1 = userRepo.getUserNameById(currentUserId)
                                .addOnSuccessListener(name -> userNameCache.put(currentUserId, name))
                                .continueWith(task -> null);
                        pendingNameTasks.add(t1);
                    }

                    Task<Void> t2 = userRepo.getUserNameById(otherId)
                            .addOnSuccessListener(name -> {
                                userNameCache.put(otherId, name);
                                String myNameFinal = userNameCache.getOrDefault(currentUserId, "אני");
                                chat.setParticipantsString(myNameFinal + ", " + name);
                                chats.setValue(new ArrayList<>(allChats));
                            }).continueWith(task -> null);
                    pendingNameTasks.add(t2);

                }

                Log.d("ChatVM", "✅ נוסף: Chat רגיל | מאת: " + chat.getFromUserName()
                        + " | תוכן: " + (last != null ? last.getText() : "אין הודעות")
                        + " | משתתף: " + otherId);

                items.add(chat);
            }
        }

        Log.d("ChatVM", "📊 לפני מיון: " + items.size() + " פריטים");

        if (!pendingNameTasks.isEmpty()) {
            Tasks.whenAllComplete(pendingNameTasks)
                    .addOnSuccessListener(results -> {
                        Log.d("ChatVM", "🎉 כל השמות נטענו. ממיין...");
                        Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        chats.setValue(items);
                        allChats = items;
                        Log.d("ChatVM", "🎯 finishLoading: הסתיים לאחר טעינת שמות.");
                    });
        } else {
            Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            chats.setValue(items);
            allChats = items;
            Log.d("ChatVM", "🎯 finishLoading: הסתיים ללא טעינת שמות.");
        }
    }

}









