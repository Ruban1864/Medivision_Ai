package com.example.meditracker.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportAnalyzer {
    private static final String TAG = "ReportAnalyzer";

    public static Map<String, String> extractParameters(String text) {
        Map<String, String> parameters = new HashMap<>();
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Input text is null or empty");
            return parameters;
        }

        // Define regex patterns for health parameters
        String[] patterns = {
                "(?:Name|Patient Name):\\s*([A-Za-z\\s]+)", // Name
                "(?:Age):\\s*(\\d+)", // Age
                "(?:RBC|Red Blood Cells):\\s*([\\d.]+)\\s*(?:million/uL|\\S+)?", // RBC
                "(?:WBC|White Blood Cells):\\s*([\\d.]+)\\s*(?:thousand/uL|\\S+)?", // WBC
                "(?:Hemoglobin|Hgb):\\s*([\\d.]+)\\s*(?:g/dL|\\S+)?", // Hemoglobin
                "(?:Platelets):\\s*([\\d.]+)\\s*(?:thousand/uL|\\S+)?", // Platelets
                "(?:Blood Sugar|Glucose):\\s*([\\d.]+)\\s*(?:mg/dL|\\S+)?", // Blood Sugar
                "(?:Cholesterol|Total Cholesterol):\\s*([\\d.]+)\\s*(?:mg/dL|\\S+)?", // Cholesterol
                "(?:BP|Blood Pressure):\\s*(\\d+/\\d+)\\s*(?:mmHg|\\S+)?", // Blood Pressure
        };

        // Corresponding keys for the parameters
        String[] keys = {
                "name", "age", "rbc", "wbc", "hemoglobin", "platelets", "sugar", "cholesterol", "bp"
        };

        // Extract parameters using regex
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = Pattern.compile(patterns[i], Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    String value = matcher.group(1).trim();
                    parameters.put(keys[i], value);
                    Log.d(TAG, "Extracted " + keys[i] + ": " + value);
                } catch (Exception e) {
                    Log.w(TAG, "Error extracting " + keys[i] + ": " + e.getMessage());
                }
            }
        }

        if (parameters.isEmpty()) {
            Log.w(TAG, "No parameters extracted from text");
        } else {
            Log.d(TAG, "Extracted parameters: " + parameters.toString());
        }
        return parameters;
    }
}