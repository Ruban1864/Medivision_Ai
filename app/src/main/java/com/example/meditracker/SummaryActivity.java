package com.example.meditracker;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SummaryActivity extends AppCompatActivity {

    private TableLayout tableExtractedData;
    private TextView tvSummaryContent;
    private String extractedText;
    private String reportUrl;
    String selectedLanguage;
    private Button btnBack;

    private final String n8nWebhookUrl = "https://ruban181818.app.n8n.cloud/webhook-test/medvision-webhook";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Initialize UI
        tableExtractedData = findViewById(R.id.tableExtractedData);
        tvSummaryContent = findViewById(R.id.tvSummaryContent);
        btnBack = findViewById(R.id.btn_back);

        selectedLanguage = getIntent().getStringExtra("language");
        // Allow scrolling for summary
        tvSummaryContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        // Get data from Intent
        extractedText = getIntent().getStringExtra("extracted_text");
        reportUrl = getIntent().getStringExtra("report_url");

        // Step 1: Extract parameters
        Map<String, String> extractedParams = extractParameters(extractedText);
        if (reportUrl != null && !reportUrl.isEmpty()) {
            extractedParams.put("Report URL", reportUrl);
        }
        extractedParams.put("Language :",selectedLanguage);
        // Step 2: Display extracted parameters in table
        populateTable(extractedParams);

        // Step 3: Send extracted data to n8n
        sendExtractedData(extractedParams);

        // Step 4: Handle Back button
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyzeReportActivity.class));
            finish();
        });
    }

    /** Populates the table with extracted key-value pairs */
    private void populateTable(Map<String, String> data) {
        tableExtractedData.removeAllViews();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            TableRow row = new TableRow(this);

            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            );

            TextView keyView = new TextView(this);
            keyView.setText(entry.getKey());
            keyView.setTextColor(Color.BLACK);
            keyView.setPadding(16, 12, 16, 12);
            keyView.setLayoutParams(params);

            TextView valueView = new TextView(this);
            String value = entry.getValue().isEmpty() ? "N/A" : entry.getValue();
            valueView.setText(value);
            valueView.setTextColor(Color.DKGRAY);
            valueView.setPadding(16, 12, 16, 12);
            valueView.setLayoutParams(params);

            row.addView(keyView);
            row.addView(valueView);
            tableExtractedData.addView(row);
        }
    }

    /** Sends extracted data to n8n webhook and displays summary */
    private void sendExtractedData(Map<String, String> extractedParams) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .callTimeout(3, TimeUnit.MINUTES)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(3, TimeUnit.MINUTES)
                        .writeTimeout(3, TimeUnit.MINUTES)
                        .build();

                JSONObject jsonObject = new JSONObject(extractedParams);
                Log.d(TAG, "Sending data to n8n: " + jsonObject);

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonObject.toString()
                );

                Request request = new Request.Builder()
                        .url(n8nWebhookUrl)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String jsonResponse = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "n8n response: " + jsonResponse);

                runOnUiThread(() -> {
                    if (!jsonResponse.isEmpty()) {
                        tvSummaryContent.setText(jsonResponse.replace("\\n", "\n"));
                    } else {
                        tvSummaryContent.setText("No summary received from server.");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error sending data to n8n", e);
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Failed to send data to n8n",
                        Toast.LENGTH_SHORT
                ).show());
            }
        }).start();
    }

    /** Extracts medical parameters from text using improved regex */
    private Map<String, String> extractParameters(String text) {
        Map<String, String> params = new HashMap<>();
        if (text == null) text = "";

        text = text.replaceAll("\\s+", " ");

        params.put("Patient Name", findValue(text, "(?i)Patient\\s*Name[^A-Za-z]*([A-Za-z ]+)"));
        params.put("Age", findValue(text, "(?i)Age[^0-9]*([0-9]{1,3})"));
        params.put("Email", findValue(text, "(?i)Email[^A-Za-z0-9]*([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})"));
        params.put("Blood Pressure", findValue(text, "(?i)Blood\\s*Pressure[^0-9]*(\\d{2,3}\\/?\\d{2,3})"));
        params.put("RBC", findValue(text, "(?i)RBC[^0-9]*([0-9.]+)"));
        params.put("WBC", findValue(text, "(?i)WBC[^0-9]*([0-9.]+)"));
        params.put("Hemoglobin", findValue(text, "(?i)Hemoglobin[^0-9]*([0-9.]+)"));
        params.put("Platelets", findValue(text, "(?i)Platelets[^0-9,]*([0-9,]+)"));
        params.put("Blood Sugar", findValue(text, "(?i)Blood\\s*Sugar[^0-9]*([0-9.]+)"));
        params.put("Cholesterol", findValue(text, "(?i)Cholesterol[^0-9]*([0-9.]+)"));

        return params;
    }

    /** Finds first regex match for a given field */
    private String findValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
