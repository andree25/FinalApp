package com.app.finalapp.ui.donation;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DonationFragment extends Fragment implements PaymentMethodNonceCreatedListener {
    private static final String TAG = "DonationFragment";
    private BraintreeFragment mBraintreeFragment;
    private EditText amountEdt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Initializing DonationFragment view.");
        View view = inflater.inflate(R.layout.fragment_donation, container, false);
        amountEdt = view.findViewById(R.id.amountEdt);
        Button donateButton = view.findViewById(R.id.donateButton);

        fetchClientToken();

        donateButton.setOnClickListener(v -> {
            String amount = amountEdt.getText().toString().trim();
            if (!amount.isEmpty()) {
                Log.d(TAG, "onClick: Initiating payment with amount: " + amount);
                initiatePayment(amount);
            } else {
                Log.e(TAG, "onClick: Amount is empty.");
            }
        });

        return view;
    }

    private void fetchClientToken() {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("generateClientToken")
                .call()
                .addOnSuccessListener(httpsCallableResult -> {
                    // Here, 'httpsCallableResult.getData()' returns an Object which is actually a Map
                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();
                    // Now, extract the clientToken from the Map
                    String clientToken = (String) result.get("clientToken");
                    setupBraintree(clientToken);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch client token", e));
    }

    private void setupBraintree(String clientToken) {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(getActivity(), clientToken);
            mBraintreeFragment.addListener(this);
            mBraintreeFragment.addListener((BraintreeErrorListener) error -> {
                if (error instanceof Exception) {
                    Log.e(TAG, "BraintreeError: ", error);
                }
            });
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
        // Prepare data to send
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
