package com.example.meditracker;

public class PatientModel {

    private String patientId;
    private String patientName;

    public PatientModel(String patientId, String patientName) {
        this.patientId = patientId;
        this.patientName = patientName;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }
}