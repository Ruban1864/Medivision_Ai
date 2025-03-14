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

public class EditMedicineActivity extends AppCompatActivity {
    private static final String TAG = "EditMedicineActivity";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etMedicineName, etDosage, etQuantity;
    private Button btnTime, btnStartDate, btnEndDate, btnSave, btnDelete;
    private Spinner spFrequency;
    private String selectedTime = "", startDate = "", endDate = "";
    private String medicineId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medicine); // Ensure this matches your XML file

        // Enable ActionBar back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Medicine");
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements with logging for debugging
        try {
            etMedicineName = findViewById(R.id.et_medicine_name);
            if (etMedicineName == null) Log.e(TAG, "et_medicine_name not found");
            etDosage = findViewById(R.id.et_dosage);
            if (etDosage == null) Log.e(TAG, "et_dosage not found");
            etQuantity = findViewById(R.id.et_quantity);
            if (etQuantity == null) Log.e(TAG, "et_quantity not found");
            btnTime = findViewById(R.id.btn_time);
            if (btnTime == null) Log.e(TAG, "btn_time not found");
            btnStartDate = findViewById(R.id.btn_start_date);
            if (btnStartDate == null) Log.e(TAG, "btn_start_date not found");
            btnEndDate = findViewById(R.id.btn_end_date);
            if (btnEndDate == null) Log.e(TAG, "btn_end_date not found");
            spFrequency = findViewById(R.id.sp_frequency);
            if (spFrequency == null) Log.e(TAG, "sp_frequency not found");
            btnSave = findViewById(R.id.btn_save);
            if (btnSave == null) Log.e(TAG, "btn_save not found");
            btnDelete = findViewById(R.id.btn_delete);
            if (btnDelete == null) Log.e(TAG, "btn_delete not found");

            // Check if any element is null
            if (etMedicineName == null || etDosage == null || etQuantity == null ||
                    btnTime == null || btnStartDate == null || btnEndDate == null ||
                    spFrequency == null || btnSave == null || btnDelete == null) {
                throw new IllegalStateException("One or more UI elements not found in layout");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "UI initialization error", e);
            finish();
            return;
        }

        // Setup Dosage Frequency Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.frequency_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequency.setAdapter(adapter);

        // Get medicine ID from Intent
        medicineId = getIntent().getStringExtra("MEDICINE_ID");
        if (medicineId == null) {
            Toast.makeText(this, "No medicine selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load existing medicine data
        loadMedicineData();

        // Time Picker
        btnTime.setOnClickListener(v -> pickTime());

        // Start Date Picker
        btnStartDate.setOnClickListener(v -> pickDate(true));

        // End Date Picker
        btnEndDate.setOnClickListener(v -> pickDate(false));

        // Save Button
        btnSave.setOnClickListener(v -> saveMedicineToFirestore());

        // Delete Button
        btnDelete.setOnClickListener(v -> deleteMedicineFromFirestore());
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

    private void loadMedicineData() {
        db.collection("users").document(userId).collection("medicines").document(medicineId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etMedicineName.setText(documentSnapshot.getString("name"));
                        etDosage.setText(documentSnapshot.getString("dosage"));
                        etQuantity.setText(String.valueOf(documentSnapshot.getLong("quantity")));
                        String schedule = documentSnapshot.getString("schedule");
                        if (schedule != null && schedule.contains(" ")) {
                            selectedTime = schedule.split(" ")[1];
                            btnTime.setText("Time: " + selectedTime);
                        }
                        startDate = documentSnapshot.getString("startDate");
                        btnStartDate.setText("Start: " + startDate);
                        endDate = documentSnapshot.getString("endDate");
                        btnEndDate.setText("End: " + endDate);

                        String frequency = documentSnapshot.getString("frequency");
                        if (frequency != null) {
                            for (int i = 0; i < spFrequency.getCount(); i++) {
                                if (spFrequency.getItemAtPosition(i).toString().equals(frequency)) {
                                    spFrequency.setSelection(i);
                                    break;
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Medicine not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Load error", e);
                    finish();
                });
    }

    private void saveMedicineToFirestore() {
        String medicineName = etMedicineName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();
        String frequency = spFrequency.getSelectedItem().toString();

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

        String schedule = startDate + " " + selectedTime;

        Map<String, Object> medicine = new HashMap<>();
        medicine.put("name", medicineName);
        medicine.put("dosage", dosage);
        medicine.put("schedule", schedule);
        medicine.put("startDate", startDate);
        medicine.put("endDate", endDate);
        medicine.put("frequency", frequency);
        medicine.put("quantity", quantityValue);
        medicine.put("refillReminder", true);

        db.collection("users").document(userId).collection("medicines").document(medicineId)
                .set(medicine)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medicine updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Medicine updated: " + medicineId);
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore update error", e);
                });
    }

    private void deleteMedicineFromFirestore() {
        db.collection("users").document(userId).collection("medicines").document(medicineId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medicine deleted successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Medicine deleted: " + medicineId);
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore delete error", e);
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}