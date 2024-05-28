package com.app.finalapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>();
    private MutableLiveData<FirebaseUser> currentUserLiveData = new MutableLiveData<>();

    public AuthenticationManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        // Initialize isUserLoggedIn based on current user status
        isUserLoggedIn.setValue(getCurrentUser() != null);
        currentUserLiveData.setValue(getCurrentUser());

    }
    public LiveData<Boolean> isUserLoggedIn() {
        return isUserLoggedIn;
    }
    public LiveData<FirebaseUser> getCurrentUserLiveData() {
        return currentUserLiveData;
    }
    public void checkUserLoggedIn() {
        isUserLoggedIn.setValue(getCurrentUser() != null);
    }
    public void registerUser(@NonNull String email, @NonNull String password, @NonNull String name, @NonNull String forname, @NonNull Uri imageUri, @NonNull Context context, AuthCallback callback) {
        if (isValidEmail(email) && isValidPassword(password)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserDataToDatabase(user.getUid(), email, name, forname, password, imageUri, context);
                            }
                            callback.onSuccess();
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    });
        } else {
            Toast.makeText(context, "Invalid email or password format", Toast.LENGTH_LONG).show();
            callback.onFailure("Invalid email or password format");
        }
    }

    public void loginUser(@NonNull String email, @NonNull String password, @NonNull Context context, AuthCallback callback) {
        if (isValidEmail(email) && isValidPassword(password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    });
        } else {
            Toast.makeText(context, "Invalid email or password format", Toast.LENGTH_LONG).show();
            callback.onFailure("Invalid email or password format");
        }
    }

    public void logoutUser() {
        mAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    private void saveUserDataToDatabase(String userId, String email, String name, String forname, String password, Uri imageUri, Context context) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("forname", forname);
        userMap.put("email", email);
        userMap.put("password", password);
        userMap.put("imageUrl", ""); // Initialize with an empty string

        // Save user data to Firebase Database (without the image URL)
        mDatabase.child(userId).setValue(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Data saved successfully
                        // Now, upload the image to Firebase Storage
                        uploadImageToStorage(userId, imageUri, context);
                    } else {
                        // If saving data fails, display a message to the user.
                        Toast.makeText(context, "Failed to save user data. " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadImageToStorage(String userId, Uri imageUri, Context context) {
        // Get a reference to the Firebase Storage location where the image will be stored
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("user_images").child(userId);

        // Upload the image to Firebase Storage
        storageRef.putFile(imageUri)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Image uploaded successfully, now get the download URL
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    // Update the user's data in the Realtime Database with the image URL
                                    updateImageUrlInDatabase(userId, uri.toString(), context);
                                })
                                .addOnFailureListener(e -> {
                                    // Handle the case where getting the image URL fails
                                    Toast.makeText(context, "Failed to get image URL. " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Handle the case where image upload fails
                        Toast.makeText(context, "Failed to upload image. " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateImageUrlInDatabase(String userId, String imageUrl, Context context) {
        // Update the user's data in the Realtime Database with the image URL
        mDatabase.child(userId).child("imageUrl").setValue(imageUrl)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Image URL saved successfully
                        Log.d("AuthenticationManager", "Image URL saved successfully");
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_LONG).show();
                    } else {
                        // If saving image URL fails, display a message to the user.
                        Toast.makeText(context, "Failed to save image URL. " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isValidPassword(String password) {
        return password.length() >= 6;
    }
}
