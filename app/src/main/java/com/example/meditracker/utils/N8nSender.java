package com.example.meditracker.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class N8nSender {
    private static final String TAG = "N8nSender";
    private static final String N8N_WEBHOOK_URL = "https://ruban11.app.n8n.cloud/webhook-test/medvision-webhook";
    private static final int REQUEST_TIMEOUT_MS = 15000; // 15 seconds timeout
    private static final int MAX_RETRIES = 3;

    public static void sendDataToN8n(Context context, Map<String, String> extracted, String userName,
                                     String email, String fileUrl, String timestamp) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot send request");
            return;
        }

        if (extracted == null || extracted.isEmpty()) {
            Log.w(TAG, "Extracted parameters are null or empty");
            Toast.makeText(context, "No health parameters to send", Toast.LENGTH_LONG).show();
            return;
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            Log.w(TAG, "File URL is null or empty");
            Toast.makeText(context, "Invalid file URL", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("user_name", userName != null ? userName : "Unknown");
            json.put("email", email != null ? email : "");
            json.put("file_url", fileUrl);
            json.put("timestamp", timestamp != null ? timestamp : "");

            // Add extracted parameters with explicit keys
            String[] paramKeys = {"name", "age", "rbc", "wbc", "hemoglobin", "platelets", "sugar", "cholesterol", "bp"};
            for (String key : paramKeys) {
                if (extracted.containsKey(key)) {
                    json.put(key, extracted.get(key));
                }
            }

            Log.d(TAG, "JSON payload: " + json.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    N8N_WEBHOOK_URL,
                    json,
                    response -> {
                        Log.d(TAG, "Webhook success: " + response.toString());
                        Toast.makeText(context, "Report sent to n8n for analysis", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        String errorMessage = "Failed to send report to n8n";
                        int statusCode = -1;
                        String responseBody = "";
                        if (error.networkResponse != null) {
                            statusCode = error.networkResponse.statusCode;
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            errorMessage += " (Status: " + statusCode + ", Response: " + responseBody + ")";
                        } else if (error.getMessage() != null) {
                            errorMessage += " (" + error.getMessage() + ")";
                        }
                        Log.e(TAG, errorMessage, error);
                        String userMessage;
                        switch (statusCode) {
                            case 403:
                                userMessage = "Webhook authentication failed. Check API key.";
                                break;
                            case 404:
                                userMessage = "Webhook URL not found. Verify n8n workflow.";
                                break;
                            case 429:
                                userMessage = "Rate limit exceeded. Try again later.";
                                break;
                            default:
                                userMessage = "Failed to send report to n8n: " + (error.getMessage() != null ? error.getMessage() : "Unknown error");
                        }
                        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    // Uncomment and add API key if required
                    // headers.put("Authorization", "Bearer YOUR_API_KEY");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    REQUEST_TIMEOUT_MS,
                    MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);
            Log.d(TAG, "Request sent to n8n webhook");

        } catch (JSONException e) {
            Log.e(TAG, "JSON construction error: " + e.getMessage(), e);
            Toast.makeText(context, "Failed to prepare report data", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
            Toast.makeText(context, "Unexpected error occurred", Toast.LENGTH_LONG).show();
        }
    }
}