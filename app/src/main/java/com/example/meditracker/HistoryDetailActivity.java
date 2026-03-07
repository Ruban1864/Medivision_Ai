package com.example.meditracker;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class HistoryDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        TextView tvTitle = findViewById(R.id.tv_report_name);
        TextView tvSummary = findViewById(R.id.tv_summary);
        TextView tvUrl = findViewById(R.id.tv_report_url);
        TableLayout table = findViewById(R.id.table_extracted_data);

        String name = getIntent().getStringExtra("name");
        String summary = getIntent().getStringExtra("summary");
        String url = getIntent().getStringExtra("url");
        String extractedData = getIntent().getStringExtra("extractedData");

        tvTitle.setText(name);
        tvSummary.setText(summary);
        tvUrl.setText(url);

        tvSummary.setMovementMethod(ScrollingMovementMethod.getInstance());

        // Convert extracted string to table rows
        if (extractedData != null && !extractedData.isEmpty()) {

            extractedData = extractedData
                    .replace("{", "")
                    .replace("}", "");

            String[] pairs = extractedData.split(",");

            for (String pair : pairs) {

                String[] keyValue = pair.split("=");

                if (keyValue.length == 2) {

                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    TableRow row = new TableRow(this);

                    TextView tvKey = new TextView(this);
                    tvKey.setText(key.toUpperCase());
                    tvKey.setPadding(8,8,8,8);
                    tvKey.setTextColor(getResources().getColor(R.color.textPrimary));
                    tvKey.setTextSize(14);

                    TextView tvValue = new TextView(this);
                    tvValue.setText(value);
                    tvValue.setPadding(8,8,8,8);
                    tvValue.setTextColor(getResources().getColor(R.color.textSecondary));
                    tvValue.setTextSize(14);

                    row.addView(tvKey);
                    row.addView(tvValue);

                    table.addView(row);
                }
            }
        }
    }
}