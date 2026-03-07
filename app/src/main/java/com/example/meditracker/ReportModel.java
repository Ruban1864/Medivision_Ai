package com.example.meditracker;

import java.util.Map;

public class ReportModel {

    private String id;
    private String name;
    private String summary;
    private String url;
    private Map<String, String> extractedData;

    public ReportModel(String id,
                       String name,
                       String summary,
                       String url,
                       Map<String,String> extractedData) {

        this.id = id;
        this.name = name;
        this.summary = summary;
        this.url = url;
        this.extractedData = extractedData;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getSummary() { return summary; }

    public String getUrl() { return url; }

    public Map<String,String> getExtractedData() {
        return extractedData;
    }
}