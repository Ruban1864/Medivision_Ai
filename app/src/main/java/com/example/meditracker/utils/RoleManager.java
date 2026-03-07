package com.example.meditracker.utils;

import android.content.Context;
import android.content.Intent;

import com.example.meditracker.DoctorDashboardActivity;
import com.example.meditracker.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleManager {

    public static void routeUser(Context context) {

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String role =
                                document.getString("role");

                        if ("doctor".equals(role)) {

                            context.startActivity(
                                    new Intent(context,
                                            DoctorDashboardActivity.class));

                        } else {

                            context.startActivity(
                                    new Intent(context,
                                            MainActivity.class));
                        }
                    }
                });
    }
}