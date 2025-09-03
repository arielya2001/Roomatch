package com.example.roomatch.model;

import java.io.Serializable;

public class Apartment implements Serializable {
    private String id;
    private String ownerId;
    private String city;
    private String street;
    private int houseNumber;
    private int price;
    private int roommatesNeeded;
    private String description;
    private String imageUrl;

    private double latitude;
    private double longitude;

    private static double searchLatitude = 0.0;
    private static double searchLongitude = 0.0;

    private transient double distance;

    public Apartment() {}

    public Apartment(String id, String ownerId, String city, String street, int houseNumber,
                     int price, int roommatesNeeded, String description, String imageUrl,
                     double latitude, double longitude) {
        this.id = id;
        this.ownerId = ownerId;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.price = price;
        this.roommatesNeeded = roommatesNeeded;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Optional legacy constructor (for backward compatibility)
    public Apartment(String id, String ownerId, String city, String street, int houseNumber,
                     int price, int roommatesNeeded, String description, String imageUrl) {
        this(id, ownerId, city, street, houseNumber, price, roommatesNeeded, description, imageUrl, 0.0, 0.0);
    }

    public static void setSearchLocation(double latitude, double longitude) {
        searchLatitude = latitude;
        searchLongitude = longitude;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public int getHouseNumber() { return houseNumber; }
    public void setHouseNumber(int houseNumber) { this.houseNumber = houseNumber; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getRoommatesNeeded() { return roommatesNeeded; }
    public void setRoommatesNeeded(int roommatesNeeded) { this.roommatesNeeded = roommatesNeeded; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public void calculateDistanceFromSearchLocation() {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                searchLatitude, searchLongitude,
                this.latitude, this.longitude,
                results
        );
        this.distance = results[0]; // במטרים
    }

}
