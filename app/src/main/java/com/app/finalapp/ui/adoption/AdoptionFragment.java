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

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdoptionFragment extends BaseFragment {
    private AuthenticationManager authManager;
    private AdoptionViewModel mViewModel;
    private NavController navController;
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
        navigationManager = NavigationManager.getInstance();
        // Check if user is logged in
        checkLoginStatus();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationManager.pushFragmentId(R.id.nav_adopt);
    }

    private void checkLoginStatus() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            enableAdoptionFeatures();
        } else {
            // Redirect to login if not logged in
            navController.navigate(R.id.nav_login);
        }
    }

    private void enableAdoptionFeatures() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            Log.d("AdoptionFragment", "User ID: " + user.getUid());
            fetchUserData(user.getUid());
        }
    }
}
