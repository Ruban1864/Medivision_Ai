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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DoctorDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerReports;
    private ProgressBar progressBar;
    private TextView txtEmpty;

    private FirebaseFirestore db;
    private String doctorId;

    private List<DoctorReportModel> reportList;
    private DoctorReportAdapter adapter;

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

        reportList = new ArrayList<>();
        adapter = new DoctorReportAdapter(reportList, this);
        recyclerReports.setAdapter(adapter);

        loadAssignedPatients();

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

    private void loadAssignedPatients() {

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("role", "patient")
                .whereEqualTo("assignedDoctorId", doctorId) // FIXED FIELD
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    reportList.clear();

                    if(queryDocumentSnapshots.isEmpty()){
                        progressBar.setVisibility(View.GONE);
                        txtEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for(QueryDocumentSnapshot patientDoc : queryDocumentSnapshots){

                        String patientId = patientDoc.getId();

                        loadPatientReports(patientId);
                    }
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Failed loading patients",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadPatientReports(String patientId) {

        db.collection("users")
                .document(patientId)
                .collection("reports")
                .get()
                .addOnSuccessListener(reportSnapshots -> {

                    for (QueryDocumentSnapshot doc : reportSnapshots) {

                        String reportName = doc.getString("name");
                        String summary = doc.getString("summary");
                        String reportUrl = doc.getString("url");

                        Map<String,String> extractedData =
                                (Map<String,String>) doc.get("extractedData");

                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");

                        Long timestamp = 0L;
                        if (ts != null) {
                            timestamp = ts.getSeconds();
                        }

                        reportList.add(
                                new DoctorReportModel(
                                        reportName,
                                        summary,
                                        reportUrl,
                                        patientId,
                                        extractedData,
                                        timestamp
                                )
                        );
                    }

                    Collections.sort(reportList,
                            (a,b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    adapter.notifyDataSetChanged();

                    progressBar.setVisibility(View.GONE);

                    txtEmpty.setVisibility(
                            reportList.isEmpty() ? View.VISIBLE : View.GONE
                    );
                });
    }
}