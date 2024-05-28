package com.app.finalapp.ui.register;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageView;

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
import android.widget.Toast;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RegisterFragment extends BaseFragment {

    private AuthenticationManager authManager;
    private EditText nameEditText, fornameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ImageView imageView;
    private int targetFragmentId;
    private ActivityResultLauncher<String> launcher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_register, container, false);

        initializeUI(root);
        initializeFirebase();
        setupPasswordToggle(root);
        setupImagePicker(root);
        setupRegisterButton(root);

        return root;
    }

    private void initializeUI(View root) {
        nameEditText = root.findViewById(R.id.registername);
        fornameEditText = root.findViewById(R.id.registerforename);
        emailEditText = root.findViewById(R.id.registeremail);
        passwordEditText = root.findViewById(R.id.registerpassword);
        confirmPasswordEditText = root.findViewById(R.id.registerconfirmpass);
        imageView = root.findViewById(R.id.imageView_picker);
    }

    private void initializeFirebase() {
        authManager = new AuthenticationManager();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        if (getArguments() != null) {
            targetFragmentId = getArguments().getInt("targetFragmentId");
        }
    }

    private void setupPasswordToggle(View root) {
        AppCompatImageView togglePasswordRegister = root.findViewById(R.id.togglePasswordRegister);
        AppCompatImageView togglePasswordRegisterC = root.findViewById(R.id.togglePasswordRegisterc);

        togglePasswordRegister.setOnClickListener(view -> togglePasswordVisibility(passwordEditText));
        togglePasswordRegisterC.setOnClickListener(view -> togglePasswordVisibility(confirmPasswordEditText));
    }

    private void togglePasswordVisibility(EditText passwordField) {
        int inputType = passwordField.getInputType();
        int cursorPosition = passwordField.getSelectionStart();

        int newInputType = (inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

        passwordField.setInputType(newInputType);
        passwordField.setSelection(cursorPosition);
    }

    private void setupImagePicker(View root) {
        Button pickButton = root.findViewById(R.id.pickimage);
        launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            if (result != null) {
                Glide.with(requireContext())
                        .load(result)
                        .centerInside()
                        .into(imageView);
            }
        });

        pickButton.setOnClickListener(view -> launcher.launch("image/*"));
    }

    private void setupRegisterButton(View root) {
        Button registerButton = root.findViewById(R.id.registercreateprofile);
        registerButton.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString();
        String forname = fornameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (!validateInputs(name, forname, email, password, confirmPassword)) return;

        showProgressBar();

        Uri imageUri = getImageUriFromImageView(imageView);
        authManager.registerUser(email, password, name, forname, imageUri, requireContext(), new AuthenticationManager.AuthCallback() {
            @Override
            public void onSuccess() {
                handleRegistrationSuccess();
            }

            @Override
            public void onFailure(String errorMessage) {
                handleRegistrationFailure(errorMessage);
            }
        });
    }

    private boolean validateInputs(String name, String forname, String email, String password, String confirmPassword) {
        if (name.isEmpty() || forname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_LONG).show();
            return false;
        }

        if (imageView.getDrawable() == null) {
            Toast.makeText(requireContext(), "Please pick an image", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void handleRegistrationSuccess() {
        hideProgressBar();
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            if (targetFragmentId != 0) {
                navController.navigate(targetFragmentId);
            } else {
                navController.navigate(R.id.nav_home);
            }
        }
    }

    private void handleRegistrationFailure(String errorMessage) {
        hideProgressBar();
        Toast.makeText(requireContext(), "Registration failed. " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private Uri getImageUriFromImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            return Uri.fromFile(saveBitmapToFile(bitmap));
        }
        return null;
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File file = File.createTempFile("temp_image", null, requireContext().getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showProgressBar() {
        ProgressBar progressBar = getView().findViewById(R.id.progressBar2);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        ProgressBar progressBar = getView().findViewById(R.id.progressBar2);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
