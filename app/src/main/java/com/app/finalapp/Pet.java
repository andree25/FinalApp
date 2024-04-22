package com.app.finalapp;

import java.util.List;

public class Pet {
    private String type;
    private String age;
    private String gender;
    private String description;
    private List<String> imageUrls;

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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
