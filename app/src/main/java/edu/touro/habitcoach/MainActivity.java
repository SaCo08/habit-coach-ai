package edu.touro.habitcoach;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import edu.touro.habitcoach.databinding.ActivityMainBinding;
import nl.dionsegijn.konfetti.core.Angle;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Spread;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private HabitAdapter adapter;
    private boolean celebrationShownToday = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Repository with context to enable persistence
        HabitRepository.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup the toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Habit Coach");
        }

        setupRecyclerView();
        updateProgress(false);
        requestNotificationPermission();

        // FAB opens management page and tells it to show the add habit section immediately
        binding.fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, ManageHabitsActivity.class);
            intent.putExtra("EXTRA_START_ADDING", true);
            startActivity(intent);
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new HabitAdapter(HabitRepository.getInstance().getHabits(), new OnHabitClickListenerImpl(), false);

        binding.contentMain.rvHabits.setLayoutManager(new LinearLayoutManager(this));
        binding.contentMain.rvHabits.setAdapter(adapter);
    }

    private class OnHabitClickListenerImpl implements HabitAdapter.OnHabitClickListener {
        @Override
        public void onHabitClick(Habit habit) {
            habit.setCompletedToday(!habit.isCompletedToday());
            // Persist the change so it survives rotation
            HabitRepository.getInstance().saveHabits();
            adapter.notifyDataSetChanged();
            updateProgress(true);
        }

        @Override
        public void onDeleteClick(Habit habit) { }

        @Override
        public void onCheckboxToggled(Habit habit, boolean isChecked) {
            // Persist the checkbox change and update progress
            HabitRepository.getInstance().saveHabits();
            adapter.notifyDataSetChanged();
            updateProgress(true);
        }
    }

    private void updateProgress(boolean animate) {
        int completed = HabitRepository.getInstance().getCompletedCount();
        int total = HabitRepository.getInstance().getTotalCount();
        int percentage = total == 0 ? 0 : (completed * 100) / total;

        if (animate) {
            ObjectAnimator animator = ObjectAnimator.ofInt(binding.contentMain.circularProgressBar, "progress", percentage);
            animator.setDuration(500);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        } else {
            binding.contentMain.circularProgressBar.setProgress(percentage);
        }

        binding.contentMain.tvPercentage.setText(percentage + "%");
        binding.contentMain.tvStatus.setText(completed + " done, " + (total - completed) + " left");

        // Handle Empty State
        if (total == 0) {
            binding.contentMain.tvEmptyState.setVisibility(View.VISIBLE);
            binding.contentMain.rvHabits.setVisibility(View.GONE);
        } else {
            binding.contentMain.tvEmptyState.setVisibility(View.GONE);
            binding.contentMain.rvHabits.setVisibility(View.VISIBLE);
        }

        // Success Celebration: Replace Toast with Confetti
        if (total > 0 && completed == total && !celebrationShownToday) {
            triggerConfetti();
            celebrationShownToday = true;
        } else if (completed < total) {
            celebrationShownToday = false;
        }
    }

    private void triggerConfetti() {
        EmitterConfig emitterConfig = new Emitter(5, TimeUnit.SECONDS).perSecond(30);
        Party party = new PartyFactory(emitterConfig)
                .angle(Angle.BOTTOM)
                .spread(Spread.ROUND)
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(0f, 30f)
                .position(0.5, -0.1) // Top center
                .build();
        
        binding.contentMain.konfettiView.start(party);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HabitRepository.getInstance().checkMidnightReset();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            updateProgress(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_manage_habits) {
            startActivity(new Intent(this, ManageHabitsActivity.class));
            return true;
        } else if (id == R.id.action_analytics) {
            startActivity(new Intent(this, AnalyticsActivity.class));
            return true;
        } else if (id == R.id.action_AI) {
            startActivity(new Intent(this, AICoachActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
