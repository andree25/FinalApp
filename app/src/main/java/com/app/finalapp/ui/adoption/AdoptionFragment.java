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

import com.app.finalapp.R;
import com.app.finalapp.ui.login.LoginViewModel;

public class AdoptionFragment extends Fragment {

    private AdoptionViewModel mViewModel;
    private NavController navController; // Declare NavController

    public static AdoptionFragment newInstance() {
        return new AdoptionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_adoption, container, false);

        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        mViewModel = new ViewModelProvider(this).get(AdoptionViewModel.class);

        mViewModel.isUserLoggedIn().observe(getViewLifecycleOwner(), isLoggedIn -> {
            if (isLoggedIn) {
                enableAdoptionFeatures();
            } else {
                navController.navigate(R.id.nav_login);
            }
        });

        return rootView;
    }

    private void enableAdoptionFeatures() {
        // Implement logic to enable adoption-related features
        // For example, enable buttons, display content, etc.
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AdoptionViewModel.class);
        // TODO: Use the ViewModel
    }

}