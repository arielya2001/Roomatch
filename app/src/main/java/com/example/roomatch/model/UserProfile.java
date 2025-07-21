package com.example.roomatch.model;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.List;

public class UserProfile implements Serializable {
    private String fullName;
    private Integer age;
    private String gender;
    private String lifestyle;
    private String interests;
    private String userType;
    private String userId; // הוספה של userId
    private List<String> contactIds;
    private String selectedCity;
    private String selectedStreet;
    private String description;
    private double lat, lng;

    // קונסטרקטור ריק עבור Firebase
    public UserProfile() {}

    public UserProfile(String fullName, int age, String gender, String lifestyle, String interests, String userType,String city,String street,Double lat, double lng,String description) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.lifestyle = lifestyle;
        this.interests = interests;
        this.userType = userType;
        this.selectedCity=city;
        this.selectedStreet=street;
        this.lat=lat;
        this.lng=lng;
        this.description=description;
    }

    // Getters ו-Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getUserId() { return userId; } // הוספת getter
    public void setUserId(String userId) { this.userId = userId; } // הוספת setter
    public List<String> getContactIds() {
        return contactIds;
    }

    public String getSelectedCity(){return this.selectedCity;}
    public String getSelectedStreet(){return this.selectedCity; }
    public LatLng getSelectedLocation(){return  new LatLng(this.lat,this.lng);}

    public String getDescription(){return this.description;}
    public void setDescription(String description){this.description=description;}


    public void setContactIds(List<String> contactIds) {
        this.contactIds = contactIds;
    }
}