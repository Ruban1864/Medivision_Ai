package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_ADD_MEDICINE = 1;
    private static final int REQUEST_CODE_EDIT_MEDICINE = 2;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvWelcome, tvExpiryAlerts;
    private RecyclerView rvMedicineReminders;
    private Button btnAddMedicine, btnEditMedicine, btnSettings, btnLogout;
    private MedicineAdapter medicineAdapter;
    private List<Medicine> medicineList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        // Set content view with theme error handling
        try {
            setContentView(R.layout.activity_main);
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

        // Initialize Firebase
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check user authentication
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        Log.d(TAG, "User logged in: " + user.getUid());

        // Initialize UI elements
        try {
            tvWelcome = findViewById(R.id.tv_welcome);
            tvExpiryAlerts = findViewById(R.id.tv_expiry_alerts);
            rvMedicineReminders = findViewById(R.id.rv_medicine_reminders);
            btnAddMedicine = findViewById(R.id.btn_add_medicine);
            btnEditMedicine = findViewById(R.id.btn_edit_medicine);
            btnSettings = findViewById(R.id.btn_settings);
            btnLogout = findViewById(R.id.btn_logout);

            if (tvWelcome == null || tvExpiryAlerts == null || rvMedicineReminders == null ||
                    btnAddMedicine == null || btnEditMedicine == null || btnSettings == null || btnLogout == null) {
                throw new NullPointerException("One or more UI elements not found");
            }
            Log.d(TAG, "UI elements initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize UI: " + e.getMessage(), e);
            Toast.makeText(this, "UI error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set welcome text
        tvWelcome.setText("Welcome, " + (user.getEmail() != null ? user.getEmail() : "User"));

        // Initialize RecyclerView
        medicineList = new ArrayList<>();
        try {
            medicineAdapter = new MedicineAdapter(medicineList, this::launchEditMedicineActivity);
            if (medicineAdapter == null) {
                throw new NullPointerException("MedicineAdapter creation failed");
            }
            rvMedicineReminders.setLayoutManager(new LinearLayoutManager(this));
            rvMedicineReminders.setAdapter(medicineAdapter);
            Log.d(TAG, "RecyclerView initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RecyclerView: " + e.getMessage(), e);
            Toast.makeText(this, "RecyclerView error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Load medicine data
        loadMedicineData(user.getUid());

        // Set button listeners
        btnAddMedicine.setOnClickListener(v -> {
            Log.d(TAG, "Add Medicine button clicked");
            startActivityForResult(new Intent(this, AddMedicineActivity.class), REQUEST_CODE_ADD_MEDICINE);
        });

        btnEditMedicine.setOnClickListener(v -> {
            Log.d(TAG, "Edit Medicine button clicked");
            Toast.makeText(this, "Click a medicine in the list to edit", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            Log.d(TAG, "Settings button clicked");
            Toast.makeText(this, "Settings feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked");
            auth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        Log.d(TAG, "onCreate completed successfully");
    }

    private void launchEditMedicineActivity(String medicineId) {
        Log.d(TAG, "Launching EditMedicineActivity for medicineId: " + medicineId);
        Intent intent = new Intent(this, EditMedicineActivity.class);
        intent.putExtra("MEDICINE_ID", medicineId);
        startActivityForResult(intent, REQUEST_CODE_EDIT_MEDICINE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if ((requestCode == REQUEST_CODE_ADD_MEDICINE || requestCode == REQUEST_CODE_EDIT_MEDICINE)
                && resultCode == RESULT_OK) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                loadMedicineData(user.getUid());
            } else {
                Log.w(TAG, "User null in onActivityResult, redirecting to LoginActivity");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        }
    }

    private void loadMedicineData(String userId) {
        Log.d(TAG, "Loading medicine data for user: " + userId);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users").document(userId).collection("medicines")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        medicineList.clear();
                        StringBuilder expiryAlerts = new StringBuilder();
                        boolean hasExpiryAlerts = false;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String schedule = document.getString("schedule");
                            String startDate = document.getString("startDate");
                            String endDate = document.getString("endDate");
                            String medicineId = document.getId();

                            if (name == null || schedule == null || startDate == null || endDate == null) {
                                Log.w(TAG, "Incomplete medicine data for ID: " + medicineId);
                                continue;
                            }

                            if (schedule.startsWith(today)) {
                                boolean isCompleted = document.getBoolean("completed") != null ? document.getBoolean("completed") : false;
                                medicineList.add(new Medicine(name, schedule, startDate, endDate, medicineId, isCompleted));
                            }

                            if (isExpiringSoon(endDate)) {
                                expiryAlerts.append("- ").append(name).append(" (Expires: ").append(endDate).append(")\n");
                                hasExpiryAlerts = true;
                            }
                        }

                        medicineAdapter.notifyDataSetChanged();
                        tvExpiryAlerts.setText(hasExpiryAlerts ? "Upcoming Expiry Alerts:\n" + expiryAlerts.toString() : "No upcoming expiries.");
                        Log.d(TAG, "Medicine data loaded successfully, items: " + medicineList.size());
                    } else {
                        Log.e(TAG, "Failed to load medicine data: " + task.getException().getMessage(), task.getException());
                        Toast.makeText(this, "Failed to load data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isExpiringSoon(String expiryDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expiry = sdf.parse(expiryDate);
            Date today = new Date();
            long diff = expiry.getTime() - today.getTime();
            long daysDiff = diff / (1000 * 60 * 60 * 24);
            return daysDiff <= 7 && daysDiff >= 0;
        } catch (Exception e) {
            Log.w(TAG, "Error parsing expiry date: " + expiryDate + ", " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "User logged out unexpectedly, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}

class Medicine {
    private final String name;
    private final String schedule;
    private final String startDate;
    private final String endDate;
    private final String medicineId;
    private boolean isCompleted;

    public Medicine(String name, String schedule, String startDate, String endDate, String medicineId, boolean isCompleted) {
        this.name = name;
        this.schedule = schedule;
        this.startDate = startDate;
        this.endDate = endDate;
        this.medicineId = medicineId;
        this.isCompleted = isCompleted;
    }

    public String getName() { return name; }
    public String getSchedule() { return schedule; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getMedicineId() { return medicineId; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
}

class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
    private final List<Medicine> medicines;
    private final OnMedicineClickListener listener;

    public interface OnMedicineClickListener {
        void onMedicineClick(String medicineId);
    }

    public MedicineAdapter(List<Medicine> medicines, OnMedicineClickListener listener) {
        this.medicines = medicines;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.medicine_item, parent, false);
            return new ViewHolder(view);
        } catch (Exception e) {
            Log.e("MedicineAdapter", "Failed to inflate item layout: " + e.getMessage(), e);
            throw new RuntimeException("Adapter layout inflation failed", e);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            Medicine medicine = medicines.get(position);
            holder.tvMedicineName.setText(medicine.getName() != null ? medicine.getName() : "Unnamed");
            holder.tvStartDate.setText("Start: " + (medicine.getStartDate() != null ? medicine.getStartDate() : "N/A"));
            holder.tvEndDate.setText("End: " + (medicine.getEndDate() != null ? medicine.getEndDate() : "N/A"));
            String dueTime = convertTo12HourFormat(medicine.getSchedule());
            holder.tvDueTime.setText("Due: " + (dueTime != null ? dueTime : "N/A"));
            updateCompletionStatus(holder, medicine.isCompleted());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null && medicine.getMedicineId() != null) {
                    listener.onMedicineClick(medicine.getMedicineId());
                }
            });
            holder.btnCompletionStatus.setOnClickListener(v -> {
                medicine.setCompleted(!medicine.isCompleted());
                updateCompletionStatus(holder, medicine.isCompleted());
                // Optionally update Firestore here if you want to persist the status
                // db.collection("users").document(userId).collection("medicines").document(medicine.getMedicineId())
                //     .update("completed", medicine.isCompleted());
            });
        } catch (Exception e) {
            Log.e("MedicineAdapter", "Error binding view holder at position " + position + ": " + e.getMessage(), e);
        }
    }

    private void updateCompletionStatus(ViewHolder holder, boolean isCompleted) {
        if (isCompleted) {
            holder.btnCompletionStatus.setText("Completed");
            holder.btnCompletionStatus.setTextColor(0xFF4CAF50); // Green
        } else {
            holder.btnCompletionStatus.setText("Incompleted");
            holder.btnCompletionStatus.setTextColor(0xFFF44336); // Red
        }
    }

    @Override
    public int getItemCount() {
        return medicines != null ? medicines.size() : 0;
    }

    // Method to convert 24-hour time to 12-hour format with AM/PM
    private String convertTo12HourFormat(String schedule) {
        if (schedule == null) return null;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = inputFormat.parse(schedule);
            return outputFormat.format(date); // e.g., "8:00 AM" or "3:30 PM"
        } catch (ParseException e) {
            Log.w("MedicineAdapter", "Error parsing schedule time: " + schedule + ", " + e.getMessage());
            return schedule; // Return original string if parsing fails
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMedicineName;
        final TextView tvStartDate;
        final TextView tvEndDate;
        final TextView tvDueTime;
        final Button btnCompletionStatus;

        ViewHolder(View itemView) {
            super(itemView);
            try {
                tvMedicineName = itemView.findViewById(R.id.et_medicine_name);
                tvStartDate = itemView.findViewById(R.id.btn_start_date);
                tvEndDate = itemView.findViewById(R.id.btn_end_date);
                tvDueTime = itemView.findViewById(R.id.tv_due_time);
                btnCompletionStatus = itemView.findViewById(R.id.btn_completion_status);
                if (tvMedicineName == null || tvStartDate == null || tvEndDate == null || tvDueTime == null || btnCompletionStatus == null) {
                    throw new NullPointerException("Text views or button not found in item layout");
                }
            } catch (Exception e) {
                Log.e("MedicineAdapter", "Failed to initialize ViewHolder: " + e.getMessage(), e);
                throw e;
            }
        }
    }
}