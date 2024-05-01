package com.app.finalapp;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.List;

public class Pet implements Serializable {
    private String type;
    private String age;
    private String gender;
    private String description;
    private List<String> imageUrls;
    private String userEmail; // Email will be fetched later
    private String userId; // Add this to store the UID

    private String uid;  // This should store the Firebase generated unique key
    // Other fields...

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Pet() {
        // Default constructor required for calls to DataSnapshot.getValue(Pet.class)
    }

    public Pet(String type, String age, String gender, String description, List<String> imageUrls) {
        this.type = type;
        this.age = age;
        this.gender = gender;
        this.description = description;
        this.imageUrls = imageUrls;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getImageUrls() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        return imageUrls;
    }
}
