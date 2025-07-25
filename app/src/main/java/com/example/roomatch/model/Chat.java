package com.example.roomatch.model;

import com.google.firebase.Timestamp;

public class Chat implements ChatListItem {
    private String id;
    private String fromUserId;
    private String apartmentId;
    private Message lastMessage;
    private Timestamp timestamp;
    private boolean hasUnread;
    private String fromUserName;
    private String apartmentName;

    private String toUserId;

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }


    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }


    public Chat() {}

    public Chat(String fromUserId, String toUserId, String apartmentId, Message lastMessage, Timestamp timestamp,
                boolean hasUnread, String fromUserName, String apartmentName) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.apartmentId = apartmentId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.hasUnread = hasUnread;
        this.fromUserName = fromUserName;
        this.apartmentName = apartmentName;
    }


    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public Message getLastMessageObj() { return lastMessage; }  // לא מתנגש עם הממשק
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getTimestampObj() { return timestamp; }  // גם שונה מהשיטה של הממשק
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isHasUnread() { return hasUnread; }
    public void setHasUnread(boolean hasUnread) { this.hasUnread = hasUnread; }
    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }
    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }

    private String participantsString;

    @Override
    public String getParticipantsString() {
        return participantsString;
    }

    @Override
    public void setParticipantsString(String names) {
        this.participantsString = names;
    }


    @Override
    public String getLastMessageSenderName() {
        if (lastMessage != null && lastMessage.getSenderName() != null) {
            return lastMessage.getSenderName();
        } else if (fromUserName != null) {
            return fromUserName;
        } else {
            return fromUserId; // fallback
        }
    }



    // 🧩 מימוש ChatListItem:
    @Override
    public long getTimestamp() {
        return timestamp != null ? timestamp.toDate().getTime() : 0;
    }

    @Override
    public String getTitle() {
        return fromUserName != null ? fromUserName : fromUserId;
    }

    @Override
    public String getSubText() {
        return "דירה: " + (apartmentName != null ? apartmentName : apartmentId);
    }

    @Override
    public String getLastMessage() {
        return lastMessage != null ? lastMessage.getText() : "אין הודעות";
    }

    @Override
    public boolean isGroup() {
        return false;
    }
}
