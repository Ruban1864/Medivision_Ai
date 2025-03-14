package com.example.meditracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMedicineActivity extends AppCompatActivity {
    private static final String TAG = "AddMedicineActivity";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etMedicineName, etDosage, etQuantity;
    private Button btnTime, btnStartDate, btnEndDate, btnSave, btnBack; // Added btnBack
    private Spinner spFrequency;
    private String selectedTime = "", startDate = "", endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // Enable ActionBar back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Medicine");
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        etMedicineName = findViewById(R.id.et_medicine_name);
        etDosage = findViewById(R.id.et_dosage);
        etQuantity = findViewById(R.id.et_quantity);
        btnTime = findViewById(R.id.btn_time);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        spFrequency = findViewById(R.id.sp_frequency);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back); // Initialize on-screen back button

        // Setup Dosage Frequency Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.frequency_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequency.setAdapter(adapter);

        // Time Picker
        btnTime.setOnClickListener(v -> pickTime());

        // Start Date Picker
        btnStartDate.setOnClickListener(v -> pickDate(true));

        // End Date Picker
        btnEndDate.setOnClickListener(v -> pickDate(false));

        // Save Button
        btnSave.setOnClickListener(v -> saveMedicineToFirestore());

        // On-screen Back Button
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "On-screen back button clicked");
            finish(); // Return to previous activity
        });
    }

    private void pickTime() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            btnTime.setText("Time: " + selectedTime);
            Log.d(TAG, "Selected Time: " + selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void pickDate(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());

            if (isStartDate) {
                startDate = formattedDate;
                btnStartDate.setText("Start: " + startDate);
                Log.d(TAG, "Selected Start Date: " + startDate);
            } else {
                endDate = formattedDate;
                btnEndDate.setText("End: " + endDate);
                Log.d(TAG, "Selected End Date: " + endDate);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveMedicineToFirestore() {
        String medicineName = etMedicineName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();
        String frequency = spFrequency.getSelectedItem().toString();

        // Validation
        if (medicineName.isEmpty() || dosage.isEmpty() || selectedTime.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || quantity.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: One or more fields are empty");
            return;
        }

        int quantityValue;
        try {
            quantityValue = Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Quantity parsing error", e);
            return;
        }

        // Combine date and time for schedule
        String schedule = startDate + " " + selectedTime;

        // Prepare Firestore data
        Map<String, Object> medicine = new HashMap<>();
        medicine.put("name", medicineName);
        medicine.put("dosage", dosage);
        medicine.put("schedule", schedule);
        medicine.put("startDate", startDate);
        medicine.put("endDate", endDate);
        medicine.put("frequency", frequency);
        medicine.put("quantity", quantityValue);
        medicine.put("refillReminder", true);

        // Get current user ID
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "User ID is null");
            return;
        }

        // Save to Firestore
        db.collection("users").document(userId).collection("medicines")
                .add(medicine)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Medicine added successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Medicine added with ID: " + documentReference.getId());
                    setResult(RESULT_OK); // Set result to indicate success
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore error", e);
                });
    }

    // Handle ActionBar back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "ActionBar back button clicked");
            finish(); // Return to previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Handle device back button
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Device back button pressed");
        finish(); // Return to previous activity
        super.onBackPressed(); // Optional, can be removed if finish() is sufficient
    }
}