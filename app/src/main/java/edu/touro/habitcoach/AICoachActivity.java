package edu.touro.habitcoach;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import edu.touro.habitcoach.databinding.ActivityAiCoachBinding;
import java.util.List;

public class AICoachActivity extends AppCompatActivity {

    private ActivityAiCoachBinding binding;
    private HabitRepository repository;
    private AiManager aiManager;
    private String currentAdvice = null;
    private boolean isAnalyzing = false;
    private int savedScrollY = 0;
    private final Handler analysisHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        HabitRepository.init(this);
        repository = HabitRepository.getInstance();
        aiManager = new AiManager();

        binding = ActivityAiCoachBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        updateAiStatus();
        displayCurrentData();
        
        currentAdvice = repository.getLastAiAdvice();
        if (currentAdvice != null) {
            binding.cvCoachInsight.setVisibility(View.VISIBLE);
            binding.tvCoachMessage.setText(currentAdvice);
        }

        if (savedInstanceState != null) {
            isAnalyzing = savedInstanceState.getBoolean("is_analyzing", false);
            savedScrollY = savedInstanceState.getInt("scroll_y", 0);
            
            if (isAnalyzing) {
                showAnalyzingUI();
                startAnalysisTask(); 
            }

            binding.aiCoachScrollView.post(() -> {
                if (binding != null) {
                    binding.aiCoachScrollView.scrollTo(0, savedScrollY);
                }
            });
        }
        
        binding.btnGenerateInsight.setOnClickListener(v -> analyzeProgress());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_analyzing", isAnalyzing);
        outState.putInt("scroll_y", binding.aiCoachScrollView.getScrollY());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        analysisHandler.removeCallbacksAndMessages(null);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Coach");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void updateAiStatus() {
        boolean enabled = getSharedPreferences("HabitCoachPrefs", MODE_PRIVATE)
                .getBoolean("ai_notifications_enabled", false);
        binding.tvFrequencyStatus.setText("Daily AI Insights: " + (enabled ? "Enabled" : "Disabled"));
    }

    private void displayCurrentData() {
        binding.tvProgressData.setText(generateHabitDataString());
    }

    private String generateHabitDataString() {
        List<Habit> habits = repository.getHabits();
        if (habits.isEmpty()) return "No habits tracked yet.";

        StringBuilder sb = new StringBuilder("Personal Habit Data:\n");
        for (Habit habit : habits) {
            sb.append(habit.getEmoji()).append(" ")
              .append(habit.getName()).append(": ")
              .append(habit.isCompletedToday() ? "Completed today. " : "Pending. ")
              .append("Weekly: ").append(habit.getWeeklyStreak()).append(" days. ")
              .append("Streak: ").append(habit.getCurrentStreak()).append("\n");
        }
        return sb.toString();
    }

    private void analyzeProgress() {
        if (isAnalyzing) return;
        showAnalyzingUI();
        startAnalysisTask();
    }

    private void showAnalyzingUI() {
        isAnalyzing = true;
        binding.cvCoachInsight.setVisibility(View.VISIBLE);
        binding.tvCoachMessage.setText("Analyzing your progress data...");
        binding.btnGenerateInsight.setEnabled(false);
        binding.btnGenerateInsight.setAlpha(0.5f);
    }

    private void startAnalysisTask() {
        String dataToAnalyze = generateHabitDataString();
        // Specific prompt to ensure non-generic, data-driven AI coaching.
        String question = "Analyze my habit stats. Identify my strongest habit, which one needs focus, " +
                "and give me one unique, non-generic strategy to improve my consistency based on these numbers.";

        aiManager.askAiWithContext(question, dataToAnalyze, new AiManager.AiCallback() {
            @Override
            public void onSuccess(String response) {
                if (isFinishing() || isDestroyed()) return;
                
                isAnalyzing = false;
                currentAdvice = response;
                repository.setLastAiAdvice(response);

                binding.tvCoachMessage.setText(response);
                binding.btnGenerateInsight.setEnabled(true);
                binding.btnGenerateInsight.setAlpha(1.0f);
            }

            @Override
            public void onFailure(String error) {
                if (isFinishing() || isDestroyed()) return;
                
                isAnalyzing = false;
                binding.tvCoachMessage.setText("I couldn't reach the AI Coach. Please check your internet connection.");
                binding.btnGenerateInsight.setEnabled(true);
                binding.btnGenerateInsight.setAlpha(1.0f);
                Toast.makeText(AICoachActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
