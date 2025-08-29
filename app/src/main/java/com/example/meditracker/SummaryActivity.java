package com.example.meditracker;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
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

    Button btnBack;

    private final String n8nWebhookUrl = "https://ruban112.app.n8n.cloud/webhook-test/medvision-webhook";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        tableExtractedData = findViewById(R.id.tableExtractedData);
        tvSummaryContent = findViewById(R.id.tvSummaryContent);
        btnBack = findViewById(R.id.btn_back);

        extractedText = getIntent().getStringExtra("extracted_text");
        reportUrl = getIntent().getStringExtra("report_url");

        Map<String, String> extractedParams = extractParameters(extractedText);
        if (reportUrl != null && !reportUrl.isEmpty()) {
            extractedParams.put("Report URL", reportUrl);
        }

        populateTable(extractedParams);

        sendExtractedData(extractedText, reportUrl);

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            startActivity(new Intent(this,AnalyzeReportActivity.class));
            finish();
        });
    }

    private void populateTable(Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            TableRow row = new TableRow(this);

            TextView paramView = new TextView(this);
            paramView.setText(entry.getKey());
            paramView.setPadding(8, 8, 8, 8);

            TextView valueView = new TextView(this);
            valueView.setText(entry.getValue().isEmpty() ? "N/A" : entry.getValue());
            valueView.setPadding(8, 8, 8, 8);

            row.addView(paramView);
            row.addView(valueView);
            tableExtractedData.addView(row);
        }
    }

    private void sendExtractedData(String text, String url) {
        Map<String, String> extractedParams = extractParameters(text);
        if (url != null && !url.isEmpty()) {
            extractedParams.put("report_url", url);
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .callTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                        .connectTimeout(1, java.util.concurrent.TimeUnit.MINUTES)
                        .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                        .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

                JSONObject jsonObject = new JSONObject(extractedParams);
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

                try {
                    JSONObject responseObj = new JSONObject(jsonResponse);
                    String summary = responseObj.optString("summary", "");

                    runOnUiThread(() -> {
                        if (!summary.isEmpty()) {
                            tvSummaryContent.setText(summary);
                            tvSummaryContent.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
                        } else {
                            tvSummaryContent.setText("No summary received.\n" + (jsonResponse != null ? jsonResponse : "No response data"));
                        }
                    });

                } catch (Exception parseError) {
                    runOnUiThread(() -> tvSummaryContent.setText("Could not parse summary.\n" + jsonResponse));
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send data to n8n", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private Map<String, String> extractParameters(String text) {
        Map<String, String> params = new HashMap<>();
        if (text == null) text = "";

        params.put("Patient Name", findValue(text, "(?i)Patient\\s*Name\\s*[:\\-]?\\s*([A-Za-z ]+)"));
        params.put("Age", findValue(text, "(?i)Age\\s*[:\\-]?\\s*(\\d{1,3})"));
        params.put("Email", findValue(text, "(?i)Email\\s*[:\\-]?\\s*([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})"));
        params.put("Blood Pressure", findValue(text, "(?i)Blood\\s*Pressure\\s*[:\\-]?\\s*(\\d{2,3}\\/?\\d{2,3})"));
        params.put("RBC", findValue(text, "(?i)RBC\\s*[:\\-]?\\s*([0-9.]+)"));
        params.put("WBC", findValue(text, "(?i)WBC\\s*[:\\-]?\\s*([0-9.]+)"));
        params.put("Hemoglobin", findValue(text, "(?i)Hemoglobin\\s*[:\\-]?\\s*([0-9.]+)"));
        params.put("Platelets", findValue(text, "(?i)Platelets\\s*[:\\-]?\\s*([0-9,]+)"));
        params.put("Blood Sugar", findValue(text, "(?i)Blood\\s*Sugar\\s*[:\\-]?\\s*([0-9.]+)"));
        params.put("Cholesterol", findValue(text, "(?i)Cholesterol\\s*[:\\-]?\\s*([0-9.]+)"));

        return params;
    }

    private String findValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
