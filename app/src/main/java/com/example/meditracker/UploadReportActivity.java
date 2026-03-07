package com.example.meditracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadReportActivity extends AppCompatActivity {

    private static final String TAG = "UploadReportActivity";

    private EditText etReportName;
    private ProgressBar progressBar;
    private Button btnAnalyze;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    private Cloudinary cloudinary;

    private Uri lastFileUri;
    private String uploadedFileUrl = null;
    private String fileType;
    private File photoFile;
    private Uri photoUri;

    private String reportId;   // ⭐ ADD THIS
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> selectFileLauncher;
    private ActivityResultLauncher<Intent> captureImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_report);

        Toolbar toolbar = findViewById(R.id.upload_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Upload Report");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        etReportName = findViewById(R.id.et_report_name);
        progressBar = findViewById(R.id.progressBarUpload);

        Button btnUploadPdf = findViewById(R.id.btn_upload_pdf);
        Button btnCaptureImage = findViewById(R.id.btn_capture_image);
        btnAnalyze = findViewById(R.id.btn_analyze);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dfjuuur7l",
                "api_key", "155684526852715",
                "api_secret", "ZsoPKadYWegNGiAlXc2nmuVeUkU"
        ));

        setupLaunchers();

        btnUploadPdf.setOnClickListener(v -> selectPdf());
        btnCaptureImage.setOnClickListener(v -> captureImage());

        btnAnalyze.setOnClickListener(v -> {

            if(uploadedFileUrl != null && reportId != null){

                Intent intent = new Intent(this, AnalyzeReportActivity.class);

                intent.putExtra("report_url", uploadedFileUrl);
                intent.putExtra("report_id", reportId);   // ⭐ IMPORTANT

                startActivity(intent);

            }else{

                Toast.makeText(this,
                        "Upload report first",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLaunchers() {

        selectFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            result.getData() != null) {
                        lastFileUri = result.getData().getData();
                        fileType = "pdf";
                        uploadFileToCloudinary(lastFileUri, fileType);
                    }
                });

        captureImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            photoUri != null) {
                        fileType = "image";
                        uploadFileToCloudinary(photoUri, fileType);
                    }
                });
    }

    private void selectPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        selectFileLauncher.launch(intent);
    }

    private void captureImage() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            captureImageLauncher.launch(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        File storageDir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                storageDir);
    }

    private void uploadFileToCloudinary(Uri fileUri, String type) {

        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        executor.execute(() -> {

            try (InputStream inputStream =
                         getContentResolver().openInputStream(fileUri)) {

                Map uploadResult =
                        cloudinary.uploader().upload(
                                inputStream,
                                ObjectUtils.asMap(
                                        "resource_type",
                                        type.equals("pdf") ? "raw" : "image"));

                uploadedFileUrl =
                        uploadResult.get("secure_url").toString();

                runOnUiThread(() ->
                        saveReportData(uploadedFileUrl, type));

            } catch (Exception e) {

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Upload failed",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // 🔥 UPDATED METHOD
    private void saveReportData(String fileUrl, String type) {

        String reportName = etReportName.getText().toString().trim();

        if (reportName.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this,"Enter report name",Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) return;

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String assignedDoctorId =
                            userDoc.getString("assignedDoctorId");

                    Map<String,Object> reportData = new HashMap<>();

                    reportData.put("name",reportName);
                    reportData.put("url",fileUrl);
                    reportData.put("fileType",type);
                    reportData.put("patientId",userId);
                    reportData.put("doctorId",assignedDoctorId);
                    reportData.put("timestamp",FieldValue.serverTimestamp());

                    firestore.collection("users")
                            .document(userId)
                            .collection("reports")
                            .add(reportData)
                            .addOnSuccessListener(docRef -> {

                                progressBar.setVisibility(View.GONE);
                                btnAnalyze.setEnabled(true);

                                // ⭐ CAPTURE DOCUMENT ID
                                reportId = docRef.getId();

                                Log.d("REPORT_DEBUG","Generated Report ID: "+reportId);

                                Toast.makeText(this,
                                        "Report uploaded",
                                        Toast.LENGTH_SHORT).show();

                            })
                            .addOnFailureListener(e -> {

                                progressBar.setVisibility(View.GONE);

                                Toast.makeText(this,
                                        "Firestore error",
                                        Toast.LENGTH_LONG).show();
                            });

                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}