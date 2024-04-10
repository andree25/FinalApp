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
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.google.firebase.database.annotations.NotNull;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public class DonationFragment extends Fragment implements PaymentMethodNonceCreatedListener {
    private static final String TAG = "DonationFragment";
    private static final String SERVER_BASE_URL = "http://192.168.100.164:3000/";
    private static final String GET_CLIENT_TOKEN_ENDPOINT = "client_token";
    private static final String CHECKOUT_ENDPOINT = "checkout";
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
                // Handle empty amount case
            }
        });

        return view;
    }

    private void fetchClientToken() {
        Log.d(TAG, "fetchClientToken: Fetching client token.");
        OkHttpClient client = new OkHttpClient(); // Initialize OkHttpClient here
        String url = SERVER_BASE_URL + GET_CLIENT_TOKEN_ENDPOINT;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "fetchClientToken - onFailure: Failed to fetch client token", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Log.d(TAG, "fetchClientToken - onResponse: Client token fetched successfully.");
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String clientToken = jsonResponse.getString("clientToken");
                        getActivity().runOnUiThread(() -> setupBraintree(clientToken));
                    } catch (JSONException e) {
                        Log.e(TAG, "fetchClientToken - onResponse: Failed to parse client token JSON", e);
                    }
                } else {
                    Log.e(TAG, "fetchClientToken - onResponse: Failed to fetch client token. Response not successful");
                }
            }
        });
    }

    private void setupBraintree(String clientToken) {
        Log.d(TAG, "setupBraintree: Setting up Braintree with client token.");
        getActivity().runOnUiThread(() -> {
            try {
                mBraintreeFragment = BraintreeFragment.newInstance(getActivity(), clientToken);
                mBraintreeFragment.addListener(this); // You're already adding this listener

                // Adding error listener
                mBraintreeFragment.addListener(new BraintreeErrorListener() {
                    @Override
                    public void onError(Exception error) {
                        if (error instanceof ErrorWithResponse) {
                            ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                            String errorMessage = errorWithResponse.getErrorResponse();
                            Log.e(TAG, "BraintreeError: " + errorMessage);
                        } else {
                            Log.e(TAG, "BraintreeError: " + error.getMessage(), error);
                        }
                    }
                });

                // Adding cancel listener
                mBraintreeFragment.addListener(new BraintreeCancelListener() {
                    @Override
                    public void onCancel(int requestCode) {
                        Log.d(TAG, "User canceled the payment.");
                    }
                });

                Log.d(TAG, "setupBraintree: BraintreeFragment initialized successfully.");
            } catch (InvalidArgumentException e) {
                Log.e(TAG, "setupBraintree: Error initializing BraintreeFragment.", e);
            }
        });
    }


    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        Log.d(TAG, "onPaymentMethodNonceCreated: Nonce created.");
        String nonce = paymentMethodNonce.getNonce();
        String amount = amountEdt.getText().toString().trim();
        if (!amount.isEmpty()) {
            Log.d(TAG, "onPaymentMethodNonceCreated: Posting nonce to server.");
            postNonceToServer(nonce, amount);
        }
    }

    private void initiatePayment(String amount) {
        Log.d(TAG, "initiatePayment: Initiating payment.");
        if (mBraintreeFragment == null) {
            Log.e(TAG, "BraintreeFragment is not initialized.");
            return;
        }
        PayPal.requestOneTimePayment(mBraintreeFragment, new PayPalRequest(amount));
    }

    private void postNonceToServer(String nonce, String amount) {
        Log.d(TAG, "postNonceToServer: Posting nonce to server.");
        OkHttpClient client = new OkHttpClient();
        String url = SERVER_BASE_URL + CHECKOUT_ENDPOINT;

        JSONObject postData = new JSONObject();
        try {
            postData.put("paymentMethodNonce", nonce);
            postData.put("amount", amount);
        } catch (JSONException e) {
            Log.e(TAG, "postNonceToServer: JSON Exception", e);
            return;
        }

        RequestBody body = RequestBody.create(postData.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "postNonceToServer - onFailure: Failed to post nonce to server", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "postNonceToServer - onResponse: Nonce posted successfully, transaction created.");
                } else {
                    Log.e(TAG, "postNonceToServer - onResponse: Failed to post nonce. Response not successful.");
                }
            }
        });
    }
}
