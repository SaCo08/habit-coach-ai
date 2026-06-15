package edu.touro.habitcoach;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import edu.touro.habitcoach.databinding.ActivityManageHabitsBinding;
import java.util.Locale;

public class ManageHabitsActivity extends AppCompatActivity {

    private ActivityManageHabitsBinding binding;
    private HabitAdapter adapter;
    private final String[] emojis = {"💧", "🏃", "📚", "🍎", "🧘", "💊", "💤", "🎸", "🌱", "🍳"};
    private int selectedEmojiIndex = 0;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showTimePicker();
                } else {
                    Toast.makeText(this, "Permission denied. Reminders won't work.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        HabitRepository.init(this);
        
        binding = ActivityManageHabitsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Habits");
        }

        setupRecyclerView();
        setupAddHabit();
        setupReminderUI();

        if (savedInstanceState != null) {
            selectedEmojiIndex = savedInstanceState.getInt("selected_emoji_index", 0);
            binding.btnEmojiPicker.setText(emojis[selectedEmojiIndex]);
            if (savedInstanceState.getBoolean("add_section_visible", false)) {
                showAddSection();
            }
        } else if (getIntent().getBooleanExtra("EXTRA_START_ADDING", false)) {
            showAddSection();
        }
    }

    private void setupReminderUI() {
        String savedTime = HabitRepository.getInstance().getReminderTime();
        binding.btnSetReminderTime.setText(formatDisplayTime(savedTime));

        binding.btnSetReminderTime.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED) {
                    showTimePicker();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                showTimePicker();
            }
        });
    }

    private void showTimePicker() {
        String currentTime = HabitRepository.getInstance().getReminderTime();
        int[] time = ReminderManager.parseTime(currentTime);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, selectedMinute) -> {
            String timeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, selectedMinute);
            HabitRepository.getInstance().setReminderTime(timeStr);
            binding.btnSetReminderTime.setText(formatDisplayTime(timeStr));
            
            ReminderManager.scheduleReminder(this, hourOfDay, selectedMinute);
            Toast.makeText(this, "Reminder set for " + formatDisplayTime(timeStr), Toast.LENGTH_SHORT).show();
        }, time[0], time[1], false);
        timePickerDialog.show();
    }

    private String formatDisplayTime(String time) {
        int[] parsed = ReminderManager.parseTime(time);
        int hour = parsed[0];
        int minute = parsed[1];
        String amPm = (hour < 12) ? "AM" : "PM";
        int displayHour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_emoji_index", selectedEmojiIndex);
        outState.putBoolean("add_section_visible", binding.addHabitSection.getVisibility() == View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupRecyclerView() {
        adapter = new HabitAdapter(HabitRepository.getInstance().getHabits(), new HabitAdapter.OnHabitClickListener() {
            @Override
            public void onHabitClick(Habit habit) { }

            @Override
            public void onDeleteClick(Habit habit) {
                HabitRepository.getInstance().removeHabit(habit);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCheckboxToggled(Habit habit, boolean isChecked) {
                // Not used in management mode, but implemented for interface
            }
        }, true);

        binding.rvManageHabits.setLayoutManager(new LinearLayoutManager(this));
        binding.rvManageHabits.setAdapter(adapter);
    }

    private void setupAddHabit() {
        binding.btnAddHabit.setImageResource(android.R.drawable.ic_input_add);

        binding.btnEmojiPicker.setText(emojis[selectedEmojiIndex]);
        binding.btnEmojiPicker.setOnClickListener(v -> {
            selectedEmojiIndex = (selectedEmojiIndex + 1) % emojis.length;
            binding.btnEmojiPicker.setText(emojis[selectedEmojiIndex]);
        });

        binding.btnCancelHabit.setOnClickListener(v -> {
            hideAddSection();
        });

        binding.btnAddHabit.setOnClickListener(v -> {
            if (binding.addHabitSection.getVisibility() == View.GONE) {
                showAddSection();
            } else {
                String name = binding.etHabitName.getText().toString().trim();
                if (!name.isEmpty()) {
                    Habit newHabit = new Habit(name, emojis[selectedEmojiIndex]);
                    HabitRepository.getInstance().addHabit(newHabit);
                    adapter.notifyDataSetChanged();
                    hideAddSection();
                    Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etHabitName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.addHabitSection.getVisibility() == View.VISIBLE) {
                    updateFabState(s.toString().trim().length() > 0);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateFabState(boolean hasText) {
        binding.btnAddHabit.setEnabled(hasText);
        binding.btnAddHabit.setAlpha(hasText ? 1.0f : 0.3f);
    }

    private void showAddSection() {
        binding.addHabitSection.setVisibility(View.VISIBLE);
        boolean hasText = binding.etHabitName.getText().toString().trim().length() > 0;
        updateFabState(hasText);
        binding.etHabitName.requestFocus();
        showKeyboard(binding.etHabitName);
    }

    private void hideAddSection() {
        binding.etHabitName.setText("");
        binding.addHabitSection.setVisibility(View.GONE);
        binding.btnAddHabit.setEnabled(true);
        binding.btnAddHabit.setAlpha(1.0f);
        hideKeyboard(binding.etHabitName);
    }

    private void showKeyboard(View view) {
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
