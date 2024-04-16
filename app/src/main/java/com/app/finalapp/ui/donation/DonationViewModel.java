package com.app.finalapp.ui.donation;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.AuthenticationManager;

public class DonationViewModel extends ViewModel {
    private MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>();
    private AuthenticationManager authenticationManager;

    public DonationViewModel() {
        authenticationManager = new AuthenticationManager();
        // Initialize isUserLoggedIn based on current user status
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
        Log.d("DonationViewModel", "Current user:" + authenticationManager.getCurrentUser());
    }

    public LiveData<Boolean> isUserLoggedIn() {
        return isUserLoggedIn;
    }

    public void checkUserLoggedIn() {
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
    }
}