package com.example.meditracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        String medicineId = intent.getStringExtra("medicineId");
        String medicineName = intent.getStringExtra("medicineName");

        Log.d(TAG, "Alarm triggered for medicine: " + medicineName);

        NotificationHelper notificationHelper =
                new NotificationHelper(context);

        notificationHelper.showNotification(
                medicineId,
                "Medicine Reminder",
                "Take " + medicineName + " in 1 hour!"
        );
    }
}