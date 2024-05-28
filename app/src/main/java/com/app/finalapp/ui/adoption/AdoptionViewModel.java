package com.app.finalapp.ui.adoption;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.Pet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdoptionViewModel extends ViewModel {
    private final MutableLiveData<List<Pet>> pets = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>();
    private final AuthenticationManager authenticationManager;
    private final DatabaseReference petsRef;
    private String currentCategory = "All";  // Default to "All"

    public AdoptionViewModel() {
        authenticationManager = new AuthenticationManager();
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
        petsRef = FirebaseDatabase.getInstance().getReference("pets");
    }

    public LiveData<List<Pet>> getPets() {
        return pets;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> isUserLoggedIn() {
        return isUserLoggedIn;
    }

    public void checkUserLoggedIn() {
        isUserLoggedIn.setValue(authenticationManager.getCurrentUser() != null);
    }

    public void loadPetsFromDatabase() {
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Pet> petList = new ArrayList<>();
                for (DataSnapshot userPetsSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot petSnapshot : userPetsSnapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null) {
                            petList.add(pet);
                        }
                    }
                }
                pets.setValue(petList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                message.setValue("Failed to load pets");
            }
        });
    }

    public void filterPetsByType(String type) {
        currentCategory = type;  // Set the current category
        petsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Pet> filteredPets = new ArrayList<>();
                for (DataSnapshot userPetsSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot petSnapshot : userPetsSnapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null && (type.equals("All") || pet.getType().equalsIgnoreCase(type))) {
                            filteredPets.add(pet);
                        }
                    }
                }
                pets.setValue(filteredPets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                message.setValue("Failed to filter pets");
            }
        });
    }

    public void searchPets(String query) {
        petsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Pet> searchedPets = new ArrayList<>();
                for (DataSnapshot userPetsSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot petSnapshot : userPetsSnapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null && petMatchesQuery(pet, query) && matchesCategory(pet)) {
                            searchedPets.add(pet);
                        }
                    }
                }
                pets.setValue(searchedPets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                message.setValue("Failed to search pets");
            }
        });
    }

    private boolean matchesCategory(Pet pet) {
        return currentCategory.equals("All") || pet.getType().equalsIgnoreCase(currentCategory);
    }

    private boolean petMatchesQuery(Pet pet, String query) {
        query = query.toLowerCase();
        return pet.getType().toLowerCase().contains(query) ||
                pet.getAge().toLowerCase().contains(query) ||
                pet.getGender().toLowerCase().contains(query);
    }
}
