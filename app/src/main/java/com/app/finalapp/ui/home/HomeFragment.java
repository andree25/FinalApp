package com.app.finalapp.ui.home;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.finalapp.databinding.FragmentHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ArticleAdapter articleAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final String API_KEY = "AIzaSyC2kJo3orR-fjfK2hVuDm14pJibpLvOMV4";
    private final String CX = "34b03732c16d245be";
    private final String DEFAULT_COUNTRY = "United States";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerViewHome;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        articleAdapter = new ArticleAdapter(getContext());
        recyclerView.setAdapter(articleAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastKnownLocation();
        }

        return root;
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                fetchCountryFromLocation(location);
                            } else {
                                Toast.makeText(getContext(), "Location not found. Using default country.", Toast.LENGTH_SHORT).show();
                                fetchAnimalData(DEFAULT_COUNTRY);
                            }
                        }
                    });
        } else {
            fetchAnimalData(DEFAULT_COUNTRY);
        }
    }

    private void fetchCountryFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String country = addresses.get(0).getCountryName();
                fetchAnimalData(country);
            } else {
                Toast.makeText(getContext(), "Unable to determine the country from the location. Using default country.", Toast.LENGTH_SHORT).show();
                fetchAnimalData(DEFAULT_COUNTRY);
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Geocoder service is not available. Using default country.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            fetchAnimalData(DEFAULT_COUNTRY);
        }
    }

    private void fetchAnimalData(String country) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleSearchApiService service = retrofit.create(GoogleSearchApiService.class);
        String query = "Animals in " + country;
        Call<SearchResponse> call = service.getSearchResults(API_KEY, CX, query);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayAnimals(response.body().getItems());
                } else {
                    binding.recyclerViewHome.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No results found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                binding.recyclerViewHome.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to fetch data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAnimals(List<SearchResponse.Item> items) {
        if (items == null || items.isEmpty()) {
            binding.recyclerViewHome.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No animal information available.", Toast.LENGTH_SHORT).show();
        } else {
            binding.recyclerViewHome.setVisibility(View.VISIBLE);
            articleAdapter.updateArticles(items);
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
                fetchAnimalData(DEFAULT_COUNTRY);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}