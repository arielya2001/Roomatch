package com.example.roomatch.model;

public class UserProfile {
    private String fullName;
    private int age;
    private String lifestyle;
    private String interests;
    private String userType; // חדש

    public UserProfile() {
        // נדרש לפיירבייס
    }

    public UserProfile(String fullName, int age, String lifestyle, String interests) {
        this.fullName = fullName;
        this.age = age;
        this.lifestyle = lifestyle;
        this.interests = interests;
        this.userType = "seeker"; // ברירת מחדל
    }

    public UserProfile(String fullName, int age, String lifestyle, String interests, String userType) {
        this.fullName = fullName;
        this.age = age;
        this.lifestyle = lifestyle;
        this.interests = interests;
        this.userType = userType;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getLifestyle() { return lifestyle; }
    public void setLifestyle(String lifestyle) { this.lifestyle = lifestyle; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
