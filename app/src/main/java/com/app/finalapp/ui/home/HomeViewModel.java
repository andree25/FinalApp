package com.app.finalapp.ui.home;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<SearchResponse.Item>> articles = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private static final String API_KEY = "AIzaSyC2kJo3orR-fjfK2hVuDm14pJibpLvOMV4";
    private static final String CX = "34b03732c16d245be";
    private static final String DEFAULT_COUNTRY = "United States";

    public LiveData<List<SearchResponse.Item>> getArticles() {
        return articles;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public String getDefaultCountry() {
        return DEFAULT_COUNTRY;
    }

    public void fetchCountryFromLocation(Location location, Geocoder geocoder) {
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String country = addresses.get(0).getCountryName();
                fetchAnimalData(country);
            } else {
                postMessage("Unable to determine the country from the location. Using default country.");
                fetchAnimalData(DEFAULT_COUNTRY);
            }
        } catch (IOException e) {
            postMessage("Geocoder service is not available. Using default country.");
            e.printStackTrace();
            fetchAnimalData(DEFAULT_COUNTRY);
        }
    }

    public void fetchAnimalData(String country) {
        isLoading.setValue(true);

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://www.googleapis.com/").addConverterFactory(GsonConverterFactory.create()).build();
        GoogleSearchApiService service = retrofit.create(GoogleSearchApiService.class);
        String query = "Animals in " + country;

        service.getSearchResults(API_KEY, CX, query).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    articles.setValue(response.body().getItems());
                } else {
                    postMessage("No results found.");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                isLoading.setValue(false);
                postMessage("Failed to fetch data.");
            }
        });
    }

    private void postMessage(String msg) {
        message.setValue(msg);
    }
}
