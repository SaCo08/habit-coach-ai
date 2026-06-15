package edu.touro.habitcoach;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private final List<Habit> habits;
    private final OnHabitClickListener listener;
    private final boolean isManagementMode;

    public interface OnHabitClickListener {
        void onHabitClick(Habit habit);
        void onDeleteClick(Habit habit);
        void onCheckboxToggled(Habit habit, boolean isChecked);
    }

    public HabitAdapter(List<Habit> habits, OnHabitClickListener listener, boolean isManagementMode) {
        this.habits = habits;
        this.listener = listener;
        this.isManagementMode = isManagementMode;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        holder.bind(habit, listener, isManagementMode);
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView emoji;
        private final TextView name;
        private final ImageView checkMark;
        private final CheckBox checkbox;
        private final ImageButton deleteButton;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.habitCard);
            emoji = itemView.findViewById(R.id.habitEmoji);
            name = itemView.findViewById(R.id.habitName);
            checkMark = itemView.findViewById(R.id.checkMark);
            checkbox = itemView.findViewById(R.id.habitCheckbox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Habit habit, OnHabitClickListener listener, boolean managementMode) {
            emoji.setText(habit.getEmoji());
            name.setText(habit.getName());

            if (managementMode) {
                // Management mode: show delete button only, hide checkbox
                deleteButton.setVisibility(View.VISIBLE);
                checkbox.setVisibility(View.GONE);
                checkMark.setVisibility(View.GONE);
                card.setCardBackgroundColor(Color.WHITE);
                deleteButton.setOnClickListener(v -> listener.onDeleteClick(habit));
            } else {
                // Normal mode: show checkbox, hide delete button
                deleteButton.setVisibility(View.GONE);
                checkbox.setVisibility(View.VISIBLE);
                
                // Set checkbox state based on completion
                checkbox.setChecked(habit.isCompletedToday());
                
                // Handle checkbox state changes
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    habit.setCompletedToday(isChecked);
                    listener.onCheckboxToggled(habit, isChecked);
                });
                
                // Update card color based on completion
                if (habit.isCompletedToday()) {
                    card.setCardBackgroundColor(itemView.getContext().getColor(R.color.warm_taupe));
                    checkMark.setVisibility(View.GONE);
                    name.setTextColor(Color.WHITE);
                } else {
                    card.setCardBackgroundColor(Color.WHITE);
                    checkMark.setVisibility(View.GONE);
                    name.setTextColor(Color.BLACK);
                }
            }
        }
    }
}
