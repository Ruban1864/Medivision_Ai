package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etFullName, etEmail, etPassword,
            etConfirmPassword, etAge, etPhoneNumber;

    private RadioGroup rgRole;

    private String selectedRole = "patient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etAge = findViewById(R.id.et_age);
        etPhoneNumber = findViewById(R.id.et_phone_number);

        rgRole = findViewById(R.id.rg_role);

        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLogin = findViewById(R.id.tv_login);

        setupPasswordToggle();
        setupRoleSelection();

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void setupRoleSelection() {

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = findViewById(checkedId);
            selectedRole = selected.getText().toString().toLowerCase();
        });
    }

    private void registerUser() {

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword =
                etConfirmPassword.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String phoneNumber =
                etPhoneNumber.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() ||
                ageStr.isEmpty() || phoneNumber.isEmpty()) {

            Toast.makeText(this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Age must be a number",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        String userId =
                                auth.getCurrentUser().getUid();

                        Map<String, Object> userData =
                                new HashMap<>();

                        userData.put("fullName", fullName);
                        userData.put("email", email);
                        userData.put("age", age);
                        userData.put("phoneNumber", phoneNumber);
                        userData.put("role", selectedRole);

                        db.collection("users")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {

                                    Toast.makeText(this,
                                            "Registration successful",
                                            Toast.LENGTH_SHORT).show();

                                    if (selectedRole.equals("doctor")) {
                                        startActivity(new Intent(
                                                this,
                                                DoctorDashboardActivity.class));
                                    } else {
                                        startActivity(new Intent(
                                                this,
                                                MainActivity.class));
                                    }

                                    finish();
                                });

                    } else {
                        Toast.makeText(this,
                                "Registration failed: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
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

        etConfirmPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etConfirmPassword.getRight()
                        - etConfirmPassword.getCompoundDrawables()[2]
                        .getBounds().width())) {
                    togglePasswordVisibility(etConfirmPassword);
                    return true;
                }
            }
            return false;
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