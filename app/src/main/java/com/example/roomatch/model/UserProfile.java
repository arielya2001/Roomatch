package com.example.roomatch.model;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private String fullName;
    private int age;
    private String gender;
    private String lifestyle;
    private String interests;
    private String userType;

    // קונסטרקטור ריק עבור Firebase
    public UserProfile() {}

    public UserProfile(String fullName, int age, String gender, String lifestyle, String interests, String userType) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.lifestyle = lifestyle;
        this.interests = interests;
        this.userType = userType;
    }

    // Getters ו-Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}