package com.example.roomatch.model;

public class Apartment {
    private String address;
    private int price;
    private String description;
    private int roommatesNeeded;
    private String ownerId;
    private String imageUrl;

    private String id;


    // Constructor ריק – נדרש על ידי Firebase
    public Apartment() {}

    // Constructor מלא
    public Apartment(String address, int price, String description,
                     int roommatesNeeded, String ownerId, String imageUrl) {
        this.address = address;
        this.price = price;
        this.description = description;
        this.roommatesNeeded = roommatesNeeded;
        this.ownerId = ownerId;
        this.imageUrl = imageUrl;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    // Getters
    public String getAddress() {
        return address;
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

    public String getOwnerId() {
        return ownerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setters
    public void setAddress(String address) {
        this.address = address;
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

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
