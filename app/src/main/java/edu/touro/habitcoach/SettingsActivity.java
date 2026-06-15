package edu.touro.habitcoach;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import edu.touro.habitcoach.databinding.ActivitySettingsBinding;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("HabitCoachPrefs", Context.MODE_PRIVATE);

        setupToolbar();
        setupReminderTime();
        setupThemeSelection();
        setupWeekStart();
        setupAiNotifications();
        setupDataManagement();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_default_settings) {
            resetToDefaults();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Are you sure you want to reset all settings to their default values?")
            .setPositiveButton("Reset", (dialog, which) -> {
                // Clear preferences
                prefs.edit().clear().apply();
                
                // Update UI and apply logic for each setting
                
                // 1. Reminder Time
                binding.tvReminderTime.setText("8:00 PM");
                
                // 2. Theme
                binding.rbThemeSystem.setChecked(true);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                
                // 3. Week Start
                binding.rbSunday.setChecked(true);
                
                // 4. AI Notifications
                binding.switchAiNotifications.setChecked(false);
                
                Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupReminderTime() {
        String savedTime = prefs.getString("reminder_time", "20:00");
        binding.tvReminderTime.setText(formatTo12Hour(savedTime));

        binding.btnReminderTime.setOnClickListener(v -> {
            String[] parts = savedTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                String time24h = String.format(Locale.US, "%02d:%02d", hourOfDay, minuteOfHour);
                String time12h = formatTo12Hour(time24h);
                
                binding.tvReminderTime.setText(time12h);
                prefs.edit().putString("reminder_time", time24h).apply();
                
                // Update AlarmManager
                ReminderManager.scheduleReminder(this, hourOfDay, minuteOfHour);
                
                Toast.makeText(this, "Reminder set for " + time12h, Toast.LENGTH_SHORT).show();
            }, hour, minute, false);
            timePickerDialog.show();
        });
    }

    private String formatTo12Hour(String time24h) {
        try {
            String[] parts = time24h.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = (hour < 12) ? "AM" : "PM";
            int displayHour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);
            return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm);
        } catch (Exception e) {
            return "8:00 PM";
        }
    }

    private void setupThemeSelection() {
        int theme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (theme == AppCompatDelegate.MODE_NIGHT_NO) binding.rbThemeLight.setChecked(true);
        else if (theme == AppCompatDelegate.MODE_NIGHT_YES) binding.rbThemeDark.setChecked(true);
        else binding.rbThemeSystem.setChecked(true);

        binding.rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.rbThemeLight) mode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (checkedId == R.id.rbThemeDark) mode = AppCompatDelegate.MODE_NIGHT_YES;
            else mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            prefs.edit().putInt("theme_mode", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });
    }

    private void setupWeekStart() {
        boolean startsMonday = prefs.getBoolean("week_starts_monday", false);
        if (startsMonday) binding.rbMonday.setChecked(true);
        else binding.rbSunday.setChecked(true);

        binding.rgWeekStart.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMonday = (checkedId == R.id.rbMonday);
            prefs.edit().putBoolean("week_starts_monday", isMonday).apply();
        });
    }

    private void setupAiNotifications() {
        boolean enabled = prefs.getBoolean("ai_notifications_enabled", false);
        binding.switchAiNotifications.setChecked(enabled);

        binding.switchAiNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("ai_notifications_enabled", isChecked).apply();
            if (isChecked) {
                Toast.makeText(this, "AI Recommendations enabled in daily reminder", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDataManagement() {
        binding.btnClearAnalytics.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Clear Analytics")
                .setMessage("Are you sure you want to clear your analytics data? This will reset your current progress.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    HabitRepository.getInstance().clearAnalytics();
                    Toast.makeText(this, "Analytics cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        binding.btnDeleteAllHabits.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete All Habits")
                .setMessage("This will permanently delete all your habits. You'll have a completely clean start.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    HabitRepository.getInstance().deleteAllHabits();
                    Toast.makeText(this, "All habits deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }
}