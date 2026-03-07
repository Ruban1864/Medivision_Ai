package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meditracker.utils.RoleManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth auth;

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn, btnGithubSignIn;
    private TextView tvForgotPassword, tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        btnGithubSignIn = findViewById(R.id.btn_github_sign_in);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);

        setupPasswordToggle();
        setupLogin();
        setupGithubLogin();

        btnGoogleSignIn.setOnClickListener(v ->
                Toast.makeText(this,
                        "Google Sign-In coming soon!",
                        Toast.LENGTH_SHORT).show());

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this,
                        "Forgot Password feature coming soon!",
                        Toast.LENGTH_SHORT).show());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void setupPasswordToggle() {

        etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight()
                        - etPassword.getCompoundDrawables()[2]
                        .getBounds().width())) {
                    togglePasswordVisibility(etPassword);
                    return true;
                }
            }
            return false;
        });
    }

    private void setupLogin() {

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            Log.d(TAG, "Login successful");

                            // 🔥 Role-Based Routing
                            RoleManager.routeUser(LoginActivity.this);

                            finish();

                        } else {
                            Toast.makeText(this,
                                    "Login failed: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setupGithubLogin() {

        btnGithubSignIn.setOnClickListener(v -> {

            OAuthProvider.Builder provider =
                    OAuthProvider.newBuilder("github.com");

            provider.addCustomParameter("scope", "user:email");

            auth.startActivityForSignInWithProvider(
                            this,
                            provider.build())
                    .addOnSuccessListener(authResult -> {

                        OAuthCredential credential =
                                (OAuthCredential) authResult.getCredential();

                        Log.d(TAG,
                                "GitHub Login successful");

                        // 🔥 Role-Based Routing
                        RoleManager.routeUser(LoginActivity.this);

                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "GitHub Login failed: "
                                            + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });
    }

    private void togglePasswordVisibility(EditText editText) {

        boolean isVisible =
                editText.getTransformationMethod() == null;

        if (isVisible) {
            editText.setTransformationMethod(
                    new PasswordTransformationMethod());
        } else {
            editText.setTransformationMethod(null);
        }

        editText.setSelection(
                editText.getText().length());
    }
}