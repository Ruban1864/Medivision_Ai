package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChooseDoctorActivity extends AppCompatActivity {

    private static final String TAG = "ChooseDoctorActivity";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    protected Button btnClose;
    private ListView listView;

    private final List<String> doctorNames = new ArrayList<>();
    private final List<String> doctorIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_doctor);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Button btnClose = findViewById(R.id.btn_close);
        listView = findViewById(R.id.list_doctors);


        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "User not authenticated",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDoctors();

        btnClose.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void loadDoctors() {

        db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .addOnSuccessListener(query -> {

                    doctorNames.clear();
                    doctorIds.clear();

                    if (query.isEmpty()) {
                        Toast.makeText(this,
                                "No doctors available",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : query) {

                        String name = doc.getString("fullName");

                        if (name != null) {
                            doctorNames.add(name);
                            doctorIds.add(doc.getId());
                        }
                    }

                    // Using custom row layout (doctor_item.xml)
                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    this,
                                    R.layout.doctor_item,
                                    R.id.tv_doctor_name,
                                    doctorNames);

                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener(
                            (parent, view, position, id) -> {

                                if (position >= doctorIds.size()) return;

                                String selectedDoctorId =
                                        doctorIds.get(position);

                                assignDoctor(selectedDoctorId);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Doctor load failed", e);
                    Toast.makeText(this,
                            "Failed to load doctors",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void assignDoctor(String doctorId) {

        String patientId =
                auth.getCurrentUser().getUid();

        db.collection("users")
                .document(patientId)
                .update("assignedDoctorId", doctorId)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this,
                            "Doctor assigned successfully",
                            Toast.LENGTH_SHORT).show();

                    finish(); // return to MainActivity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Assignment failed", e);
                    Toast.makeText(this,
                            "Assignment failed",
                            Toast.LENGTH_SHORT).show();
                });
    }
}