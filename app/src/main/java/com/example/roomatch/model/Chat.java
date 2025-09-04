package com.example.roomatch.model;

import com.google.firebase.Timestamp;

public class Chat {
    private String id;
    private String fromUserId;
    private String apartmentId;
    private Message lastMessage;
    private Timestamp timestamp;
    private boolean hasUnread;
    private String fromUserName;
    private String apartmentName;

    // קונסטרקטור ריק עבור Firebase
    public Chat() {}

    public Chat(String fromUserId, String apartmentId, Message lastMessage, Timestamp timestamp,
                boolean hasUnread, String fromUserName, String apartmentName) {
        this.fromUserId = fromUserId;
        this.apartmentId = apartmentId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.hasUnread = hasUnread;
        this.fromUserName = fromUserName;
        this.apartmentName = apartmentName;
    }

    // Getters ו-Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isHasUnread() { return hasUnread; }
    public void setHasUnread(boolean hasUnread) { this.hasUnread = hasUnread; }
    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }
    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }
}