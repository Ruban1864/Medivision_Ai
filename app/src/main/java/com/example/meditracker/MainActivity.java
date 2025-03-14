package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_MEDICINE = 1;
    private static final int REQUEST_CODE_EDIT_MEDICINE = 2; // Added for edit
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
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvWelcome = findViewById(R.id.tv_welcome);
        tvExpiryAlerts = findViewById(R.id.tv_expiry_alerts);
        rvMedicineReminders = findViewById(R.id.rv_medicine_reminders);
        btnAddMedicine = findViewById(R.id.btn_add_medicine);
        btnEditMedicine = findViewById(R.id.btn_edit_medicine);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout);

        tvWelcome.setText("Welcome, " + user.getEmail());

        medicineList = new ArrayList<>();
        medicineAdapter = new MedicineAdapter(medicineList, this::launchEditMedicineActivity); // Pass click listener
        rvMedicineReminders.setLayoutManager(new LinearLayoutManager(this));
        rvMedicineReminders.setAdapter(medicineAdapter);

        loadMedicineData(user.getUid());

        btnAddMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_MEDICINE);
        });

        // Optionally keep btnEditMedicine as a hint, or remove it if editing is via RecyclerView
        btnEditMedicine.setOnClickListener(v -> {
            Toast.makeText(this, "Click a medicine in the list to edit", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void launchEditMedicineActivity(String medicineId) {
        Intent intent = new Intent(this, EditMedicineActivity.class);
        intent.putExtra("MEDICINE_ID", medicineId);
        startActivityForResult(intent, REQUEST_CODE_EDIT_MEDICINE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ADD_MEDICINE || requestCode == REQUEST_CODE_EDIT_MEDICINE)
                && resultCode == RESULT_OK) {
            loadMedicineData(auth.getCurrentUser().getUid());
        }
    }

    private void loadMedicineData(String userId) {
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
                            String medicineId = document.getId(); // Get document ID

                            if (schedule != null && schedule.startsWith(today)) {
                                medicineList.add(new Medicine(name, schedule, startDate, endDate, medicineId));
                            }

                            if (endDate != null && isExpiringSoon(endDate)) {
                                expiryAlerts.append("- ").append(name).append(" (Expires: ").append(endDate).append(")\n");
                                hasExpiryAlerts = true;
                            }
                        }

                        medicineAdapter.notifyDataSetChanged();
                        tvExpiryAlerts.setText(hasExpiryAlerts ? "Upcoming Expiry Alerts:\n" + expiryAlerts.toString() : "No upcoming expiries.");
                    } else {
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
            return false;
        }
    }
}

class Medicine {
    private String name;
    private String schedule;
    private String startDate;
    private String endDate;
    private String medicineId; // Added to store document ID

    public Medicine(String name, String schedule, String startDate, String endDate, String medicineId) {
        this.name = name;
        this.schedule = schedule;
        this.startDate = startDate;
        this.endDate = endDate;
        this.medicineId = medicineId;
    }

    public String getName() { return name; }
    public String getSchedule() { return schedule; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getMedicineId() { return medicineId; } // Getter for medicineId
}

class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
    private List<Medicine> medicines;
    private OnMedicineClickListener listener;

    // Interface for click handling
    public interface OnMedicineClickListener {
        void onMedicineClick(String medicineId);
    }

    public MedicineAdapter(List<Medicine> medicines, OnMedicineClickListener listener) {
        this.medicines = medicines;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Medicine medicine = medicines.get(position);
        holder.text1.setText(medicine.getName());
        holder.text2.setText("Start: " + medicine.getStartDate() + " | Due: " + medicine.getSchedule() + " | End: " + medicine.getEndDate());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMedicineClick(medicine.getMedicineId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}