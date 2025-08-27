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

public class StartupActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

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
}
