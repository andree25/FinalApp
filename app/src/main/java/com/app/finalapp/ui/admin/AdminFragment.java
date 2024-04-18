package com.app.finalapp.ui.admin;

import androidx.fragment.app.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.app.finalapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminFragment extends Fragment {

    private EditText causeInput, targetInput;
    private FloatingActionButton saveCauseButton, saveTargetButton;
    private ExtendedFloatingActionButton resetCollectedAmount;
    private DatabaseReference database;

    public static AdminFragment newInstance() {
        return new AdminFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        causeInput = view.findViewById(R.id.cause_input);
        targetInput = view.findViewById(R.id.target_input);
        saveCauseButton = view.findViewById(R.id.donation_case_button);
        saveTargetButton = view.findViewById(R.id.donation_taget_button);
        resetCollectedAmount = view.findViewById(R.id.reset_collected_amount);
        database = FirebaseDatabase.getInstance().getReference().child("adminSettings");

        saveCauseButton.setOnClickListener(v -> saveCause());
        saveTargetButton.setOnClickListener(v -> saveTarget());
        resetCollectedAmount.setOnClickListener(v -> resetCollectedAmount());

        return view;
    }

    private void saveCause() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is authenticated, proceed with database write
            String cause = causeInput.getText().toString().trim();
            if (!cause.isEmpty()) {
                FirebaseDatabase.getInstance().getReference("adminSettings").child("cause").setValue(cause)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cause saved successfully!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save cause.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), "Please enter a cause.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // User not authenticated, handle accordingly
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTarget() {
        String target = targetInput.getText().toString().trim();
        if (!target.isEmpty()) {
            try {
                int targetValue = Integer.parseInt(target);
                database.child("target").setValue(targetValue)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Target saved successfully!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save target.", Toast.LENGTH_SHORT).show());
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Please enter a target.", Toast.LENGTH_SHORT).show();
        }
    }
    private void resetCollectedAmount() {
        database.child("currentAmount").setValue(0)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Collected amount reset successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to reset collected amount.", Toast.LENGTH_SHORT).show());
    }
}
