package com.example.roomatch.model;

import java.io.Serializable;
import java.util.List;
import java.util.Arrays;


public class Contact implements Serializable {
    private String userId;
    private String fromUserId;
    private String toUserId;
    private String fullName;
    private String status;

    // שדות חדשים
    private String lifestyle;
    private List<String> interests;

    public Contact() {}

    public Contact(String userId, String fromUserId, String toUserId, String fullName, String status,
                   String lifestyle, List<String> interests) {
        this.userId = userId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.fullName = fullName;
        this.status = status;
        this.lifestyle = lifestyle;
        this.interests = interests;
    }

    // getters & setters
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

    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }
}
