package com.example.meditracker;

import java.util.Map;

public class DoctorReportModel {

    private String reportName;
    private String summary;
    private String reportUrl;
    private String patientId;
    private Map<String,String> extractedData;
    private Long timestamp;
    public DoctorReportModel(
            String reportName,
            String summary,
            String reportUrl,
            String patientId,
            Map<String,String> extractedData,
            Long timestamp) {

        this.reportName = reportName;
        this.summary = summary;
        this.reportUrl = reportUrl;
        this.patientId = patientId;
        this.extractedData = extractedData;
        this.timestamp = timestamp;
    }

    public String getReportName() {
        return reportName;
    }

    public String getSummary() {
        return summary;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public String getPatientId() {
        return patientId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Map<String,String> getExtractedData() {
        return extractedData;
    }
}