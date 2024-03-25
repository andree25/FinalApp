package com.app.finalapp.ui.adoption;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.app.finalapp.ui.login.LoginViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdoptionFragment extends Fragment {
    private AuthenticationManager authManager;
    private AdoptionViewModel mViewModel;
    private NavController navController; // Declare NavController
    private NavigationManager navigationManager;

    public static AdoptionFragment newInstance() {
        return new AdoptionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_adoption, container, false);
        authManager = new AuthenticationManager(); // Initialize AuthenticationManager

        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        mViewModel = new ViewModelProvider(this).get(AdoptionViewModel.class);

        mViewModel.isUserLoggedIn().observe(getViewLifecycleOwner(), isLoggedIn -> {
            if (isLoggedIn) {
                enableAdoptionFeatures();
            } else {
                navigationManager.setAdoptionFragmentId(R.id.nav_adopt);
                navController.navigate(R.id.nav_login);
            }
        });
        navigationManager = NavigationManager.getInstance();

        return rootView;
    }

    private void enableAdoptionFeatures() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            Log.d("AdoptionFragment", "uuid " + user.getUid());
            fetchUserData(user.getUid());
        } else Log.d("AdoptionFragment", "uuid is empty " + user.getUid());

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

                    Log.d("AdoptionFragment", "fetchUserData: Name: " + name + ", Email: " + email + ", ImageUrl: " + imageUrl);

                } else {
                    Log.d("AdoptionFragment", "fetchUserData: User data does not exist for ID: " + userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdoptionFragment", "Failed to fetch user data: " + error.getMessage());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AdoptionViewModel.class);
        // TODO: Use the ViewModel
    }

}