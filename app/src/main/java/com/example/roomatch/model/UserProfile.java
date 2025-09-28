package com.example.roomatch.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private double lat=0, lng=0;

    private String profileImageUrl;


    @ServerTimestamp
    private Date createdAt;

    // קונסטרקטור ריק עבור Firebase
    public UserProfile() {}

    public UserProfile(String fullName, int age, String gender, String lifestyle, String interests, String userType,String city,String street,double lat, double lng,String description) {
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    @Exclude
    public List<String> getLifeStyleslist()
    {
        if(lifestyle!=null)
        {
            return Arrays.asList(this.lifestyle.split(","));
        }
        return new ArrayList<String>();
    }
    @Exclude
    public List<String> getInterestsList()
    {
        if(interests!=null)
        {
            return Arrays.asList(this.interests.split(","));
        }
        return new ArrayList<String>();
    }
    public void setInterests(String interests) { this.interests = interests; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getUserId() { return userId; } // הוספת getter
    public void setUserId(String userId) { this.userId = userId; } // הוספת setter
    public List<String> getContactIds() {
        return contactIds;
    }

    public String getSelectedCity(){return this.selectedCity;}
    public String getSelectedStreet(){return this.selectedStreet; }
    public LatLng getSelectedLocation(){try{return  new LatLng(this.lat,this.lng);}catch (Exception ex){return new LatLng(0,0);}}
    public void setLat(double lat) {this.lat=lat;}
    public void setLng(double lng) {this.lng=lng;}
    public String getDescription(){return this.description;}
    public void setDescription(String description){this.description=description;}
    public double getLat(){return this.lat;}
    public double getLng(){return this.lng;}

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date timeStamp)
    {
        this.createdAt=timeStamp;
    }

    public void setContactIds(List<String> contactIds) {
        this.contactIds = contactIds;
    }
}
