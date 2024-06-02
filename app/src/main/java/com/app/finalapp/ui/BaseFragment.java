package com.app.finalapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

import android.util.Log;
import android.widget.Toast;

public abstract class BaseFragment extends Fragment {
    protected AuthenticationManager authManager;
    protected NavController navController;
    protected NavigationManager navigationManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        authManager = new AuthenticationManager();
        navigationManager = NavigationManager.getInstance();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        authManager.getCurrentUserLiveData().observe(getViewLifecycleOwner(), firebaseUser -> {
            if (firebaseUser != null) {
                fetchUserData(firebaseUser.getUid());
            } else {
                updateUI(null, null, null); // Clear UI if no user is logged in
            }
        });
    }


    protected void navigateBack() {
        Integer destinationId = navigationManager.popFragmentId();
        if (destinationId != null) {
            navController.navigate(destinationId);
        } else {
            // If no ID in the stack, try to pop the back stack or navigate to a default home fragment
            if (!navController.popBackStack()) {
                // No more entries in the back stack, navigate to a safe default
                navController.navigate(R.id.nav_home);
            }
        }
    }

    protected void fetchUserData(String userId) {
        if (!isAdded()) {
            Log.e("BaseFragment", "Fragment not attached when trying to fetch user data.");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isAdded()) { // Check if the fragment is still active
                    if (snapshot.exists()) {
                        updateUI(snapshot.child("name").getValue(String.class),
                                snapshot.child("email").getValue(String.class),
                                snapshot.child("imageUrl").getValue(String.class));
                    } else {
                        Log.d("BaseFragment", "User data does not exist for ID: " + userId);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("BaseFragment", "Failed to fetch user data: " + error.getMessage());
            }
        });
    }


    protected void updateUI(String name, String email, String imageUrl) {
        if (!isAdded()) {
            Log.e("BaseFragment", "Fragment not attached when trying to update UI.");
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
                navUsername.invalidate();
                navUsername.requestLayout();
            }
            if (navEmail != null) {
                navEmail.setText(email);
                navEmail.invalidate();
                navEmail.requestLayout();
            }
            if (navImageView != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Ensure the user is authenticated
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        // Use Firebase Storage Reference to get the download URL
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Glide.with(this)
                                    .load(uri)
                                    .placeholder(R.drawable.groom_icon) // Replace with your default placeholder
                                    .error(R.drawable.groom_icon) // Replace with your error image
                                    .into(navImageView);
                        }).addOnFailureListener(e -> {
                            Log.e("BaseFragment", "Failed to get download URL", e);
                            // Handle failure to get the URL (e.g., set a default image)
                            navImageView.setImageResource(R.drawable.groom_icon);
                        });
                    }
                } else {
                    // Set a default image if imageUrl is null or empty
                    navImageView.setImageResource(R.drawable.groom_icon);
                }
            }
        }
    }

    protected void restartActivity() {
        if (getActivity() != null) {
            getActivity().finish();
            getActivity().startActivity(getActivity().getIntent());
        }
    }

}
