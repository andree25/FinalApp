package com.app.finalapp.ui.login;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;

public class LoginFragment extends BaseFragment {
    private static final String TAG = "LoginFragment";
    private FirebaseAuth mAuth;
    private AuthenticationManager authManager;
    private EditText emailLogin, passwordLogin;
    private AppCompatImageView togglePassword;
    private View rootView;
    private NavigationManager navigationManager;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "Fragment attached to activity");

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_login, container, false);

        authManager = new AuthenticationManager(); // Initialize AuthenticationManager
        mAuth = FirebaseAuth.getInstance();
        togglePassword = rootView.findViewById(R.id.togglePassword);
        emailLogin = rootView.findViewById(R.id.email_login);
        passwordLogin = rootView.findViewById(R.id.password_login);

        togglePassword.setOnClickListener(view -> {
            int inputType = passwordLogin.getInputType();
            int cursorPosition = passwordLogin.getSelectionStart();

            int newInputType = inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            passwordLogin.setInputType(newInputType);

            // Restore cursor position
            passwordLogin.setSelection(cursorPosition);
        });

        Button loginButton = rootView.findViewById(R.id.button_login);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                Log.d(TAG, "Login button clicked");
                if (isValidInputs()) {
                    loginUser(emailLogin.getText().toString(), passwordLogin.getText().toString());
                } else {
                    Toast.makeText(requireContext(), "Invalid email or password format", Toast.LENGTH_LONG).show();
                }
            });
        }

        Button registerButton = rootView.findViewById(R.id.button_register);
        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Log.d(TAG, "Register button clicked");
                Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_register);
            });
        }
        navigationManager = NavigationManager.getInstance();

        return rootView;
    }

    private boolean isValidInputs() {
        return authManager.isValidEmail(emailLogin.getText().toString()) && authManager.isValidPassword(passwordLogin.getText().toString());
    }

    private void loginUser(String email, String password) {
        if (!isNetworkConnected()) {
            Toast.makeText(requireContext(), "No network connection", Toast.LENGTH_LONG).show();
            return;
        }

        showLoadingIndicator();

        if (email.equals("admin@oscar.com") && password.equals("admin1")) {
            navigateToAdminArea();
            hideLoadingIndicator();
            return; // Stop further processing if it's the admin
        }
        authManager.loginUser(email, password, requireContext(), new AuthenticationManager.AuthCallback() {
            @Override
            public void onSuccess() {

                hideLoadingIndicator();
                FirebaseUser user = authManager.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "uuid " + user.getUid());
                    fetchUserData(user.getUid());
                    navigateBack();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                hideLoadingIndicator();
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToAdminArea() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_admin);
    }

    private void showLoadingIndicator() {
        // Show loading indicator
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        // Hide loading indicator
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.GONE);
    }

    private boolean isNetworkConnected() {
        // Check network connectivity
        // Implement your network connectivity check here
        return true; // Placeholder, replace with actual implementation
    }
}
