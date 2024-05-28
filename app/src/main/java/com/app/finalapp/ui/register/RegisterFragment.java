package com.app.finalapp.ui.register;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends BaseFragment {
    private AuthenticationManager authManager;
    private int targetFragmentId;

    private RegisterViewModel viewModel;
    private static final String USERS_NODE = "users";
    private EditText nameEditText, fornameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ImageView imageView;
    private NavigationManager navigationManager;

    private ActivityResultLauncher<String> launcher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_register, container, false);
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        authManager = new AuthenticationManager();
        Button pickButton = root.findViewById(R.id.pickimage);
        imageView = root.findViewById(R.id.imageView_picker);
        nameEditText = root.findViewById(R.id.registername);
        fornameEditText = root.findViewById(R.id.registerforename);
        emailEditText = root.findViewById(R.id.registeremail);
        passwordEditText = root.findViewById(R.id.registerpassword);
        confirmPasswordEditText = root.findViewById(R.id.registerconfirmpass);
        AppCompatImageView togglePasswordRegister = root.findViewById(R.id.togglePasswordRegister);
        AppCompatImageView togglePasswordRegisterC = root.findViewById(R.id.togglePasswordRegisterc);
        navigationManager = NavigationManager.getInstance();

        mAuth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            targetFragmentId = getArguments().getInt("targetFragmentId");
        }
        mDatabase = FirebaseDatabase.getInstance().getReference().child(USERS_NODE);

        togglePasswordRegister.setOnClickListener(view -> {
            int inputType = passwordEditText.getInputType();
            int cursorPosition = passwordEditText.getSelectionStart();

            int newInputType = inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            passwordEditText.setInputType(newInputType);

            passwordEditText.setSelection(cursorPosition);
        });
        togglePasswordRegisterC.setOnClickListener(view -> {
            int inputType = confirmPasswordEditText.getInputType();
            int cursorPosition = confirmPasswordEditText.getSelectionStart();

            int newInputType = inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            confirmPasswordEditText.setInputType(newInputType);

            confirmPasswordEditText.setSelection(cursorPosition);
        });
        // Initialize ActivityResultLauncher for image picking
        launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            if (result != null) {
                Glide.with(requireContext())
                        .load(result)
                        .centerInside() // Scale image to fit inside ImageView while maintaining aspect ratio
                        .into(imageView);
            }
        });

        // Set onClickListener for the pick image button
        if (pickButton != null) {
            pickButton.setOnClickListener(view -> {
                launcher.launch("image/*");
            });
        }

        // Set onClickListener for the register button
        Button registerButton = root.findViewById(R.id.registercreateprofile);
        registerButton.setOnClickListener(view -> registerUser());

        return root;
    }

    private void showProgressBar() {
        if (getView() != null) {
            ProgressBar progressBar = getView().findViewById(R.id.progressBar2);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideProgressBar() {
        if (getView() != null) {
            ProgressBar progressBar = getView().findViewById(R.id.progressBar2);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void registerUser() {
        // Get user input
        String name = nameEditText.getText().toString();
        String forname = fornameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validate inputs
        if (name.isEmpty() || forname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if an image is selected
        if (imageView.getDrawable() == null) {
            Toast.makeText(requireContext(), "Please pick an image", Toast.LENGTH_LONG).show();
            return;
        }

        // Show the progress bar
        showProgressBar();

        // Get the Uri of the selected image
        Uri imageUri = getImageUriFromImageView(imageView);
        authManager.registerUser(email, password, name, forname, imageUri, requireContext(), new AuthenticationManager.AuthCallback() {
            @Override
            public void onSuccess() {
                hideProgressBar();
                FirebaseUser user = authManager.getCurrentUser();
                if (user != null) {
                    Log.d("RegisterFragment", "uuid " + user.getUid());
                    fetchUserData(user.getUid());
                    updateUIAfterAuth(user.getUid());
                    if (targetFragmentId != 0) {
                        navController.navigate(targetFragmentId);
                    } else {
                        navController.navigate(R.id.nav_home); // Navigate to a default fragment if targetFragmentId is not set
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Hide the progress bar
                hideProgressBar();

                // Display registration failure message
                Toast.makeText(requireContext(), "Registration failed. " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Helper method to get the Uri of the selected image from an ImageView
    private Uri getImageUriFromImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            // Use MediaStore to create a temporary file and get its content URI
            return Uri.fromFile(saveBitmapToFile(bitmap));
        }
        return null;
    }
    private void updateUIAfterAuth(String userId) {
        fetchUserData(userId);
    }
    // Helper method to save a Bitmap to a temporary file
    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File file = File.createTempFile("temp_image", null, requireContext().getCacheDir());
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}