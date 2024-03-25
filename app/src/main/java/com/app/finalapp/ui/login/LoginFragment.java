package com.app.finalapp.ui.login;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class LoginFragment extends Fragment {
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

        authManager.loginUser(email, password, requireContext(), new AuthenticationManager.AuthCallback() {
            @Override
            public void onSuccess() {
                hideLoadingIndicator();
                FirebaseUser user = authManager.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "uuid "+user.getUid());
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

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        if (navigationManager.getAdoptionFragmentId() != null) {
            navController.navigate(navigationManager.getAdoptionFragmentId());
            navigationManager.setAdoptionFragmentId(null);
        }
    }

    private void fetchUserData(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                    Log.d(TAG, "fetchUserData: Name: " + name + ", Email: " + email + ", ImageUrl: " + imageUrl);

                    // Update UI with user data
                    updateUI(name, email, imageUrl);
                } else {
                    Log.d(TAG, "fetchUserData: User data does not exist for ID: " + userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch user data: " + error.getMessage());
            }
        });
    }


    // Method to update UI with user data
    private void updateUI(String name, String email, String imageUrl) {
        if (!isAdded()) {
            // Fragment is not attached to any activity, handle accordingly
            return;
        }

        NavigationView navigationView = requireActivity().findViewById(R.id.nav_view);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView navUsername = headerView.findViewById(R.id.textView_name_navigation_header);
            TextView navEmail = headerView.findViewById(R.id.textView_email_navigation_header);
            ImageView navImageView = headerView.findViewById(R.id.imageView_navigation_header);

            if (navUsername != null) {
                navUsername.setText(name);
            } else {
                Log.e(TAG, "navUsername is null");
            }

            if (navEmail != null) {
                navEmail.setText(email);
            } else {
                Log.e(TAG, "navEmail is null");
            }

            if (navImageView != null) {
                Glide.with(requireContext()).load(imageUrl).into(navImageView);
            } else {
                Log.e(TAG, "navImageView is null");
            }
        } else {
            Log.e(TAG, "NavigationView is null");
        }
    }


    private boolean isNetworkConnected() {
        // Check network connectivity
        // Implement your network connectivity check here
        return true; // Placeholder, replace with actual implementation
    }
}
