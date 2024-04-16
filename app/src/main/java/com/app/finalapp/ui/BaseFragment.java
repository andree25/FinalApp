package com.app.finalapp.ui;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.util.Log;

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
    public void onViewCreated(View view, android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
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

            if (navUsername != null) navUsername.setText(name);
            if (navEmail != null) navEmail.setText(email);
            if (navImageView != null) Glide.with(this).load(imageUrl).into(navImageView);
        }
    }
}