package edu.touro.habitcoach;

import android.app.Application;

public class HabitCoachApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the repository once for the entire app lifecycle
        HabitRepository.init(this);
    }
}
