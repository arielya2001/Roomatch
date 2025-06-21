package com.example.roomatch.model;

public class GroupChatMessage {

    private String senderUserId;     // מי בפועל כתב את ההודעה (יכול להיות כל חבר בקבוצה)
    private String senderGroupId;    // הקבוצה בשמה נשלחה ההודעה
    private String receiverId;       // בעל הדירה
    private String apartmentId;      // מזהה הדירה
    private String content;          // תוכן ההודעה
    private long timestamp;          // מתי נשלחה ההודעה

    private String senderId;
    private String senderName;


    // דרוש ל-Firebase
    public GroupChatMessage() {
    }

    public GroupChatMessage(String senderUserId, String senderGroupId, String receiverId,
                            String apartmentId, String content, long timestamp) {
        this.senderUserId = senderUserId;
        this.senderGroupId = senderGroupId;
        this.receiverId = receiverId;
        this.apartmentId = apartmentId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }


    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderGroupId() {
        return senderGroupId;
    }

    public void setSenderGroupId(String senderGroupId) {
        this.senderGroupId = senderGroupId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(String apartmentId) {
        this.apartmentId = apartmentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
