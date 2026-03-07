package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DoctorDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerReports;
    private ProgressBar progressBar;
    private TextView txtEmpty;

    private FirebaseFirestore db;
    private String doctorId;

    private List<PatientModel> patientList;
    private PatientAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        recyclerReports = findViewById(R.id.recyclerReports);
        progressBar = findViewById(R.id.progressBar);
        txtEmpty = findViewById(R.id.txtEmpty);
        Button btnLogout = findViewById(R.id.btnLogout);

        recyclerReports.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        doctorId = FirebaseAuth.getInstance().getUid();

        patientList = new ArrayList<>();
        adapter = new PatientAdapter(patientList, this);
        recyclerReports.setAdapter(adapter);

        loadPatients();

        btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(
                    DoctorDashboardActivity.this,
                    LoginActivity.class
            );

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });
    }

    private void loadPatients() {

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("role", "patient")
                .whereEqualTo("assignedDoctorId", doctorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    progressBar.setVisibility(View.GONE);
                    patientList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String patientId = doc.getId();
                        String name = doc.getString("fullName");

                        patientList.add(new PatientModel(patientId, name));
                    }

                    adapter.notifyDataSetChanged();

                    txtEmpty.setVisibility(
                            patientList.isEmpty() ? View.VISIBLE : View.GONE
                    );
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Failed to load patients",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}