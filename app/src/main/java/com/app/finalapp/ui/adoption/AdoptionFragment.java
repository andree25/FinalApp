package com.app.finalapp.ui.adoption;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.Pet;
import com.app.finalapp.PetAdapter;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdoptionFragment extends BaseFragment {
    private AuthenticationManager authManager;
    private AdoptionViewModel mViewModel;
    private NavController navController;
    private NavigationManager navigationManager;
    private DatabaseReference databaseReference;
    private FloatingActionButton addPet;
    private Button allButton, dogsButton, catsButton, bunnyButton, hamstersButton, guineaButton, parrotButton, fishButton, otherButton;
    SearchView searchView;
    private String currentCategory = "All";  // Default to "All"

    public static AdoptionFragment newInstance() {
        return new AdoptionFragment();
    }

    private PetAdapter petAdapter;
    private List<Pet> petList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_adoption, container, false);
        authManager = new AuthenticationManager();
        databaseReference = FirebaseDatabase.getInstance().getReference("adoptions");
        addPet = rootView.findViewById(R.id.dog_add_button);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navigationManager = NavigationManager.getInstance();

        allButton = rootView.findViewById(R.id.all_button);
        dogsButton = rootView.findViewById(R.id.dogs_button);
        catsButton = rootView.findViewById(R.id.cats_button);
        bunnyButton = rootView.findViewById(R.id.bunny_button);
        hamstersButton = rootView.findViewById(R.id.hamster_button);
        guineaButton = rootView.findViewById(R.id.guinea_button);
        parrotButton = rootView.findViewById(R.id.parrot_button);
        fishButton = rootView.findViewById(R.id.fish_button);
        otherButton = rootView.findViewById(R.id.other_button);

        allButton.setOnClickListener(v -> {
            currentCategory = "All";
            loadPetsFromDatabase();
            searchView.setQuery("", false);
        });
        dogsButton.setOnClickListener(v -> {
            currentCategory = "Dog";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        catsButton.setOnClickListener(v -> {
            currentCategory = "Cat";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        bunnyButton.setOnClickListener(v -> {
            currentCategory = "Bunny";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        hamstersButton.setOnClickListener(v -> {
            currentCategory = "Hamster";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        guineaButton.setOnClickListener(v -> {
            currentCategory = "Guinea Pig";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        parrotButton.setOnClickListener(v -> {
            currentCategory = "Parrot";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        fishButton.setOnClickListener(v -> {
            currentCategory = "Fishes";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });
        otherButton.setOnClickListener(v -> {
            currentCategory = "Other";
            filterPetsByType(currentCategory);
            searchView.setQuery("", false);
        });

        searchView = rootView.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPets(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchPets(newText);
                return true;
            }
        });

        loadPetsFromDatabase();
        addPet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_nav_adopt_to_nav_pet);
            }
        });

        setupRecyclerView(rootView);
        checkLoginStatus();
        return rootView;
    }

    private void setupRecyclerView(View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        petAdapter = new PetAdapter(getContext(), petList, pet -> {
            Toast.makeText(getContext(), "Clicked on " + pet.getType(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(petAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navigationManager.pushFragmentId(R.id.nav_adopt);
    }

    private void filterPetsByType(String type) {
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("pets");
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                petList.clear();
                for (DataSnapshot userPetsSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot petSnapshot : userPetsSnapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null && type.equals(pet.getType())) {
                            petList.add(pet);
                        }
                    }
                }
                petAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void searchPets(String query) {
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("pets");
        petsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                petList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null && petMatchesQuery(pet, query) && matchesCategory(pet)) {
                            petList.add(pet);
                        }
                    }
                }
                petAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private boolean matchesCategory(Pet pet) {
        return currentCategory.equals("All") || pet.getType().equalsIgnoreCase(currentCategory);
    }


    private boolean petMatchesQuery(Pet pet, String query) {
        query = query.toLowerCase();
        return (pet.getType().toLowerCase().contains(query) ||
                pet.getAge().toLowerCase().contains(query) ||
                pet.getGender().toLowerCase().contains(query) ||
                pet.getDescription().toLowerCase().contains(query));
    }

    private void checkLoginStatus() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            enableAdoptionFeatures();
        } else {
            navController.navigate(R.id.nav_login);
        }
    }

    private void loadPetsFromDatabase() {
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("pets");
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                petList.clear();
                for (DataSnapshot userPetsSnapshot : dataSnapshot.getChildren()) {
                    // Iterate over each user's pets
                    for (DataSnapshot petSnapshot : userPetsSnapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null) {
                            petList.add(pet);
                        }
                    }
                }
                petAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
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
