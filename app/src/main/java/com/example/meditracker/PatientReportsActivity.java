package com.example.meditracker;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatientReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerReports;
    private ProgressBar progressBar;
    private TextView txtPatientName;

    private FirebaseFirestore db;

    private List<DoctorReportModel> reportList;
    private DoctorReportAdapter adapter;

    private String patientId;
    private String patientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_reports);

        recyclerReports = findViewById(R.id.recyclerReports);
        progressBar = findViewById(R.id.progressBar);
        txtPatientName = findViewById(R.id.txtPatientName);

        recyclerReports.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        patientId = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");

        txtPatientName.setText(patientName + " Reports");

        reportList = new ArrayList<>();
        adapter = new DoctorReportAdapter(reportList, this);
        recyclerReports.setAdapter(adapter);

        loadReports();
    }

    private void loadReports() {

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(patientId)
                .collection("reports")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    progressBar.setVisibility(View.GONE);
                    reportList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String reportName = doc.getString("name");
                        String summary = doc.getString("summary");
                        String url = doc.getString("url");

                        Map<String,String> extractedData =
                                (Map<String,String>) doc.get("extractedData");

                        reportList.add(
                                new DoctorReportModel(
                                        reportName,
                                        summary,
                                        url,
                                        patientId,
                                        extractedData,
                                        0L
                                )
                        );
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}