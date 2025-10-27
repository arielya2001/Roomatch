package com.example.roomatch.model;

public class GroupChatListItem implements ChatListItem {
    private final GroupChat groupChat;
    private String lastMessageSenderName;

    private String addressStreet;
    private String addressHouseNumber;
    private String addressCity;

    private boolean hasUnread = false;
    private String participantsString;

    public GroupChatListItem(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    // === מזהים ===
    /** מזהה ה-thread: document id של הקבוצה (משמש לפתיחת הצ'אט וקריאת group_messages/{id}/chat) */
    public String getId() {
        return groupChat != null ? groupChat.getId() : null;
    }

    /** מזהה הקבוצה הלוגית (shared group id), לא ה-thread id */
    public String getGroupId() {
        return groupChat != null ? groupChat.getGroupId() : null;
    }

    public String getApartmentId() {
        return groupChat != null ? groupChat.getApartmentId() : null;
    }

    // === כתובת לתצוגה (ניתן להזין מבחוץ דרך ה־VM) ===
    public String getAddressStreet() { return addressStreet; }
    public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }

    public String getAddressHouseNumber() { return addressHouseNumber; }
    public void setAddressHouseNumber(String addressHouseNumber) { this.addressHouseNumber = addressHouseNumber; }

    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }

    // === הכותרות/טקסטים לרשימה ===
    public void setLastMessageSenderName(String lastMessageSenderName) {
        this.lastMessageSenderName = lastMessageSenderName;
    }

    public String getLastMessageSenderName() {
        return lastMessageSenderName != null ? lastMessageSenderName : "אנונימי";
    }

    @Override
    public long getTimestamp() {
        return groupChat != null ? groupChat.getLastMessageTimestamp() : 0L;
    }

    @Override
    public String getTitle() {
        // אם יש לך שדה name בקבוצה – עדיף להשתמש בו; אחרת אפשר לגזור משמות חברים/דיפולט
        return groupChat != null && groupChat.getGroupName() != null
                ? groupChat.getGroupName()
                : "צ'אט קבוצתי";
    }

    @Override
    public String getSubText() {
        if (addressStreet != null && addressHouseNumber != null && addressCity != null) {
            return addressStreet + " " + addressHouseNumber + ", " + addressCity;
        } else {
            // נפילה רכה אם לא הוזנו שדות כתובת בדוקומנט ה-thread
            return "דירה: " + getApartmentId();
        }
    }

    @Override
    public String getLastMessage() {
        return (groupChat != null && groupChat.getLastMessage() != null)
                ? groupChat.getLastMessage()
                : "אין הודעות";
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    // === משתתפים (טקסט מרוכז לתצוגה) ===
    @Override
    public String getParticipantsString() {
        return participantsString;
    }

    @Override
    public void setParticipantsString(String names) {
        this.participantsString = names;
    }

    // === מצב "לא נקרא" ===
    public boolean isHasUnread() {
        return hasUnread;
    }

    public void setHasUnread(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }
}
