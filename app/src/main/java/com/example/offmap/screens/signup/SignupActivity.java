package com.example.offmap.screens.signup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.offmap.MainActivity;
import com.example.offmap.R;
import com.example.offmap.screens.login.LoginActivity;
import com.example.offmap.services.SQLiteManager;
import com.example.offmap.viewmodels.SignupViewModel;
import com.example.offmap.viewmodels.SignupViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {



    private TextInputEditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private MaterialButton signUp, login;


    private SignupViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Initialize ViewModel
        SQLiteManager sqLiteManager = new SQLiteManager(this);
        SignupViewModelFactory factory = new SignupViewModelFactory(sqLiteManager);
        viewModel = new ViewModelProvider(this, factory).get(SignupViewModel.class);


        initView();
        observeViewModel();
        setupListeners();
    }

    private void initView() {
       nameInput = findViewById(R.id.name);
       emailInput = findViewById(R.id.email);
       passwordInput = findViewById(R.id.password);
       confirmPasswordInput = findViewById(R.id.confirmPassword);
       signUp = findViewById(R.id.signupButton);
       login = findViewById(R.id.loginButton);
    }

    private void observeViewModel() {
        viewModel.getToastMessage().observe(this, this::showToast);
        viewModel.getUserCreationSuccess().observe(this, success -> {
            if (success) {
                navigateToLoginActivity();
            }
        });
    }

    private void setupListeners() {
        if (isUserLoggedIn()) {
            navigateToMainScreen();
            return;
        }


        signUp.setOnClickListener(v -> signUpUser());
        login.setOnClickListener(v -> navigateToLoginActivity());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void signUpUser() {
        String fullName = Objects.requireNonNull(nameInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

        if (!validateInputs(fullName, email, password, confirmPassword)) return;


        viewModel.signUp(fullName, email, password);
    }

    public boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email)
        || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("Please fill all fields.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showToast("Passwords do not match.");
            return false;
        }

        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return preferences.getBoolean("is_logged_in", false);
    }

    private void navigateToMainScreen() {
        // Navigate to the main activity or dashboard
        Intent intent = new Intent(this, MainActivity.class); // Replace with actual MainActivity class
        startActivity(intent);
        finish();
    }
    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}