package com.example.meditracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.InputStream;

public class AnalyzeReportActivity extends AppCompatActivity {

    Button btnSelectPdf, btnCaptureImage, btnExtractText;
    TextView txtExtractedText;
    Uri pdfUri;
    Bitmap capturedImage;
    String extractedText = "";

    // NEW: To hold the Cloudinary URL passed from previous activity
    String reportUrl;

    ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    pdfUri = result.getData().getData();
                    capturedImage = null;
                    String name = getFileName(pdfUri);
                    Toast.makeText(this, "Selected PDF: " + name, Toast.LENGTH_SHORT).show();
                }
            });

    ActivityResultLauncher<Intent> imageCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null && result.getData().getExtras() != null) {
                    capturedImage = (Bitmap) result.getData().getExtras().get("data");
                    pdfUri = null;
                    Toast.makeText(this, "Image captured", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_report);

        // Receive report_url from Intent extras
        reportUrl = getIntent().getStringExtra("report_url");

        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnExtractText = findViewById(R.id.btnExtractText);
        txtExtractedText = findViewById(R.id.txtExtractedText);

        btnSelectPdf.setOnClickListener(v -> pickPdf());
        btnCaptureImage.setOnClickListener(v -> captureImage());
        btnExtractText.setOnClickListener(v -> {
            if (pdfUri != null) {
                extractTextFromPdf();
            } else if (capturedImage != null) {
                extractTextFromImage();
            } else {
                Toast.makeText(this, "Select a PDF or capture an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        pdfPickerLauncher.launch(intent);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageCaptureLauncher.launch(intent);
    }

    private void extractTextFromPdf() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);
            StringBuilder sb = new StringBuilder();

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n");
            }
            reader.close();

            extractedText = sb.toString();
            // Pass extractedText and reportUrl to SummaryActivity
            navigateToSummaryActivity(extractedText);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to extract text from PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractTextFromImage() {
        try {
            InputImage image = InputImage.fromBitmap(capturedImage, 0);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(visionText -> {
                        extractedText = visionText.getText();
                        // Pass extractedText and reportUrl to SummaryActivity
                        navigateToSummaryActivity(extractedText);
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to extract text from image", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToSummaryActivity(String extractedText) {
        Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra("extracted_text", extractedText);
        intent.putExtra("report_url", reportUrl); // pass Cloudinary URL too
        startActivity(intent);
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        return result != null ? result : uri.getLastPathSegment();
    }
}
