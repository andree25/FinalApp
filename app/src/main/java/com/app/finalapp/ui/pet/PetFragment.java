package com.app.finalapp.ui.pet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private View view;

    private PetViewModel mViewModel;
    private Spinner petTypeSpinner, genderTypeSpinner;
    private RecyclerView imagesRecyclerView;
    private ImagesAdapter imagesAdapter;
    private List<Uri> imagesUriList = new ArrayList<>();
    private List<String> uploadedUrls = new ArrayList<>();
    private static final int IMAGE_PICK_CODE = 1000;

    public static PetFragment newInstance() {
        return new PetFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_pet, container, false);

        petTypeSpinner = view.findViewById(R.id.pet_type_spinner);
        genderTypeSpinner = view.findViewById(R.id.gender_type_spinner);
        imagesRecyclerView = view.findViewById(R.id.images_recyclerview);

        ArrayAdapter<CharSequence> adapterPet = new ArrayAdapter<>(getContext(), R.layout.spinner_item, getResources().getStringArray(R.array.animal_types));
        ArrayAdapter<CharSequence> adapterGender = new ArrayAdapter<>(getContext(), R.layout.spinner_item, getResources().getStringArray(R.array.gender_type));
        adapterPet.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        petTypeSpinner.setAdapter(adapterPet);
        genderTypeSpinner.setAdapter(adapterGender);

        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagesAdapter = new ImagesAdapter(getContext(), imagesUriList);
        imagesRecyclerView.setAdapter(imagesAdapter);

        ExtendedFloatingActionButton addPetImage = view.findViewById(R.id.addPetImage);
        addPetImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_CODE);
        });

        Button savePetButton = view.findViewById(R.id.savePetButton);
        savePetButton.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImagesAndSavePet();
            }
        });

        return view;
    }

    private boolean validateInputs() {
        if (petTypeSpinner.getSelectedItem() == null ||
                genderTypeSpinner.getSelectedItem() == null ||
                ((EditText) getView().findViewById(R.id.age_edittext)).getText().toString().trim().isEmpty() ||
                ((EditText) getView().findViewById(R.id.editTextTextMultiLine)).getText().toString().trim().isEmpty() ||
                imagesUriList.isEmpty()) {

            Toast.makeText(getContext(), "All fields and at least one image are required", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void uploadImagesAndSavePet() {
        showLoadingIndicator();  // Show loading when starting to upload images
        if (imagesUriList.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one image.", Toast.LENGTH_SHORT).show();
            hideLoadingIndicator();  // Hide loading if there are no images to upload
            return;
        }

        uploadedUrls.clear();  // Clear previously uploaded URLs

        for (Uri uri : imagesUriList) {
            uploadImageToFirebase(uri, new ImageUploadCallback() {
                @Override
                public void onUploadSuccess(String imageUrl) {
                    uploadedUrls.add(imageUrl);
                    // Check if all images are uploaded
                    if (uploadedUrls.size() == imagesUriList.size()) {
                        savePetData(uploadedUrls);
                    }
                }

                @Override
                public void onUploadFailure(Exception e) {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();  // Hide loading indicator if the upload fails
                }
            });
        }
    }


    private void uploadImageToFirebase(Uri imageUri, ImageUploadCallback callback) {
        // Reference to where the image will be stored in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("pet_images/" + System.currentTimeMillis() + "_pet_image");

        // Upload the image file to Firebase Storage
        storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    // After the upload is done, request the public download URL
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Call the onUploadSuccess in the callback with the URL
                        callback.onUploadSuccess(task.getResult().toString());
                    } else if (task.getException() != null) {
                        // Call the onUploadFailure in the callback
                        callback.onUploadFailure(task.getException());
                    }
                });
    }


    private void savePetData(List<String> imageUrls) {
        String type = petTypeSpinner.getSelectedItem().toString();
        String age = ((EditText) getView().findViewById(R.id.age_edittext)).getText().toString();
        String gender = genderTypeSpinner.getSelectedItem().toString();
        String description = ((EditText) getView().findViewById(R.id.editTextTextMultiLine)).getText().toString();

        Pet newPet = new Pet(type, age, gender, description, imageUrls);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userPetsRef = FirebaseDatabase.getInstance().getReference("pets").child(user.getUid());
            String petKey = userPetsRef.push().getKey();
            userPetsRef.child(petKey).setValue(newPet)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Pet saved successfully!", Toast.LENGTH_SHORT).show();
                        navigateBack();  // Navigate back after successful operation
                        hideLoadingIndicator();  // Hide loading indicator on success
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to save pet.", Toast.LENGTH_SHORT).show();
                        hideLoadingIndicator();  // Hide loading indicator on failure
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            hideLoadingIndicator();  // Hide loading indicator if user is not logged in
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

    interface ImageUploadCallback {
        void onUploadSuccess(String imageUrl);

        void onUploadFailure(Exception e);
    }
    private void showLoadingIndicator() {
        ProgressBar loadingIndicator = view.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        ProgressBar loadingIndicator = view.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.GONE);
    }
}
