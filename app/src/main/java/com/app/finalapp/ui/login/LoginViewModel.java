package com.app.finalapp.ui.login;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.AuthenticationManager;

public class LoginViewModel extends ViewModel {
    private final AuthenticationManager authManager;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    public LoginViewModel() {
        authManager = new AuthenticationManager();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsAdmin() {
        return isAdmin;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public boolean isValidEmail(String email) {
        return authManager.isValidEmail(email);
    }

    public boolean isValidPassword(String password) {
        return authManager.isValidPassword(password);
    }

    public void loginUser(String email, String password, Context context) {
        isLoading.setValue(true);
        if (email.equals("admin@oscar.com") && password.equals("admin1")) {
            isAdmin.setValue(true);
            isLoading.setValue(false);
            return;
        }
        authManager.loginUser(email, password, context, new AuthenticationManager.AuthCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                loginSuccess.setValue(true);
            }

            @Override
            public void onFailure(String errorMessage) {
                isLoading.setValue(false);
                LoginViewModel.this.errorMessage.setValue(errorMessage);
            }
        });
    }
}
