package com.example.roomatch.model;

import com.google.firebase.Timestamp;

public class Message {
    private String id;
    private String fromUserId;

    private String senderName; // <== הוסף את זה

    private String toUserId;
    private String text;
    private String apartmentId;
    private long timestamp;
    private boolean read;
    private String imageUrl;

    private String addressStreet;
    private String addressCity;
    private String addressHouseNumber;

    public String getAddressStreet() {
        return addressStreet;
    }
    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressCity() {
        return addressCity;
    }
    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressHouseNumber() {
        return addressHouseNumber;
    }
    public void setAddressHouseNumber(String addressHouseNumber) {
        this.addressHouseNumber = addressHouseNumber;
    }




    // קונסטרקטור ריק עבור Firebase
    public Message() {}

    public Message(String fromUserId, String toUserId, String text, String apartmentId, long timestamp) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.text = text;
        this.apartmentId = apartmentId;
        this.timestamp = timestamp;
    }

    // Getters ו-Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}