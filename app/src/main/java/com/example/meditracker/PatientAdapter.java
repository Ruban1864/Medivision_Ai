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

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    private List<PatientModel> list;
    private Context context;

    public PatientAdapter(List<PatientModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_patient, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        PatientModel model = list.get(position);

        holder.txtPatientName.setText(model.getPatientName());

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, PatientReportsActivity.class);

            intent.putExtra("patientId", model.getPatientId());
            intent.putExtra("patientName", model.getPatientName());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPatientName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtPatientName = itemView.findViewById(R.id.txtPatientName);
        }
    }
}