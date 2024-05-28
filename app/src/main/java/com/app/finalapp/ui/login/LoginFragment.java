package com.app.finalapp.ui.login;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.app.finalapp.R;
import com.app.finalapp.ui.BaseFragment;

public class LoginFragment extends BaseFragment {
    private static final String TAG = "LoginFragment";
    private LoginViewModel loginViewModel;
    private EditText emailLogin, passwordLogin;
    private AppCompatImageView togglePassword;
    private View rootView;
    private NavController navController;
    private int targetFragmentId;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "Fragment attached to activity");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_login, container, false);
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        initUI();
        observeViewModel();

        return rootView;
    }

    private void initUI() {
        togglePassword = rootView.findViewById(R.id.togglePassword);
        emailLogin = rootView.findViewById(R.id.email_login);
        passwordLogin = rootView.findViewById(R.id.password_login);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        if (getArguments() != null) {
            targetFragmentId = getArguments().getInt("targetFragmentId");
        }

        togglePassword.setOnClickListener(view -> {
            int inputType = passwordLogin.getInputType();
            int cursorPosition = passwordLogin.getSelectionStart();
            int newInputType = inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            passwordLogin.setInputType(newInputType);
            passwordLogin.setSelection(cursorPosition);
        });

        Button loginButton = rootView.findViewById(R.id.button_login);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                Log.d(TAG, "Login button clicked");
                if (isValidInputs()) {
                    loginViewModel.loginUser(emailLogin.getText().toString(), passwordLogin.getText().toString(), requireContext());
                } else {
                    Toast.makeText(requireContext(), "Invalid email or password format", Toast.LENGTH_LONG).show();
                }
            });
        }

        Button registerButton = rootView.findViewById(R.id.button_register);
        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                Bundle args = new Bundle();
                if (getArguments() != null) {
                    int targetFragmentId = getArguments().getInt("targetFragmentId", 0);
                    args.putInt("targetFragmentId", targetFragmentId);
                }
                Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_register, args);
            });
        }
    }

    private void observeViewModel() {
        loginViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoadingIndicator();
            } else {
                hideLoadingIndicator();
            }
        });

        loginViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        loginViewModel.getIsAdmin().observe(getViewLifecycleOwner(), isAdmin -> {
            if (isAdmin) {
                navigateToAdminArea();
            }
        });

        loginViewModel.getLoginSuccess().observe(getViewLifecycleOwner(), loginSuccess -> {
            if (loginSuccess) {
                if (targetFragmentId != 0) {
                    navController.navigate(targetFragmentId);
                } else {
                    navController.navigate(R.id.nav_home);
                }
            }
        });
    }

    private boolean isValidInputs() {
        return loginViewModel.isValidEmail(emailLogin.getText().toString()) && loginViewModel.isValidPassword(passwordLogin.getText().toString());
    }

    private void navigateToAdminArea() {
        navController.navigate(R.id.nav_admin);
    }

    private void showLoadingIndicator() {
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        ProgressBar loadingIndicator = rootView.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.GONE);
    }
}
