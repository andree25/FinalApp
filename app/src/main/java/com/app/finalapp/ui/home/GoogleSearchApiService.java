package com.app.finalapp.ui.home;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleSearchApiService {
    @GET("customsearch/v1")
    Call<SearchResponse> getSearchResults(
            @Query("key") String apiKey,
            @Query("cx") String cx,
            @Query("q") String query
    );
}

