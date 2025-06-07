package com.example.roomatch.model;

public class Apartment {
    private String location;
    private int price;
    private String description;
    private int roommatesNeeded;
    private String entryDate;
    private String ownerId;
    private String imageUrl;

    // Constructor ריק – נדרש על ידי Firebase
    public Apartment() {}

    // Constructor מלא
    public Apartment(String location, int price, String description,
                     int roommatesNeeded, String entryDate,
                     String ownerId, String imageUrl) {
        this.location = location;
        this.price = price;
        this.description = description;
        this.roommatesNeeded = roommatesNeeded;
        this.entryDate = entryDate;
        this.ownerId = ownerId;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getLocation() {
        return location;
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getRoommatesNeeded() {
        return roommatesNeeded;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setters
    public void setLocation(String location) {
        this.location = location;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRoommatesNeeded(int roommatesNeeded) {
        this.roommatesNeeded = roommatesNeeded;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
