package com.campusmate.pro.notification;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import com.campusmate.pro.MainActivity;
import com.campusmate.pro.R;
import com.campusmate.pro.receiver.ReminderReceiver;

public class NotificationHelper {
    public static final String CHANNEL_ID = "campusmate_reminder";

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat CampusMate",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Pengingat deadline tugas dan sesi fokus.");
            channel.enableVibration(true);
            channel.enableLights(true);
            Uri soundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void show(Context context, String title, String message, int id) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_campusmate_notification)
                .setContentTitle(title)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.campusmate_logo_mark))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(id, builder.build());
    }

    public static boolean scheduleTaskReminder(Context context, long triggerAtMillis, String taskTitle, String courseName) {
        if (triggerAtMillis <= System.currentTimeMillis()) return false;
        try {
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("title", "Pengingat Tugas");
            intent.putExtra("message", taskTitle + " - " + courseName);
            int requestCode = Math.abs((taskTitle + courseName + triggerAtMillis).hashCode());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return false;

            // Jangan pakai setExact/setExactAndAllowWhileIdle karena di Android 12+ bisa crash
            // jika permission exact alarm tidak diberikan. set() tetap menjadwalkan alarm lokal
            // tanpa membuat aplikasi keluar sendiri.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
