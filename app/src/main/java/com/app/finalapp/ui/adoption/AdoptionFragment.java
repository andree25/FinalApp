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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdoptionFragment extends BaseFragment {
    private AuthenticationManager authManager;
    private AdoptionViewModel mViewModel;
    private NavController navController;
    private NavigationManager navigationManager;
    private DatabaseReference databaseReference;

    public static AdoptionFragment newInstance() {
        return new AdoptionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_adoption, container, false);
        authManager = new AuthenticationManager();
        databaseReference = FirebaseDatabase.getInstance().getReference("adoptions");

        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navigationManager = NavigationManager.getInstance();

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
            navController.navigate(R.id.nav_login);
        }
    }

    private void enableAdoptionFeatures() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            Log.d("AdoptionFragment", "User ID: " + user.getUid());
            fetchUserData(user.getUid());
            saveAdoptionData(user.getUid(), "Sample Adoption Data"); // Example function call
        }
    }

    private void saveAdoptionData(String userId, String data) {
        // Using push() to save data without overwriting
        databaseReference.child(userId).push().setValue(data)
                .addOnSuccessListener(aVoid -> Log.d("AdoptionFragment", "Adoption data saved successfully"))
                .addOnFailureListener(e -> Log.e("AdoptionFragment", "Failed to save adoption data", e));
    }
}
