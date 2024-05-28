package com.app.finalapp.ui.petDetail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.finalapp.Pet;

public class PetDetailViewModel extends ViewModel {
    private final MutableLiveData<Pet> pet = new MutableLiveData<>();

    public void setPet(Pet pet) {
        this.pet.setValue(pet);
    }

    public LiveData<Pet> getPet() {
        return pet;
    }
}
