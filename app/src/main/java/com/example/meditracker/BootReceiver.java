package com.example.meditracker;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.d(TAG, "Device rebooted, rescheduling alarms");

            // Initialize Firebase
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

            if (userId == null) {
                Log.w(TAG, "No user logged in, skipping alarm rescheduling");
                return;
            }

            // Fetch all medicines and reschedule alarms
            db.collection("users").document(userId).collection("medicines")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String medicineId = document.getId();
                            String medicineName = document.getString("name");
                            String schedule = document.getString("schedule");
                            String frequency = document.getString("frequency");

                            if (schedule != null && medicineName != null && frequency != null) {
                                rescheduleAlarm(context, alarmManager, medicineId, medicineName, schedule, frequency);
                            }
                        }
                        Log.d(TAG, "Alarms rescheduled successfully");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching medicines: " + e.getMessage()));
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void rescheduleAlarm(Context context, AlarmManager alarmManager, String medicineId,
                                 String medicineName, String schedule, String frequency) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra("medicineId", medicineId);
        alarmIntent.putExtra("medicineName", medicineName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, medicineId.hashCode(), alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(schedule));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Set alarm 1 hour before the scheduled time
            Calendar alarmTime = (Calendar) calendar.clone();
            alarmTime.add(Calendar.HOUR_OF_DAY, -1);

            // Adjust for past alarms based on frequency
            Calendar now = Calendar.getInstance();
            while (alarmTime.before(now)) {
                if (frequency.equals("Daily")) {
                    alarmTime.add(Calendar.DAY_OF_YEAR, 1);
                } else if (frequency.equals("Weekly")) {
                    alarmTime.add(Calendar.WEEK_OF_YEAR, 1);
                } else if (frequency.equals("Monthly")) {
                    alarmTime.add(Calendar.MONTH, 1);
                } else {
                    break; // One-time alarm, don’t reschedule if past
                }
            }

            // Schedule the alarm
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Rescheduled alarm for " + medicineName + " at " + sdf.format(alarmTime.getTime()));
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling alarm for " + medicineName + ": " + e.getMessage());
        }
    }
}