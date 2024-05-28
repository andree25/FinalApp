package com.app.finalapp.ui.home;

import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.finalapp.databinding.FragmentHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FragmentHomeBinding binding;
    private ArticleAdapter articleAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initializeUIElements();
        setupRecyclerView();
        setupSwipeRefreshLayout();
        observeViewModel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        if (hasLocationPermission()) {
            getLastKnownLocation();
        } else {
            requestLocationPermissions();
        }

        return root;
    }

    private void initializeUIElements() {
        progressBar = binding.progressBar;
        swipeRefreshLayout = binding.swipeRefreshLayout;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewHome;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        articleAdapter = new ArticleAdapter(getContext());
        recyclerView.setAdapter(articleAdapter);
    }

    private void setupSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this::getLastKnownLocation);
    }

    private void observeViewModel() {
        homeViewModel.getArticles().observe(getViewLifecycleOwner(), articles -> {
            if (articles != null && !articles.isEmpty()) {
                articleAdapter.updateArticles(articles);
                binding.recyclerViewHome.setVisibility(View.VISIBLE);
            } else {
                binding.recyclerViewHome.setVisibility(View.GONE);
                Toast.makeText(getContext(), "No results found.", Toast.LENGTH_SHORT).show();
            }
        });

        homeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        homeViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void getLastKnownLocation() {
        if (hasLocationPermission()) {
            try {
                progressBar.setVisibility(View.VISIBLE);
                fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), this::handleLocationResult);
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else {
            homeViewModel.fetchAnimalData(homeViewModel.getDefaultCountry());
        }
    }

    private void handleLocationResult(Location location) {
        if (location != null) {
            homeViewModel.fetchCountryFromLocation(location, new Geocoder(getContext(), Locale.getDefault()));
        } else {
            Toast.makeText(getContext(), "Location not found. Using default country.", Toast.LENGTH_SHORT).show();
            homeViewModel.fetchAnimalData(homeViewModel.getDefaultCountry());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied. Using default country.", Toast.LENGTH_SHORT).show();
                homeViewModel.fetchAnimalData(homeViewModel.getDefaultCountry());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
