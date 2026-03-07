package com.example.meditracker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DoctorReportAdapter
        extends RecyclerView.Adapter<DoctorReportAdapter.ViewHolder> {

    private final List<DoctorReportModel> list;
    private final Context context;

    public DoctorReportAdapter(List<DoctorReportModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_doctor_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        DoctorReportModel model = list.get(position);

        // Report Name
        holder.txtReportName.setText(model.getReportName());

        // Patient ID
        holder.txtPatientId.setText("Patient ID: " + model.getPatientId());

        // Summary preview
        String summary = model.getSummary();

        if (summary == null || summary.isEmpty()) {
            summary = "No AI summary available";
        }

        if (summary.length() > 120) {
            summary = summary.substring(0, 120) + "...";
        }

        holder.txtSummary.setText(summary);

        // Click event
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, HistoryDetailActivity.class);

            intent.putExtra("name", model.getReportName());
            intent.putExtra("summary", model.getSummary());
            intent.putExtra("url", model.getReportUrl());

            if (model.getExtractedData() != null) {
                intent.putExtra(
                        "extractedData",
                        model.getExtractedData().toString()
                );
            }

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtReportName;
        TextView txtSummary;
        TextView txtPatientId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtReportName = itemView.findViewById(R.id.txtReportName);
            txtSummary = itemView.findViewById(R.id.txtSummary);
            txtPatientId = itemView.findViewById(R.id.txtPatientId);
        }
    }
}