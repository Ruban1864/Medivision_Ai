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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GithubAuthProvider;
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
        Log.d(TAG, "LoginActivity started");

        auth = FirebaseAuth.getInstance();

        try {
            etEmail = findViewById(R.id.et_email);
            etPassword = findViewById(R.id.et_password);
            btnLogin = findViewById(R.id.btn_login);
            btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
            btnGithubSignIn = findViewById(R.id.btn_github_sign_in); // Initialize GitHub button
            tvForgotPassword = findViewById(R.id.tv_forgot_password);
            tvRegister = findViewById(R.id.tv_register);

            if (etEmail == null || etPassword == null || btnLogin == null || btnGoogleSignIn == null ||
                    btnGithubSignIn == null || tvForgotPassword == null || tvRegister == null) {
                throw new NullPointerException("One or more UI elements not found");
            }
            Log.d(TAG, "UI elements initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize UI: " + e.getMessage(), e);
            Toast.makeText(this, "UI error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Password visibility toggle
        etPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[2].getBounds().width())) {
                    togglePasswordVisibility(etPassword);
                    return true;
                }
            }
            return false;
        });

        // Email/Password Login
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email/Password Login successful");
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Email/Password Login failed: " + task.getException().getMessage());
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Google Sign-In (Placeholder - Replace with actual Google Sign-In logic if needed)
        btnGoogleSignIn.setOnClickListener(v -> {
            Log.d(TAG, "Google Sign-In clicked");
            Toast.makeText(this, "Google Sign-In feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // GitHub Sign-In
        btnGithubSignIn.setOnClickListener(v -> {
            Log.d(TAG, "GitHub Sign-In clicked");
            signInWithGitHub();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Log.d(TAG, "Forgot Password clicked");
            Toast.makeText(this, "Forgot Password feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        tvRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register link clicked");
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility(EditText editText) {
        boolean isPasswordVisible = editText.getTransformationMethod() == null;
        if (isPasswordVisible) {
            editText.setTransformationMethod(new PasswordTransformationMethod());
            editText.setCompoundDrawablesWithIntrinsicBounds(
                    editText.getCompoundDrawables()[0],
                    null,
                    getResources().getDrawable(R.drawable.ic_eye_closed),
                    null
            );
        } else {
            editText.setTransformationMethod(null);
            editText.setCompoundDrawablesWithIntrinsicBounds(
                    editText.getCompoundDrawables()[0],
                    null,
                    getResources().getDrawable(R.drawable.ic_eye_open),
                    null
            );
        }
        editText.setSelection(editText.getText().length());
    }

    private void signInWithGitHub() {
        // Configure OAuthProvider for GitHub
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");

        // Optional: Specify additional scopes (e.g., email access)
        provider.addCustomParameter("scope", "user:email");

        // Start the GitHub sign-in flow
        auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "GitHub Sign-In successful");
                    OAuthCredential credential = (OAuthCredential) authResult.getCredential();
                    String accessToken = credential.getAccessToken();
                    // Securely handle the access token (e.g., store in Firebase or your backend)
                    updateUI(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "GitHub Sign-In failed: " + e.getMessage());
                    Toast.makeText(LoginActivity.this, "GitHub Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(com.google.firebase.auth.FirebaseUser user) {
        if (user != null) {
            Log.d(TAG, "User signed in: " + user.getDisplayName());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}