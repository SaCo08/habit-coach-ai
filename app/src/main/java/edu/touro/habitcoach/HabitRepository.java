package edu.touro.habitcoach;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HabitRepository {
    private static HabitRepository instance;
    private final List<Habit> habits;
    private final SharedPreferences prefs;
    private final Context context;

    private static final String PREFS_NAME = "HabitCoachPrefs";
    private static final String HABITS_KEY = "saved_habits";
    private static final String LAST_OPENED_DATE_KEY = "last_opened_date";
    private static final String ONBOARDING_COMPLETED_KEY = "onboarding_completed";
    private static final String AI_USER_ID_KEY = "ai_user_id";
    public static final String REMINDER_TIME_KEY = "reminder_time";
    
    private static final String GLOBAL_STREAK_KEY = "global_streak";
    private static final String LAST_STREAK_DATE_KEY = "last_streak_date";
    private static final String LAST_AI_ADVICE_KEY = "last_ai_advice";
    
    public static final String ACTION_DATE_CHANGED_REFRESH = "edu.touro.habitcoach.DATE_CHANGED";

    private HabitRepository(Context context) {
        this.context = context.getApplicationContext();
        this.habits = new ArrayList<>();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadHabits();
        checkMidnightReset();
        ensureAiUserId();
        migrateReminderTime();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new HabitRepository(context);
        }
    }

    public static synchronized HabitRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Repository must be initialized with context first");
        }
        return instance;
    }

    private void loadHabits() {
        String json = getSafeString(HABITS_KEY, null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                habits.clear();
                for (int i = 0; i < array.length(); i++) {
                    habits.add(Habit.fromJsonObject(array.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveHabits() {
        try {
            JSONArray array = new JSONArray();
            for (Habit h : habits) {
                array.put(h.toJsonObject());
            }
            prefs.edit().putString(HABITS_KEY, array.toString()).apply();
            updateGlobalStreak();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkMidnightReset() {
        String today = getTodayDateString();
        String lastOpenedDate = getSafeString(LAST_OPENED_DATE_KEY, "");

        if (!lastOpenedDate.isEmpty() && !today.equals(lastOpenedDate)) {
            resetHabitsForNewDay();
            if (isYesterday(lastOpenedDate)) {
                if (!wasPerfectDay(lastOpenedDate)) {
                    setGlobalStreak(0);
                }
            } else {
                setGlobalStreak(0);
            }

            Intent intent = new Intent(ACTION_DATE_CHANGED_REFRESH);
            context.sendBroadcast(intent);
        }
        
        prefs.edit().putString(LAST_OPENED_DATE_KEY, today).apply();
    }

    private void resetHabitsForNewDay() {
        for (Habit habit : habits) {
            habit.setCompletedToday(false);
        }
        saveHabits();
    }

    private boolean isYesterday(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = sdf.format(yesterday.getTime());
            return dateStr.equals(yesterdayStr);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean wasPerfectDay(String dateStr) {
        if (habits.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            for (Habit habit : habits) {
                if (!habit.isCompletedOnDate(cal)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getGlobalStreak() {
        return prefs.getInt(GLOBAL_STREAK_KEY, 0);
    }

    private void setGlobalStreak(int streak) {
        prefs.edit().putInt(GLOBAL_STREAK_KEY, streak).apply();
    }

    public void updateGlobalStreak() {
        if (habits.isEmpty()) return;
        int completed = getCompletedCount();
        int total = getTotalCount();
        if (completed == total && total > 0) {
            String today = getTodayDateString();
            String lastStreakDay = getSafeString(LAST_STREAK_DATE_KEY, "");
            if (!today.equals(lastStreakDay)) {
                int currentStreak = getGlobalStreak();
                setGlobalStreak(currentStreak + 1);
                prefs.edit().putString(LAST_STREAK_DATE_KEY, today).apply();
            }
        }
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void ensureAiUserId() {
        if (!prefs.contains(AI_USER_ID_KEY)) {
            prefs.edit().putString(AI_USER_ID_KEY, UUID.randomUUID().toString()).apply();
        }
    }

    public String getLastAiAdvice() {
        return getSafeString(LAST_AI_ADVICE_KEY, null);
    }

    public void setLastAiAdvice(String advice) {
        prefs.edit().putString(LAST_AI_ADVICE_KEY, advice).apply();
    }

    public List<Habit> getHabits() {
        return habits;
    }

    public void addHabit(Habit habit) {
        habits.add(habit);
        saveHabits();
    }

    public void removeHabit(Habit habit) {
        habits.remove(habit);
        saveHabits();
    }

    public int getCompletedCount() {
        int count = 0;
        for (Habit h : habits) if (h.isCompletedToday()) count++;
        return count;
    }

    public int getTotalCount() {
        return habits.size();
    }

    private String getSafeString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public String getReminderTime() {
        return getSafeString(REMINDER_TIME_KEY, "20:00");
    }

    public void setReminderTime(String time) {
        prefs.edit().putString(REMINDER_TIME_KEY, time).apply();
    }

    private void migrateReminderTime() {
        String time = getReminderTime();
        if (time.contains("AM") || time.contains("PM")) {
            int[] parsed = ReminderManager.parseTime(time);
            String newTime = String.format(Locale.US, "%02d:%02d", parsed[0], parsed[1]);
            setReminderTime(newTime);
        }
    }

    public void clearAnalytics() {
        setGlobalStreak(0);
        prefs.edit().remove(LAST_STREAK_DATE_KEY).apply();
        for (Habit h : habits) h.getCompletionDates().clear();
        saveHabits();
    }

    public void deleteAllHabits() {
        habits.clear();
        setGlobalStreak(0);
        prefs.edit().remove(LAST_STREAK_DATE_KEY).apply();
        saveHabits();
    }

    public int getPerfectDaysCount() {
        if (habits.isEmpty()) return 0;
        Map<String, Integer> completionCounts = new HashMap<>();
        for (Habit h : habits) {
            for (String date : h.getCompletionDates()) {
                completionCounts.put(date, completionCounts.getOrDefault(date, 0) + 1);
            }
        }
        int perfectDays = 0;
        int totalHabits = habits.size();
        for (int count : completionCounts.values()) {
            if (count == totalHabits) perfectDays++;
        }
        return perfectDays;
    }

    public Habit getBestHabit() {
        if (habits.isEmpty()) return null;
        Habit best = habits.get(0);
        for (Habit h : habits) {
            if (h.getCompletionDates().size() > best.getCompletionDates().size()) {
                best = h;
            }
        }
        return best;
    }
}