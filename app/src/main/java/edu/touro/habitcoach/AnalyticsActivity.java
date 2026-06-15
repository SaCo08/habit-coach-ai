package edu.touro.habitcoach;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import edu.touro.habitcoach.databinding.ActivityAnalyticsBinding;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;
    private HabitRepository repository;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure Repository is initialized with context for rotation resilience
        HabitRepository.init(this);
        repository = HabitRepository.getInstance();
        
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("HabitCoachPrefs", Context.MODE_PRIVATE);

        setupToolbar();
        displayWeeklyPerfection();
        displayBestHabit();
        displayIndividualStreaks();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayWeeklyPerfection() {
        int perfectDays = repository.getPerfectDaysCount();
        binding.pbWeeklyStreak.setProgress(perfectDays);
        binding.tvPerfectDaysCount.setText(perfectDays + "/7 days completed");
    }

    private void displayBestHabit() {
        Habit best = repository.getBestHabit();
        if (best != null && best.getWeeklyStreak() > 0) {
            binding.tvBestHabitEmoji.setText(best.getEmoji());
            binding.tvBestHabitName.setText(best.getName());
            binding.tvBestHabitStreak.setText(best.getWeeklyStreak() + " days");
        } else {
            binding.tvBestHabitEmoji.setText("⏳");
            binding.tvBestHabitName.setText("0");
            binding.tvBestHabitStreak.setText("0 days");
        }
    }

    private void displayIndividualStreaks() {
        List<Habit> habits = repository.getHabits();
        binding.llHabitStreaks.removeAllViews();

        if (habits.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("0 habits tracked");
            emptyText.setPadding(0, 16, 0, 0);
            binding.llHabitStreaks.addView(emptyText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Habit habit : habits) {
            View habitView = inflater.inflate(R.layout.item_habit, binding.llHabitStreaks, false);
            
            MaterialCardView card = habitView.findViewById(R.id.habitCard);
            TextView emoji = habitView.findViewById(R.id.habitEmoji);
            TextView name = habitView.findViewById(R.id.habitName);
            View checkMark = habitView.findViewById(R.id.checkMark);
            View deleteButton = habitView.findViewById(R.id.deleteButton);

            emoji.setText(habit.getEmoji());
            name.setText(habit.getName());
            checkMark.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);

            // Add a streak badge to the right side
            TextView streakText = new TextView(this);
            streakText.setText(habit.getWeeklyStreak() + " days");
            streakText.setTextColor(getResources().getColor(R.color.purple_500, getTheme()));
            streakText.setTypeface(null, Typeface.BOLD);
            
            android.widget.LinearLayout container = (android.widget.LinearLayout) name.getParent();
            container.addView(streakText);

            binding.llHabitStreaks.addView(habitView);
        }
    }
}
