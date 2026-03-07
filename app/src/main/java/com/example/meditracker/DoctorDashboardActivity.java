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
import com.google.firebase.firestore.Query;
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

        loadAssignedReports();

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

    private void loadAssignedReports() {

        progressBar.setVisibility(View.VISIBLE);

        db.collectionGroup("reports")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    progressBar.setVisibility(View.GONE);
                    reportList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String reportName = doc.getString("name");
                        String summary = doc.getString("summary");
                        String reportUrl = doc.getString("url");

                        String patientId = doc.getReference()
                                .getParent()
                                .getParent()
                                .getId();

                        Map<String,String> extractedData =
                                (Map<String,String>) doc.get("extractedData");

                        Long timestamp = doc.getLong("timestamp");

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

                    // SORT LOCALLY IN JAVA
                    Collections.sort(reportList,
                            (a,b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    adapter.notifyDataSetChanged();

                    txtEmpty.setVisibility(
                            reportList.isEmpty() ? View.VISIBLE : View.GONE
                    );
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);

                    e.printStackTrace();

                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}