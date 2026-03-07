package com.example.meditracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter
        extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<ReportModel> reportList;   // ✅ Correct type
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ReportModel model);
    }

    public HistoryAdapter(List<ReportModel> reportList,
                          OnItemClickListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        ReportModel model = reportList.get(position);  // ✅ Correct

        holder.tvReportName.setText(model.getName());

        String summary = model.getSummary();
        if (summary != null && summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }

        holder.tvSummaryPreview.setText(
                summary != null ? summary : "No summary available");

        holder.itemView.setOnClickListener(v ->
                listener.onItemClick(model));
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvReportName;
        TextView tvSummaryPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvReportName =
                    itemView.findViewById(R.id.tv_report_name);

            tvSummaryPreview =
                    itemView.findViewById(R.id.tv_summary_preview);
        }
    }
}