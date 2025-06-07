package com.example.roomatch.model;

// בתוך package model
public class UserProfile {
    private String fullName;
    private int age;
    private String lifestyle;
    private String interests;

    public UserProfile(String fullName, int age, String lifestyle, String interests) {
        this.fullName = fullName;
        this.age = age;
        this.lifestyle = lifestyle;
        this.interests = interests;
    }

    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getLifestyle() { return lifestyle; }
    public String getInterests() { return interests; }
}
