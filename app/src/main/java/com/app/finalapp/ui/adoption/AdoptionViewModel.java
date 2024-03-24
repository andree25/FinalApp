package com.app.finalapp.ui.adoption;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.AuthenticationManager;

public class AdoptionViewModel extends ViewModel {
    private MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>();
    private AuthenticationManager authenticationManager;

    public AdoptionViewModel() {
        authenticationManager = new AuthenticationManager();
        // Initialize isUserLoggedIn based on current user status
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
    }

    public LiveData<Boolean> isUserLoggedIn() {
        return isUserLoggedIn;
    }

    public void checkUserLoggedIn() {
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
    }
}
