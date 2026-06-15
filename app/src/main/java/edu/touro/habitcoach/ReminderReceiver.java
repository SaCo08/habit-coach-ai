package edu.touro.habitcoach;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "habit_reminder_channel";
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification trigger received");
        final PendingResult pendingResult = goAsync();

        HabitRepository.init(context);
        HabitRepository repository = HabitRepository.getInstance();
        SharedPreferences prefs = context.getSharedPreferences("HabitCoachPrefs", Context.MODE_PRIVATE);
        
        // Respect the frequency setting: if "requested", we don't send AI notifications automatically
        String frequency = prefs.getString("ai_frequency", "daily");
        boolean aiEnabled = prefs.getBoolean("ai_notifications_enabled", true); // Default to true for "fully connected"
        
        int completed = repository.getCompletedCount();
        int total = repository.getTotalCount();
        int remaining = total - completed;

        // If frequency is not 'requested', we try to get an AI response
        if (aiEnabled && total > 0 && !frequency.equals("requested")) {
            AiManager aiManager = new AiManager();
            String contextData = generateHabitDataString(repository);
            
            // Refined prompt for more personality and less "robotic" feel
            String question = "You are my personal Habit Coach. I've done " + completed + "/" + total + 
                    " habits today. Give me a unique, short, and punchy notification (under 20 words) " +
                    "that isn't generic. Use my progress context to motivate me personally.";

            aiManager.askAiWithContext(question, contextData, new AiManager.AiCallback() {
                @Override
                public void onSuccess(String response) {
                    showNotification(context, "AI Coach Insight", response);
                    finalizeReceiver(context, repository, pendingResult);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "AI Failure: " + error);
                    // Fallback only if the internet/API fails
                    showNotification(context, "Habit Coach", "The AI Coach is resting, but don't you rest! " + remaining + " habits left.");
                    finalizeReceiver(context, repository, pendingResult);
                }
            });
        } else {
            // Standard notification if AI is disabled or set to 'requested'
            String message = (total == 0) ? "Start your journey by adding a habit!" :
                             (remaining > 0) ? "Don't forget your " + remaining + " remaining habits!" :
                             "You've mastered the day! All habits complete.";
            showNotification(context, "Habit Coach", message);
            finalizeReceiver(context, repository, pendingResult);
        }
    }

    private String generateHabitDataString(HabitRepository repository) {
        List<Habit> habits = repository.getHabits();
        if (habits.isEmpty()) return "No habits tracked yet.";

        StringBuilder sb = new StringBuilder("User's Habit Data:\n");
        for (Habit habit : habits) {
            sb.append("- ").append(habit.getName())
              .append(" (").append(habit.getEmoji()).append("): ")
              .append(habit.isCompletedToday() ? "Done today. " : "Pending. ")
              .append("Streak: ").append(habit.getCurrentStreak()).append(" days.\n");
        }
        return sb.toString();
    }

    private void finalizeReceiver(Context context, HabitRepository repository, PendingResult pendingResult) {
        try {
            String reminderTime = repository.getReminderTime();
            int[] time = ReminderManager.parseTime(reminderTime);
            ReminderManager.scheduleReminder(context, time[0], time[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule", e);
            ReminderManager.scheduleReminder(context, 20, 0);
        } finally {
            pendingResult.finish();
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
