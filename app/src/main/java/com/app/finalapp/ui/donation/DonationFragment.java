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

import com.app.finalapp.AuthenticationManager;
import com.app.finalapp.NavigationManager;
import com.app.finalapp.R;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.PayPal;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DonationFragment extends Fragment implements PaymentMethodNonceCreatedListener {
    private static final String TAG = "DonationFragment";
    private BraintreeFragment mBraintreeFragment;
    private EditText amountEdt;
    private AuthenticationManager authManager;
    private NavController navController;
    private NavigationManager navigationManager;
    private ProgressBar progressBar;
    private TextView tvCollected, tvTarget;
    private DatabaseReference database;
    private int currentAmount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Initializing DonationFragment view.");
        View view = inflater.inflate(R.layout.fragment_donation, container, false);
        amountEdt = view.findViewById(R.id.amountEdt);
        Button donateButton = view.findViewById(R.id.donateButton);
        progressBar = view.findViewById(R.id.progressDonation);
        tvCollected = view.findViewById(R.id.donationTargetSum);
        tvTarget = view.findViewById(R.id.initialtarget);

        authManager = new AuthenticationManager();
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navigationManager = NavigationManager.getInstance();

        database = FirebaseDatabase.getInstance().getReference().child("adminSettings");

        database.child("target").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int targetAmount = dataSnapshot.getValue(Integer.class);
                    tvTarget.setText(String.format(Locale.getDefault(), "Target: $%d", targetAmount));
                    updateProgress(targetAmount);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load target amount", databaseError.toException());
            }
        });

        donateButton.setOnClickListener(v -> {
            String amountString = amountEdt.getText().toString().trim();
            if (!amountString.isEmpty()) {
                int amount = Integer.parseInt(amountString);
                currentAmount += amount; // Update current collected amount
                updateProgress(currentAmount);
                initiatePayment(amountString);
            }
        });

        fetchClientToken();

        return view;
    }

    private void updateProgress(int targetAmount) {
        tvCollected.setText(String.format(Locale.getDefault(), "Collected: $%d", currentAmount));
        int progress = (int) ((currentAmount / (float) targetAmount) * 100);
        progressBar.setProgress(progress);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavigationManager.getInstance().pushFragmentId(R.id.nav_donate); // Correct place to push the ID
    }

    private void fetchClientToken() {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("generateClientToken")
                .call()
                .addOnSuccessListener(httpsCallableResult -> {
                    // Extract clientToken from the data map
                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();
                    String clientToken = (String) result.get("clientToken");
                    setupBraintree(clientToken);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch client token", e));
    }

    private void setupBraintree(String clientToken) {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(getActivity(), clientToken);
            mBraintreeFragment.addListener(this);
            mBraintreeFragment.addListener((BraintreeErrorListener) error -> Log.e(TAG, "BraintreeError: ", error));
            mBraintreeFragment.addListener((BraintreeCancelListener) requestCode -> Log.d(TAG, "User canceled the payment."));
        } catch (InvalidArgumentException e) {
            Log.e(TAG, "Error initializing BraintreeFragment.", e);
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        String nonce = paymentMethodNonce.getNonce();
        String amount = amountEdt.getText().toString().trim();
        if (!amount.isEmpty()) {
            postNonceToServer(nonce, amount);
        }
    }

    private void initiatePayment(String amount) {
        PayPal.requestOneTimePayment(mBraintreeFragment, new PayPalRequest(amount));
    }

    private void postNonceToServer(String nonce, String amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentMethodNonce", nonce);
        data.put("amount", amount);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("processPayment")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> Log.d(TAG, "Payment processed successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to process payment", e));
    }
}
