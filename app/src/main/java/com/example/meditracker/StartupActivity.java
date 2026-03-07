package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StartupActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        requestNotificationPermission();
        auth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_bar);
        buttonContainer = findViewById(R.id.button_container);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        // If already logged in, skip to MainActivity
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Show loading for 2 seconds, then show buttons
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            buttonContainer.setVisibility(View.VISIBLE);
        }, 2000); // 2 seconds

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }
}
