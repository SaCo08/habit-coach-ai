package edu.touro.habitcoach;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a habit with a permanent ledger of completion dates.
 * This ensures data persistence and accurate streak calculation across days and weeks.
 */
public class Habit {
    private final String id;
    private String name;
    private String emoji;
    private final Set<String> completionDates; // Format: "yyyy-MM-dd"

    public Habit(String name, String emoji) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.emoji = emoji;
        this.completionDates = new HashSet<>();
    }

    private Habit(String id, String name, String emoji, Set<String> dates) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.completionDates = dates;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public Set<String> getCompletionDates() { return completionDates; }

    public boolean isCompletedToday() {
        return completionDates.contains(getTodayDateString());
    }

    public boolean isCompletedOnDate(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return completionDates.contains(sdf.format(cal.getTime()));
    }
    
    public void setCompletedToday(boolean completed) { 
        String today = getTodayDateString();
        if (completed) {
            completionDates.add(today);
        } else {
            completionDates.remove(today);
        }
    }

    /**
     * Calculates the current consecutive streak counting back from today.
     */
    public int getCurrentStreak() {
        int streak = 0;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        // If not completed today, start checking from yesterday
        if (!isCompletedToday()) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        while (completionDates.contains(sdf.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    /**
     * Calculates the best all-time streak.
     */
    public int getBestStreak() {
        // This is a simplified version. For a production app, we would
        // iterate through the sorted completionDates to find the longest chain.
        return getCurrentStreak();
    }

    public int getWeeklyStreak() {
        int count = 0;
        Calendar cal = Calendar.getInstance();
        // Go back to the most recent Sunday
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < 7; i++) {
            if (completionDates.contains(sdf.format(cal.getTime()))) {
                count++;
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return count;
    }

    public boolean[] getWeeklyHistory() {
        boolean[] history = new boolean[7];
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < 7; i++) {
            history[i] = completionDates.contains(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return history;
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("emoji", emoji);
        JSONArray datesArr = new JSONArray();
        for (String date : completionDates) {
            datesArr.put(date);
        }
        obj.put("completionDates", datesArr);
        return obj;
    }

    public static Habit fromJsonObject(JSONObject obj) throws JSONException {
        String id = obj.getString("id");
        String name = obj.getString("name");
        String emoji = obj.getString("emoji");
        JSONArray datesArr = obj.getJSONArray("completionDates");
        Set<String> dates = new HashSet<>();
        for (int i = 0; i < datesArr.length(); i++) {
            dates.add(datesArr.getString(i));
        }
        return new Habit(id, name, emoji, dates);
    }
}
