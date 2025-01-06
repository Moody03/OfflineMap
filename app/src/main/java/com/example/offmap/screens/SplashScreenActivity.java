package com.example.offmap.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.offmap.R;
import com.example.offmap.screens.signup.SignupActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;

    private ImageView splashLogo;
    private TextView splashName;
    private TextView splashStudentNumber;
    private Animation fadeIn;
    private Animation slideUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init Views
        initViews();


        // Apply animation
        splashLogo.startAnimation(fadeIn);
        splashName.startAnimation(slideUp);
        splashStudentNumber.startAnimation(slideUp);

        // Delay the transition to the MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }

    private void initViews() {
        splashLogo = findViewById(R.id.splash_logo);
        splashName = findViewById(R.id.splash_name);
        splashStudentNumber = findViewById(R.id.splash_student_number);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
    }


}