package com.example.meditracker;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
    private String selectedTime = "";
    private String startDate = "";
    private String endDate = "";
    private String medicineId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_edit_medicine);
            Log.d(TAG, "Layout set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage(), e);
            if (e instanceof IllegalStateException && e.getMessage().contains("Theme.AppCompat")) {
                Log.e(TAG, "Theme error: Ensure AndroidManifest.xml or styles.xml uses a Theme.AppCompat theme");
            }
            Toast.makeText(this, "Error loading UI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Enable ActionBar back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Medicine");
        }

        // Initialize Firebase
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (userId == null) {
                Log.d(TAG, "No user logged in, redirecting to LoginActivity");
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            Log.d(TAG, "Firebase initialized, userId: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get medicine ID from Intent
        medicineId = getIntent().getStringExtra("MEDICINE_ID");
        if (medicineId == null) {
            Log.e(TAG, "No medicine ID provided");
            Toast.makeText(this, "No medicine selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI elements
        try {
            etMedicineName = findViewById(R.id.et_medicine_name);
            etDosage = findViewById(R.id.et_dosage);
            etQuantity = findViewById(R.id.et_quantity);
            btnTime = findViewById(R.id.btn_time);
            btnStartDate = findViewById(R.id.btn_start_date);
            btnEndDate = findViewById(R.id.btn_end_date);
            spFrequency = findViewById(R.id.sp_frequency);
            btnSave = findViewById(R.id.btn_save);
            btnDelete = findViewById(R.id.btn_delete);

            if (etMedicineName == null || etDosage == null || etQuantity == null ||
                    btnTime == null || btnStartDate == null || btnEndDate == null ||
                    spFrequency == null || btnSave == null || btnDelete == null) {
                throw new IllegalStateException("One or more UI elements not found in layout");
            }
            Log.d(TAG, "UI elements initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize UI: " + e.getMessage(), e);
            Toast.makeText(this, "UI initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup Dosage Frequency Spinner
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.frequency_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFrequency.setAdapter(adapter);
            Log.d(TAG, "Spinner initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Spinner: " + e.getMessage(), e);
            Toast.makeText(this, "Spinner error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Load existing medicine data
        loadMedicineData();

        // Set button listeners
        btnTime.setOnClickListener(v -> pickTime());
        btnStartDate.setOnClickListener(v -> pickDate(true));
        btnEndDate.setOnClickListener(v -> pickDate(false));
        btnSave.setOnClickListener(v -> saveMedicineToFirestore());
        btnDelete.setOnClickListener(v -> deleteMedicineFromFirestore());

        Log.d(TAG, "onCreate completed successfully");
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
                        Long quantity = documentSnapshot.getLong("quantity");
                        etQuantity.setText(quantity != null ? String.valueOf(quantity) : "");
                        String schedule = documentSnapshot.getString("schedule");
                        if (schedule != null && schedule.contains(" ")) {
                            selectedTime = schedule.split(" ")[1];
                            btnTime.setText("Time: " + selectedTime);
                        }
                        startDate = documentSnapshot.getString("startDate");
                        btnStartDate.setText(startDate != null ? "Start: " + startDate : "Select Start Date");
                        endDate = documentSnapshot.getString("endDate");
                        btnEndDate.setText(endDate != null ? "End: " + endDate : "Select End Date");

                        String frequency = documentSnapshot.getString("frequency");
                        if (frequency != null) {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spFrequency.getAdapter();
                            int position = adapter.getPosition(frequency);
                            if (position >= 0) {
                                spFrequency.setSelection(position);
                            }
                        }
                        Log.d(TAG, "Medicine data loaded successfully");
                    } else {
                        Log.w(TAG, "Medicine document not found");
                        Toast.makeText(this, "Medicine not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load medicine data: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void saveMedicineToFirestore() {
        String medicineName = etMedicineName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String frequency = spFrequency.getSelectedItem() != null ? spFrequency.getSelectedItem().toString() : "";

        if (medicineName.isEmpty() || dosage.isEmpty() || quantityStr.isEmpty() || selectedTime.isEmpty() ||
                startDate.isEmpty() || endDate.isEmpty() || frequency.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: One or more fields are empty");
            return;
        }

        int quantityValue;
        try {
            quantityValue = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Quantity parsing error: " + e.getMessage(), e);
            return;
        }

        String schedule = startDate + " " + selectedTime;

        // Use parameterized Map to avoid unchecked warning
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
                    updateAlarm(medicineId, schedule, medicineName, frequency);
                    Toast.makeText(this, "Medicine updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Medicine updated: " + medicineId);
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating medicine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore update error: " + e.getMessage(), e);
                });
    }

    private void deleteMedicineFromFirestore() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicineId", medicineId);

        // Use modern PendingIntent flags with API check
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, medicineId.hashCode(), intent, flags);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm cancelled for medicineId: " + medicineId);
        }

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
                    Log.e(TAG, "Firestore delete error: " + e.getMessage(), e);
                });
    }

    @SuppressLint("ScheduleExactAlarm")
    private void updateAlarm(String medicineId, String schedule, String medicineName, String frequency) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            Toast.makeText(this, "Cannot set alarm: Alarm service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicineId", medicineId);
        intent.putExtra("medicineName", medicineName);

        // Use modern PendingIntent flags with API check
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, medicineId.hashCode(), intent, flags);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(schedule));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Set alarm 1 hour before the scheduled time
            Calendar alarmTime = (Calendar) calendar.clone();
            alarmTime.add(Calendar.HOUR_OF_DAY, -1);

            // Adjust for past alarms based on frequency
            Calendar now = Calendar.getInstance();
            while (alarmTime.before(now)) {
                if ("Daily".equals(frequency)) {
                    alarmTime.add(Calendar.DAY_OF_YEAR, 1);
                } else if ("Weekly".equals(frequency)) {
                    alarmTime.add(Calendar.WEEK_OF_YEAR, 1);
                } else if ("Monthly".equals(frequency)) {
                    alarmTime.add(Calendar.MONTH, 1);
                } else {
                    break; // One-time alarm, don’t reschedule if past
                }
            }

            // Use modern AlarmManager API with compatibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarms not permitted; requesting permission");
                Toast.makeText(this, "Please allow exact alarm permission", Toast.LENGTH_LONG).show();
                Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(permissionIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Alarm updated for " + medicineName + " at " + sdf.format(alarmTime.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating alarm: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating alarm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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