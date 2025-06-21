package com.example.roomatch.model;

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

    public void setContactIds(List<String> contactIds) {
        this.contactIds = contactIds;
    }
}