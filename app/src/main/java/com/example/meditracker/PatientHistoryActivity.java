package com.example.meditracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatientHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<ReportModel> reportList;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_history);

        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        adapter = new HistoryAdapter(reportList, this::openDetail);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadReports();
    }

    private void loadReports() {

        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    reportList.clear();

                    queryDocumentSnapshots.forEach(doc -> {

                        String id = doc.getId();
                        String name = doc.getString("name");
                        String summary = doc.getString("summary");
                        String url = doc.getString("url");

                        // ⭐ NEW: extracted data
                        Map<String,String> extractedData =
                                (Map<String,String>) doc.get("extractedData");

                        reportList.add(new ReportModel(
                                id,
                                name,
                                summary,
                                url,
                                extractedData
                        ));
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load history",
                                Toast.LENGTH_SHORT).show());
    }

    private void openDetail(ReportModel model) {

        Intent intent = new Intent(this, HistoryDetailActivity.class);

        intent.putExtra("summary", model.getSummary());
        intent.putExtra("name", model.getName());
        intent.putExtra("url", model.getUrl());

        // ⭐ Convert Map to string for display
        if(model.getExtractedData()!=null){
            intent.putExtra("extractedData",
                    model.getExtractedData().toString());
        }

        startActivity(intent);
    }
}