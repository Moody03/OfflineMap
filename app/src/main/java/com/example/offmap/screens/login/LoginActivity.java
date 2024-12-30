package com.example.offmap.screens.login;



import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;


import com.example.offmap.MainActivity;
import com.example.offmap.R;
import com.example.offmap.screens.signup.SignupActivity;
import com.example.offmap.services.SQLiteManager;
import com.example.offmap.viewmodels.LoginViewModel;
import com.example.offmap.viewmodels.LoginViewModelFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private TextInputEditText emailInput, passwordInput;
    private MaterialButton login, signup;


    private LoginViewModel loginViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SQLiteManager sqLiteManager = new SQLiteManager(this);
        LoginViewModelFactory factory = new LoginViewModelFactory(sqLiteManager);
        loginViewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        initViews();
        setupObserver();
        setupListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        login = findViewById(R.id.loginButton);
        signup = findViewById(R.id.signupButton);
    }

    private void setupObserver() {
        loginViewModel.getToastMessage().observe(this, this::showToast);

        loginViewModel.getLoggedInUser().observe(this, user -> {
            if (user != null) {
                Log.d("LoginActivity", "Logged in as: "+ user.getFullName());
                markUserLoggedIn();
                navigateToMainScreen();
            }
        });
    }

    private void setupListeners() {
        login.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please enter email and password.", Toast.LENGTH_SHORT).show();
            } else {
                loginViewModel.login(email, password);
            }
        });

        signup.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToMainScreen() {
        // Navigate to the main activity or dashboard
        Intent intent = new Intent(this, MainActivity.class); // Replace with actual MainActivity class
        startActivity(intent);
        finish();
    }


    private void markUserLoggedIn() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
    }



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}