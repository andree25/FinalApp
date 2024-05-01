package com.app.finalapp.ui.petDetail;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.finalapp.Pet;
import com.app.finalapp.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PetDetailFragment extends Fragment {
    private Pet pet;
    private ImageView mainImage;
    private Handler imageSwitcherHandler;
    private Runnable imageSwitcherRunnable;
    private List<String> imageUrls;
    private int currentImageIndex = 0;
    private static final int IMAGE_SWITCH_DELAY = 3000; // 3 seconds delay
    private static final String TAG = "PetDetailFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageSwitcherHandler = new Handler();
        if (getArguments() != null) {
            pet = (Pet) getArguments().getSerializable("pet");
            if (pet == null) {
                Log.e(TAG, "No pet data available, returning to previous screen.");
            } else {
                imageUrls = pet.getImageUrls();
            }
        }
    }

    private void startImageSlideshow() {
        Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // After fade out, load next image and fade it in
                Glide.with(getContext())
                        .load(imageUrls.get(currentImageIndex))
                        .into(mainImage);
                mainImage.startAnimation(fadeIn);
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        imageSwitcherRunnable = new Runnable() {
            @Override
            public void run() {
                if (getContext() == null) return;
                mainImage.startAnimation(fadeOut);
                imageSwitcherHandler.postDelayed(this, IMAGE_SWITCH_DELAY);
            }
        };
        imageSwitcherHandler.post(imageSwitcherRunnable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pet_detail, container, false);
        if (pet != null) {
            setupViews(view);
        }
        return view;
    }

    private void setupViews(View view) {
        mainImage = view.findViewById(R.id.pet_main_image);
        TextView petType = view.findViewById(R.id.pet_type);
        TextView petAge = view.findViewById(R.id.pet_age);
        TextView petGender = view.findViewById(R.id.pet_gender);
        TextView petDescription = view.findViewById(R.id.pet_description);
        Button emailButton = view.findViewById(R.id.send_email_button);
        TextView textButton = view.findViewById(R.id.textButton);
        petType.setText(pet.getType());
        petAge.setText(pet.getAge());
        petGender.setText(pet.getGender());
        petDescription.setText(pet.getDescription());

        if (imageUrls != null && !imageUrls.isEmpty()) {
            startImageSlideshow();
        }
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchUserEmailAndSendEmail();
            }
        });

        textButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && currentUser.getUid().equals(pet.getUserId())) {
                    confirmAndDeletePet();
                } else {
                    Toast.makeText(getContext(), "You do not have permission to delete this pet.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmAndDeletePet() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Pet")
                .setMessage("Are you sure you want to delete this pet?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deletePet();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePet() {
        if (pet.getUid() == null || pet.getUid().isEmpty()) {
            Toast.makeText(getContext(), "Error: Pet key is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("pets").child(pet.getUserId()).child(pet.getUid());
        petRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Pet deleted successfully", Toast.LENGTH_SHORT).show();
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack(); // Go back to the previous fragment
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete pet: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void fetchUserEmailAndSendEmail() {
        if (pet.getUserId() == null || pet.getUserId().isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            Toast.makeText(getContext(), "User ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(pet.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String email = dataSnapshot.child("email").getValue(String.class);
                    sendEmail(email);
                } else {
                    Log.e(TAG, "User not found");
                    Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to access database.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void sendEmail(String emailAddress) {
        String[] addresses = {emailAddress}; // Email address fetched from the database
        String subject = "Inquiry about " + pet.getType();
        String body = "Hello! \n I am interested in your " + pet.getType() + ", aged " + pet.getAge() + ", gender: " + pet.getGender() + ". \nCould you please let me know if the Animal is still available for adoption?";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageSwitcherHandler != null) {
            imageSwitcherHandler.removeCallbacks(imageSwitcherRunnable);
        }
    }

}
