package com.example.roomatch.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roomatch.model.Chat;
import com.example.roomatch.model.ChatListItem;
import com.example.roomatch.model.GroupChat;
import com.example.roomatch.model.GroupChatListItem;
import com.example.roomatch.model.Message;
import com.example.roomatch.model.UserProfile;
import com.example.roomatch.model.repository.ApartmentRepository;
import com.example.roomatch.model.repository.ChatRepository;
import com.example.roomatch.model.repository.UserRepository;
import com.example.roomatch.utils.ChatUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * VM:
 * - מסך רשימת צ'אטים: נטען מ-userChats/{me}/threads עם פגינציה
 * - מסך צ'אט ספציפי: נטען הודעות בפגינציה (יש לך כבר fetchAccumulated)
 */
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final ApartmentRepository apartmentRepo;

    // הודעות של צ'אט ספציפי (פגינציה קיימת)
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    // רשימת צ'אטים (threads) לפיד הראשי
    private final MutableLiveData<List<ChatListItem>> chats = new MutableLiveData<>(new ArrayList<>());
    private List<ChatListItem> allChats = new ArrayList<>();

    public LiveData<List<ChatListItem>> getChats() { return chats; }
    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<String> getToast() { return toast; }

    // פגינציית הודעות בצ'אט
    private DocumentSnapshot lastDoc = null;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private static final int PAGE_SIZE = 10;

    // פגינציית threads ברשימת צ'אטים
    private DocumentSnapshot lastThreadsDoc = null;
    private boolean threadsLoading = false;
    private boolean threadsHasMore = true;
    private static final int THREADS_PAGE_SIZE = 20;

    public ChatViewModel(UserRepository userRepo, ChatRepository chatRepo, ApartmentRepository apartmentRepo) {
        this.userRepo = userRepo;
        this.chatRepo = chatRepo;
        this.apartmentRepo = apartmentRepo;
    }

    private String uid() { return userRepo.getCurrentUserId(); }

    // ==============================
    // ===== רשימת צ'אטים (threads)
    // ==============================

    public void loadChatsFirstPage() {
        if (threadsLoading) return;
        threadsLoading = true;
        threadsHasMore = true;
        lastThreadsDoc = null;

        String me = uid();
        if (me == null) {
            toast.setValue("שגיאה: משתמש לא מחובר");
            threadsLoading = false;
            return;
        }

        chatRepo.getUserChatThreadsPage(me, THREADS_PAGE_SIZE, null)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ChatListItem> items = mapThreadsSnapshotToItems(snap);
                    if (snap.size() > 0) lastThreadsDoc = snap.getDocuments().get(snap.size() - 1);
                    threadsHasMore = (snap.size() == THREADS_PAGE_SIZE);

                    // מיין לפי זמן אחרון (יורד)
                    items.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    allChats = items;
                    chats.setValue(new ArrayList<>(items));
                    threadsLoading = false;

                    Log.d("ChatVM", "loadChatsFirstPage: loaded " + items.size());
                })
                .addOnFailureListener(e -> {
                    threadsLoading = false;
                    toast.setValue("שגיאה בטעינת צ'אטים: " + e.getMessage());
                });
    }

    public void loadChatsNextPage() {
        if (threadsLoading || !threadsHasMore) return;
        threadsLoading = true;

        String me = uid();
        if (me == null) { threadsLoading = false; return; }

        chatRepo.getUserChatThreadsPage(me, THREADS_PAGE_SIZE, lastThreadsDoc)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ChatListItem> newItems = mapThreadsSnapshotToItems(snap);
                    if (snap.size() > 0) lastThreadsDoc = snap.getDocuments().get(snap.size() - 1);
                    threadsHasMore = (snap.size() == THREADS_PAGE_SIZE);

                    List<ChatListItem> merged = new ArrayList<>(allChats);
                    merged.addAll(newItems);
                    merged.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    allChats = merged;
                    chats.setValue(new ArrayList<>(merged));
                    threadsLoading = false;

                    Log.d("ChatVM", "loadChatsNextPage: appended " + newItems.size());
                })
                .addOnFailureListener(e -> {
                    threadsLoading = false;
                    toast.setValue("שגיאה בטעינת עמוד נוסף: " + e.getMessage());
                });
    }

    /** המרת מסמכי thread ל־ChatListItem/GroupChatListItem */
    private List<ChatListItem> mapThreadsSnapshotToItems(QuerySnapshot snap) {
        List<ChatListItem> items = new ArrayList<>();
        for (DocumentSnapshot d : snap) {
            // אם יש לך מודל ChatListItem תואם – אפשר: ChatListItem it = d.toObject(ChatListItem.class);
            // כאן אמיר ידני כי פעמים רבות השדות שונים מעט:
            String id = d.getString("id");           // chatId או groupId
            String type = d.getString("type");       // "private" | "group"
            String apartmentId = d.getString("apartmentId");
            String addressStreet = d.getString("addressStreet");
            String addressHouseNumber = d.getString("addressHouseNumber");
            String addressCity = d.getString("addressCity");
            String lastMessage = d.getString("lastMessage");
            String lastMessageSenderName = d.getString("lastMessageSenderName");
            Long ts = d.getLong("lastMessageTimestamp");
            Boolean hasUnread = d.getBoolean("hasUnread");

            long timestamp = (ts != null ? ts : 0L);

            if ("group".equals(type)) {
                GroupChat gc = new GroupChat();
                gc.setId(id);
                gc.setGroupId(id);
                gc.setApartmentId(apartmentId);
                gc.setLastMessage(lastMessage);
                if (ts != null) gc.setLastMessageTimestamp(ts);

                GroupChatListItem item = new GroupChatListItem(gc);
                item.setAddressStreet(addressStreet);
                item.setAddressHouseNumber(addressHouseNumber);
                item.setAddressCity(addressCity);
                item.setLastMessageSenderName(lastMessageSenderName);
                item.setHasUnread(hasUnread != null && hasUnread);

                items.add(item);
            } else {
                Chat chat = new Chat();
                chat.setId(id);
                chat.setApartmentId(apartmentId);
                chat.setAddressStreet(addressStreet);
                chat.setAddressHouseNumber(addressHouseNumber);
                chat.setAddressCity(addressCity);
                chat.setType("private");
                if (lastMessage != null) {
                    Message m = new Message();
                    m.setText(lastMessage);
                    m.setSenderName(lastMessageSenderName);
                    m.setTimestamp(timestamp);
                    chat.setLastMessage(m);
                    chat.setTimestamp(new Timestamp(new Date(timestamp)));
                }

                // אם צריך מאפיינים נוספים (from/to) – אפשר לאחזרם מ־participants בשדה נוסף

                // Chat יורש/מממש ChatListItem
                chat.setHasUnread(hasUnread != null && hasUnread);
                if (ts != null) chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date(ts)));
                items.add(chat);
            }
        }
        return items;
    }



    /** חיפוש ברשימה שכבר נטענה */
    public void filterChats(String query) {
        List<ChatListItem> filtered = new ArrayList<>();
        String lower = query == null ? "" : query.toLowerCase();
        for (ChatListItem item : allChats) {
            String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
            String sub = item.getSubText() != null ? item.getSubText().toLowerCase() : "";
            if (title.contains(lower) || sub.contains(lower)) {
                filtered.add(item);
            }
        }
        chats.setValue(filtered);
    }

    // =================================
    // ===== הודעות בצ'אט (Pagination)
    // =================================

    public boolean isLoading() { return isLoading; }
    public boolean hasMore() { return hasMore; }

    public void loadFirstPage(String chatId) {
        if (isLoading) return;
        isLoading = true;
        hasMore = true;
        lastDoc = null;
        messages.setValue(new ArrayList<>());
        fetchAccumulated(PAGE_SIZE, /*replace=*/true, new ArrayList<>(), chatId);
    }

    public void loadNextPage(String chatId) {
        if (isLoading || !hasMore) return;
        isLoading = true;
        fetchAccumulated(PAGE_SIZE, /*replace=*/false, new ArrayList<>(), chatId);
    }

    private void fetchAccumulated(final int wanted, final boolean replace, final List<Message> acc, String chatId) {
        Query q = chatRepo.buildQuery(chatId).limit(PAGE_SIZE);
        if (lastDoc != null) q = q.startAfter(lastDoc);

        q.get()
                .addOnSuccessListener(snap -> {
                    if (snap.size() > 0) lastDoc = snap.getDocuments().get(snap.size() - 1);

                    acc.addAll(filterBatch(snap));
                    boolean noMoreServerPages = (snap.size() < PAGE_SIZE);
                    boolean filledEnough = (acc.size() >= wanted);

                    if (!filledEnough && !noMoreServerPages) {
                        fetchAccumulated(wanted, replace, acc, chatId);
                        return;
                    }

                    if (replace) {
                        messages.setValue(new ArrayList<>(acc));
                    } else {
                        List<Message> cur = messages.getValue();
                        if (cur == null) cur = new ArrayList<>();
                        cur.addAll(acc);
                        messages.setValue(cur);
                    }

                    hasMore = !noMoreServerPages;
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    toast.setValue("שגיאה בטעינה: " + e.getMessage());
                });
    }

    private List<Message> filterBatch(QuerySnapshot snap) {
        List<Message> out = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Message m = doc.toObject(Message.class);
            if (m == null) continue;
            out.add(m);
        }
        return out;
    }

    // ===========================
    // ========= פעולות ==========
    // ===========================

    public String getCurrentUserId() { return uid(); }

    /** שליחה + עדכון thread (השדות של הכתובת מוזנים לפני השליחה ע"י קורא ה־VM) */
    public void sendMessage(String chatId, String toUserId, String apartmentId, String text) {
        String fromUid = uid();
        if (fromUid == null || text.trim().isEmpty()) {
            toast.setValue("שגיאה: משתמש לא מחובר או הודעה ריקה");
            return;
        }

        apartmentRepo.getApartmentById(apartmentId).addOnSuccessListener(apartment -> {
            if (apartment == null) {
                toast.setValue("שגיאה: לא נמצאה דירה");
                return;
            }

            Message message = new Message(fromUid, toUserId, text, apartmentId, System.currentTimeMillis());
            message.setSenderName(userRepo.getCurrentUserName());
            message.setAddressStreet(apartment.getStreet());
            message.setAddressHouseNumber(String.valueOf(apartment.getHouseNumber()));
            message.setAddressCity(apartment.getCity());

            chatRepo.sendMessage(chatId, message)
                    .addOnSuccessListener(r -> {
                        toast.setValue("הודעה נשלחה");
                        // רענון הודעות הצ'אט המקומי
                        loadFirstPage(chatId);
                        // רענון Thread list – אופציונלי: למשוך מחדש first page/לעדכן פריט בודד
                        // loadChatsFirstPage();
                    })
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
        message.setSenderName(userRepo.getCurrentUserName());

        chatRepo.sendMessageWithImage(chatId, message, imageUri)
                .addOnSuccessListener(r -> toast.setValue("הודעה נשלחה"))
                .addOnFailureListener(e -> toast.setValue("שגיאה: " + e.getMessage()));
    }

    public Query getChatMessagesQuery(String chatId, int limit) {
        return chatRepo.getPaginatedChatMessagesQuery(chatId, Math.max(limit, 20));
    }

    public void markMessagesAsRead(String chatId) {
        String me = uid();
        if (me == null) return;

        chatRepo.markMessagesAsRead(chatId, me)
                .addOnFailureListener(e -> toast.setValue("שגיאה בסימון כנקראו: " + e.getMessage()));
    }
}
