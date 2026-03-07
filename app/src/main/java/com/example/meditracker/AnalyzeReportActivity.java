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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

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

    Spinner spinnerLanguage;
    String selectedLanguage = "English";

    String reportUrl;
    String reportId;

    ActivityResultLauncher<Intent> pdfPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() != null) {
                            pdfUri = result.getData().getData();
                            capturedImage = null;
                            Toast.makeText(this,"PDF Selected",Toast.LENGTH_SHORT).show();
                        }
                    });

    ActivityResultLauncher<Intent> imageCaptureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() != null && result.getData().getExtras() != null) {
                            capturedImage = (Bitmap) result.getData().getExtras().get("data");
                            pdfUri = null;
                            Toast.makeText(this,"Image Captured",Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_report);

        reportUrl = getIntent().getStringExtra("report_url");
        reportId = getIntent().getStringExtra("report_id");

        if(reportId == null){
            Toast.makeText(this,"Report ID missing",Toast.LENGTH_LONG).show();
        }

        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnExtractText = findViewById(R.id.btnExtractText);
        txtExtractedText = findViewById(R.id.txtExtractedText);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.languages_array,
                        android.R.layout.simple_spinner_item
                );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        btnSelectPdf.setOnClickListener(v -> pickPdf());
        btnCaptureImage.setOnClickListener(v -> captureImage());

        btnExtractText.setOnClickListener(v -> {

            selectedLanguage = spinnerLanguage.getSelectedItem().toString();

            if(pdfUri!=null){
                extractTextFromPdf();
            }
            else if(capturedImage!=null){
                extractTextFromImage();
            }
            else{
                Toast.makeText(this,"Select PDF or Capture Image",Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void pickPdf(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        pdfPickerLauncher.launch(intent);
    }

    private void captureImage(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageCaptureLauncher.launch(intent);
    }

    private void extractTextFromPdf(){

        try{
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);

            StringBuilder sb = new StringBuilder();

            for(int i=1;i<=reader.getNumberOfPages();i++){
                sb.append(PdfTextExtractor.getTextFromPage(reader,i)).append("\n");
            }

            reader.close();

            navigateToSummaryActivity(sb.toString());

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void extractTextFromImage(){

        try{

            InputImage image = InputImage.fromBitmap(capturedImage,0);

            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(visionText -> {

                        navigateToSummaryActivity(visionText.getText());

                    });

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void navigateToSummaryActivity(String extractedText){

        Intent intent = new Intent(this,SummaryActivity.class);

        intent.putExtra("extracted_text",extractedText);
        intent.putExtra("report_url",reportUrl);
        intent.putExtra("report_id",reportId);
        intent.putExtra("language",selectedLanguage);

        startActivity(intent);
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri){

        String result=null;

        if(uri.getScheme().equals("content")){

            try(Cursor cursor=getContentResolver().query(uri,null,null,null,null)){

                if(cursor!=null && cursor.moveToFirst()){
                    result=cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }

        return result!=null ? result : uri.getLastPathSegment();
    }
}