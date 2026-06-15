package edu.touro.habitcoach;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class ReminderManager {

    public static void scheduleReminder(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the scheduled time is in the past, move it to tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    /**
     * Safely parses a time string in either "HH:mm" or legacy "h:mm AM/PM" format.
     * @param time The time string to parse
     * @return An array containing [hour, minute]
     */
    public static int[] parseTime(String time) {
        int[] result = new int[]{20, 0}; // Default 8:00 PM
        if (time == null || !time.contains(":")) return result;

        try {
            String[] parts = time.split(":");
            if (parts.length >= 2) {
                // Parse hour
                result[0] = Integer.parseInt(parts[0].trim());
                
                // Parse minute, stripping any AM/PM suffix
                String minutePart = parts[1].trim();
                String minuteDigits = minutePart.replaceAll("[^0-9]", "");
                if (!minuteDigits.isEmpty()) {
                    result[1] = Integer.parseInt(minuteDigits);
                }

                // Convert to 24-hour if it's a legacy 12-hour string
                String upperTime = time.toUpperCase();
                if (upperTime.contains("PM") && result[0] < 12) {
                    result[0] += 12;
                } else if (upperTime.contains("AM") && result[0] == 12) {
                    result[0] = 0;
                }
            }
        } catch (Exception e) {
            // Fallback to defaults on any parsing error
        }
        return result;
    }
}
