// NavigationManager.java
package com.app.finalapp;

public class NavigationManager {
    private static NavigationManager instance;
    private Integer adoptionFragmentId;
    private Integer loginFragmentId;

    private NavigationManager() {
        // Private constructor to prevent instantiation
    }

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public Integer getAdoptionFragmentId() {
        return adoptionFragmentId;
    }

    public void setAdoptionFragmentId(Integer adoptionFragmentId) {
        this.adoptionFragmentId = adoptionFragmentId;
    }

    public Integer getLoginFragmentId() {
        return loginFragmentId;
    }

    public void setLoginFragmentId(Integer loginFragmentId) {
        this.loginFragmentId = loginFragmentId;
    }
}
