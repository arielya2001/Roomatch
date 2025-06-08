package com.example.roomatch.model;

public class Apartment {
    private String city;
    private String street;
    private int houseNumber;

    private int price;
    private String description;
    private int roommatesNeeded;
    private String ownerId;
    private String imageUrl;

    private String id;

    // קונסטרקטור ריק – נדרש על ידי Firebase
    public Apartment() {}

    // קונסטרקטור מלא
    public Apartment(String city, String street, int houseNumber, int price, String description,
                     int roommatesNeeded, String ownerId, String imageUrl) {
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.price = price;
        this.description = description;
        this.roommatesNeeded = roommatesNeeded;
        this.ownerId = ownerId;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public int getHouseNumber() {
        return houseNumber;
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

    public String getId() {
        return id;
    }

    // Setters
    public void setCity(String city) {
        this.city = city;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setHouseNumber(int houseNumber) {
        this.houseNumber = houseNumber;
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

    public void setId(String id) {
        this.id = id;
    }

    // אופציונלי – אם רוצים כתובת אחת כמחרוזת
    public String getFullAddress() {
        return "עיר: " + city + ", רחוב: " + street + ", מס' " + houseNumber;
    }
}
