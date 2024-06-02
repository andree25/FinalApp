// DonationFragment.java
package com.app.finalapp.ui.donation;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.PayPal;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DonationFragment extends BaseFragment implements PaymentMethodNonceCreatedListener, BraintreeCancelListener {
    private static final String TAG = "DonationFragment";
    private BraintreeFragment mBraintreeFragment;
    private EditText amountEdt;
    private ProgressBar progressBar, loadingProgressBar;
    private TextView textViewCollected, textViewTarget, donationsDescription;
    private DatabaseReference database;
    private NavController navController;
    private int targetFragmentId;
    private AuthenticationManager authManager;
    private String transactionId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        authManager = new AuthenticationManager();  // Initialize your AuthenticationManager
        View view = inflater.inflate(R.layout.fragment_donation, container, false);

        amountEdt = view.findViewById(R.id.amountEdt);
        Button donateButton = view.findViewById(R.id.donateButton);
        progressBar = view.findViewById(R.id.progressDonation);
        textViewCollected = view.findViewById(R.id.donationTargetSum);
        textViewTarget = view.findViewById(R.id.initialtarget);
        donationsDescription = view.findViewById(R.id.donationsDescription);
        database = FirebaseDatabase.getInstance().getReference().child("adminSettings");
        loadingProgressBar = view.findViewById(R.id.progressBar3);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        if (getArguments() != null) {
            targetFragmentId = getArguments().getInt("targetFragmentId");
        }

        if (authManager.getCurrentUser() == null) {
            navigateToLogin();
            return null; // Return null to prevent further execution of onCreateView
        } else {
            fetchUserData(authManager.getCurrentUser().getUid());
        }

        donateButton.setOnClickListener(v -> {
            String amountString = amountEdt.getText().toString().trim();
            if (!amountString.isEmpty()) {
                int amount = Integer.parseInt(amountString);
                showLoadingIndicator();
                initiatePayment(amountString);
            }
        });

        fetchClientToken();
        fetchTargetAndCurrentAmount();
        fetchCauseDescription();
        return view;
    }

    private void navigateToLogin() {
        Bundle args = new Bundle();
        args.putInt("targetFragmentId", R.id.nav_donate);
        navController.navigate(R.id.nav_login, args);
    }

    private void fetchTargetAndCurrentAmount() {
        retryWithExponentialBackoff(() -> database.child("target").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                int targetAmount = dataSnapshot.getValue(Integer.class);
                textViewTarget.setText(String.format(Locale.getDefault(), "Target: $%d", targetAmount));
                progressBar.setMax(targetAmount);
                Log.d(TAG, "Target amount fetched successfully: " + targetAmount);
            } else {
                Log.e(TAG, "Target amount not found.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching target amount: " + e.getMessage());
        }));

        retryWithExponentialBackoff(() -> database.child("currentAmount").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                int currentAmount = dataSnapshot.getValue(Integer.class);
                updateProgress(currentAmount);
                Log.d(TAG, "Current amount fetched successfully: " + currentAmount);
            } else {
                Log.e(TAG, "Current amount not found.");
                textViewCollected.setText(String.format(Locale.getDefault(), "Collected: $0"));
                progressBar.setProgress(0);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching current amount: " + e.getMessage());
            textViewCollected.setText(String.format(Locale.getDefault(), "Collected: $0"));
            progressBar.setProgress(0);
        }));
    }

    private void fetchCauseDescription() {
        retryWithExponentialBackoff(() -> database.child("cause").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                String cause = dataSnapshot.getValue(String.class);
                donationsDescription.setText(cause);
            } else {
                donationsDescription.setText("Cause description not available.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch cause description", e);
            donationsDescription.setText("Failed to load cause.");
        }));
    }

    private void fetchClientToken() {
        retryWithExponentialBackoff(() -> FirebaseFunctions.getInstance().getHttpsCallable("generateClientToken").call().addOnSuccessListener(httpsCallableResult -> {
            Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();
            String clientToken = (String) result.get("clientToken");
            setupBraintree(clientToken);
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch client token", e)));
    }

    private void setupBraintree(String clientToken) {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(getActivity(), clientToken);
            mBraintreeFragment.addListener(this);
            mBraintreeFragment.addListener((BraintreeErrorListener) error -> Log.e(TAG, "BraintreeError: ", error));
            mBraintreeFragment.addListener(this); // Added BraintreeCancelListener
        } catch (InvalidArgumentException e) {
            Log.e(TAG, "Error initializing BraintreeFragment.", e);
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        String nonce = paymentMethodNonce.getNonce();
        String amount = amountEdt.getText().toString().trim();
        if (!amount.isEmpty()) {
            postNonceToServer(nonce, Integer.parseInt(amount));
        }
    }

    private void initiatePayment(String amount) {
        transactionId = UUID.randomUUID().toString(); // Generate a unique transaction ID
        logTransactionStart(transactionId, amount);
        PayPal.requestOneTimePayment(mBraintreeFragment, new PayPalRequest(amount));
    }

    private void postNonceToServer(String nonce, int amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentMethodNonce", nonce);
        data.put("amount", amount);
        data.put("transactionId", transactionId); // Pass the transaction ID to ensure idempotency

        retryWithExponentialBackoff(() -> FirebaseFunctions.getInstance().getHttpsCallable("processPayment").call(data)
                .addOnSuccessListener(httpsCallableResult -> {
                    Log.d(TAG, "Payment processed successfully");
                    // Call saveDonation only after successful payment processing
                    saveDonation(amount);
                    updateTransactionStatus(transactionId, "success");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to process payment", e);
                    Toast.makeText(getContext(), "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    hideLoadingIndicator(); // Hide loading indicator if payment fails
                    updateTransactionStatus(transactionId, "failed");
                }));
    }

    private void saveDonation(int amount) {
        retryWithExponentialBackoff(() -> database.child("currentAmount").get().addOnSuccessListener(dataSnapshot -> {
            int currentAmount = dataSnapshot.exists() ? dataSnapshot.getValue(Integer.class) : 0;
            int updatedAmount = currentAmount + amount;
            database.child("currentAmount").setValue(updatedAmount)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Donation updated successfully");
                            updateProgress(updatedAmount); // Update UI only here after successful database update
                            amountEdt.setText(""); // Clear the donation amount input field after the donation is successfully processed
                            hideLoadingIndicator();
                        } else {
                            Log.e(TAG, "Failed to update donation amount", task.getException());
                            Toast.makeText(getContext(), "Failed to update donation total", Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch current donation amount", e);
        }));
    }

    private void updateProgress(int currentAmount) {
        textViewCollected.setText(String.format(Locale.getDefault(), "Collected: $%d", currentAmount));
        int progress = (int) ((currentAmount / (float) progressBar.getMax()) * 100);
        progressBar.setProgress(progress);
    }

    private void showLoadingIndicator() {
        loadingProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void retryWithExponentialBackoff(Runnable task) {
        int maxRetries = 5;
        int initialDelay = 1000; // Initial delay in milliseconds
        for (int i = 0; i < maxRetries; i++) {
            try {
                task.run();
                return; // Task succeeded
            } catch (Exception e) {
                Log.e(TAG, "Task failed, attempt " + (i + 1), e);
                try {
                    TimeUnit.MILLISECONDS.sleep(initialDelay * (long) Math.pow(2, i));
                } catch (InterruptedException interruptedException) {
                    Log.e(TAG, "Retry interrupted", interruptedException);
                }
            }
        }
        Log.e(TAG, "Task failed after " + maxRetries + " attempts");
    }

    private void logTransactionStart(String transactionId, String amount) {
        // Log the start of the transaction
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("transactionId", transactionId);
        transactionData.put("amount", amount);
        transactionData.put("status", "pending");

        FirebaseDatabase.getInstance().getReference("transactions").child(transactionId).setValue(transactionData)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log transaction start", e));
    }

    private void updateTransactionStatus(String transactionId, String status) {
        // Update the status of the transaction
        FirebaseDatabase.getInstance().getReference("transactions").child(transactionId).child("status").setValue(status)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update transaction status", e));
    }

    @Override
    public void onCancel(int requestCode) {
        // Handle the cancellation of the payment
        Log.d(TAG, "Payment was cancelled by the user.");
        hideLoadingIndicator();
        updateTransactionStatus(transactionId, "cancelled");
        Toast.makeText(getContext(), "Payment was cancelled.", Toast.LENGTH_SHORT).show();
    }
}
