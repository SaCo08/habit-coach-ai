package edu.touro.habitcoach;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                HabitRepository.init(context);
                String reminderTime = HabitRepository.getInstance().getReminderTime();
                
                // Use the safe parsing helper to avoid crashes
                int[] time = ReminderManager.parseTime(reminderTime);
                ReminderManager.scheduleReminder(context, time[0], time[1]);
            } catch (Exception e) {
                Log.e("BootReceiver", "Failed to reschedule reminder after boot", e);
            }
        }
    }
}
