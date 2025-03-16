package com.example.meditracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String medicineId = intent.getStringExtra("medicineId");
        String medicineName = intent.getStringExtra("medicineName");

        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showNotification(medicineId, "Medicine Reminder",
                "Take " + medicineName + " in 1 hour!");
    }
}