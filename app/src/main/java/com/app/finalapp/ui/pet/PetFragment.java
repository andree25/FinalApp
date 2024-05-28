package com.app.finalapp.ui.pet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.finalapp.Pet;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PetFragment extends BaseFragment {

    private static final int IMAGE_PICK_CODE = 1000;
    private PetViewModel mViewModel;
    private Spinner petTypeSpinner, genderTypeSpinner;
    private RecyclerView imagesRecyclerView;
    private ImagesAdapter imagesAdapter;
    private List<Uri> imagesUriList = new ArrayList<>();
    private List<String> uploadedUrls = new ArrayList<>();
    private View rootView;

    public static PetFragment newInstance() {
        return new PetFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pet, container, false);
        mViewModel = new ViewModelProvider(this).get(PetViewModel.class);

        setupUI();
        setupRecyclerView();
        setupObservers();

        return rootView;
    }

    private void setupUI() {
        petTypeSpinner = rootView.findViewById(R.id.pet_type_spinner);
        genderTypeSpinner = rootView.findViewById(R.id.gender_type_spinner);
        ExtendedFloatingActionButton addPetImage = rootView.findViewById(R.id.addPetImage);
        Button savePetButton = rootView.findViewById(R.id.savePetButton);

        ArrayAdapter<CharSequence> adapterPet = new ArrayAdapter<>(getContext(), R.layout.spinner_item, getResources().getStringArray(R.array.animal_types));
        ArrayAdapter<CharSequence> adapterGender = new ArrayAdapter<>(getContext(), R.layout.spinner_item, getResources().getStringArray(R.array.gender_type));
        adapterPet.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        petTypeSpinner.setAdapter(adapterPet);
        genderTypeSpinner.setAdapter(adapterGender);

        addPetImage.setOnClickListener(v -> openImagePicker());
        savePetButton.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImagesAndSavePet();
            }
        });
    }

    private void setupRecyclerView() {
        imagesRecyclerView = rootView.findViewById(R.id.images_recyclerview);
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagesAdapter = new ImagesAdapter(getContext(), imagesUriList);
        imagesRecyclerView.setAdapter(imagesAdapter);
    }

    private void setupObservers() {
        mViewModel.getImagesUriList().observe(getViewLifecycleOwner(), images -> imagesAdapter.notifyDataSetChanged());
        mViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoadingIndicator();
            } else {
                hideLoadingIndicator();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_CODE);
    }

    private boolean validateInputs() {
        EditText ageEditText = rootView.findViewById(R.id.age_edittext);
        EditText descriptionEditText = rootView.findViewById(R.id.editTextTextMultiLine);

        if (petTypeSpinner.getSelectedItem() == null ||
                genderTypeSpinner.getSelectedItem() == null ||
                ageEditText.getText().toString().trim().isEmpty() ||
                descriptionEditText.getText().toString().trim().isEmpty() ||
                imagesUriList.isEmpty()) {
            Toast.makeText(getContext(), "All fields and at least one image are required", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void uploadImagesAndSavePet() {
        if (imagesUriList.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one image.", Toast.LENGTH_SHORT).show();
            return;
        }

        mViewModel.setLoading(true);
        uploadedUrls.clear();
        for (Uri uri : imagesUriList) {
            uploadImageToFirebase(uri, new ImageUploadCallback() {
                @Override
                public void onUploadSuccess(String imageUrl) {
                    uploadedUrls.add(imageUrl);
                    if (uploadedUrls.size() == imagesUriList.size()) {
                        savePetData(uploadedUrls);
                    }
                }

                @Override
                public void onUploadFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mViewModel.setLoading(false);
                }
            });
        }
    }

    private void uploadImageToFirebase(Uri imageUri, ImageUploadCallback callback) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("pet_images/" + System.currentTimeMillis() + "_pet_image");

        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onUploadSuccess(task.getResult().toString());
                    } else if (task.getException() != null) {
                        callback.onUploadFailure(task.getException());
                    }
                });
    }

    private void savePetData(List<String> imageUrls) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String type = petTypeSpinner.getSelectedItem().toString();
            String age = ((EditText) rootView.findViewById(R.id.age_edittext)).getText().toString();
            String gender = genderTypeSpinner.getSelectedItem().toString();
            String description = ((EditText) rootView.findViewById(R.id.editTextTextMultiLine)).getText().toString();

            Pet newPet = new Pet(type, age, gender, description, imageUrls);
            newPet.setUserId(user.getUid());

            DatabaseReference userPetsRef = FirebaseDatabase.getInstance().getReference("pets").child(user.getUid());
            String petKey = userPetsRef.push().getKey();
            newPet.setUid(petKey);
            userPetsRef.child(petKey).setValue(newPet)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Pet saved successfully!", Toast.LENGTH_SHORT).show();
                        navigateBack();
                        mViewModel.setLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to save pet.", Toast.LENGTH_SHORT).show();
                        mViewModel.setLoading(false);
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            mViewModel.setLoading(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            handleImageSelection(data);
        }
    }

    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                imagesUriList.add(imageUri);
            }
        } else if (data.getData() != null) {
            imagesUriList.add(data.getData());
        }
        imagesAdapter.notifyDataSetChanged();
    }

    private void showLoadingIndicator() {
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.GONE);
    }

    interface ImageUploadCallback {
        void onUploadSuccess(String imageUrl);
        void onUploadFailure(Exception e);
    }
}
