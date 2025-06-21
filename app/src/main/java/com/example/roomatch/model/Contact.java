package com.example.roomatch.model;

import java.io.Serializable;

public class Contact implements Serializable {
    private String userId; // מזהה המסמך ב-Firestore
    private String fromUserId; // השולח
    private String toUserId; // המקבל
    private String fullName; // שם המלא של השולח (או המקבל, תלוי בעיצוב)
    private String status; // e.g., "accepted", "pending"

    public Contact() {}

    public Contact(String userId, String fromUserId, String toUserId, String fullName, String status) {
        this.userId = userId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.fullName = fullName;
        this.status = status;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}