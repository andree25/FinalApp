package com.app.finalapp.ui.shelter;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.finalapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.maps.model.Polyline;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShelterFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private Polyline currentPolyline;
    private GoogleMap mMap;
    private PlacesClient placesClient;

    public ShelterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Location permission needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE))
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_shelter, container, false);
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyC2kJo3orR-fjfK2hVuDm14pJibpLvOMV4");
        }
        placesClient = Places.createClient(requireContext());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            // Load and set the custom map style
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getContext(), R.raw.custom_map));  // Assuming you've saved the JSON style in res/raw/style_json.json

            if (!success) {
                Log.e("MapsActivity", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivity", "Can't find style. Error: ", e);
        }
        mMap.setMyLocationEnabled(true);  // Enables the "My Location" layer
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                searchNearbyShelters(currentLocation);
                addLocationMarker(currentLocation);
            }
        });
        mMap.setOnMarkerClickListener(marker -> {
            LatLng destination = marker.getPosition();
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    fetchDirections(currentLocation, destination);
                }
            });
            return false; // This should be false if you want the default behavior (like showing the info window)
        });

    }

    private void addLocationMarker(LatLng location) {
        MarkerOptions options = new MarkerOptions().position(location)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));  // Custom color for the marker
        mMap.addMarker(options);
    }

    private void searchNearbyShelters(LatLng location) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + location.latitude + "," + location.longitude +
                "&radius=100000" +
                "&type=dog_shelter" +
                "&keyword=shelter|resque" +
                "&key=AIzaSyC2kJo3orR-fjfK2hVuDm14pJibpLvOMV4";


        JsonObjectRequest
                jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject locationObj = place.getJSONObject("geometry").getJSONObject("location");
                            LatLng placeLocation = new LatLng(locationObj.getDouble("lat"), locationObj.getDouble("lng"));
                            String placeName = place.getString("name");
                            mMap.addMarker(new MarkerOptions().position(placeLocation).title(placeName));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            // Handle error
            error.printStackTrace();
        });

        // Add the request to your request queue
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchDirections(LatLng startLocation, LatLng endLocation) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLocation.latitude + "," + startLocation.longitude +
                "&destination=" + endLocation.latitude + "," + endLocation.longitude +
                "&mode=driving" +  // You can change this to walking, bicycling, etc.
                "&key=AIzaSyC2kJo3orR-fjfK2hVuDm14pJibpLvOMV4";

        JsonObjectRequest directionsRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject jsonResponse = response.getJSONArray("routes").getJSONObject(0);
                        JSONObject poly = jsonResponse.getJSONObject("overview_polyline");
                        JSONObject leg = jsonResponse.getJSONArray("legs").getJSONObject(0);
                        JSONObject durationObject = leg.getJSONObject("duration");
                        String durationText = durationObject.getString("text");  // Get the human-readable duration text
                        String polyline = poly.getString("points");
                        drawPolyline(polyline);
                        showDuration(durationText);  // Display the duration on the UI
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            error.printStackTrace();
        });

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(directionsRequest);
    }

    private void showDuration(String duration) {
        // You can use a Toast or update a TextView element to show the duration:
        Toast.makeText(getContext(), "Estimated travel time: " + duration + " driving", Toast.LENGTH_LONG).show();
    }

    private void drawPolyline(String encodedPolyline) {
        // Remove the existing polyline if there is one
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        List<LatLng> list = decodePoly(encodedPolyline);
        currentPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(list)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true));
    }


    // Method to decode polyline
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
    }


}
