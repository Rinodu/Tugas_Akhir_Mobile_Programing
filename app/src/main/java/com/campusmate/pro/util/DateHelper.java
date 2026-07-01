package com.campusmate.pro.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {
    private static final Locale ID = new Locale("id", "ID");

    public static String now() {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", ID).format(new Date());
    }

    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String prettyToday() {
        return new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(new Date());
    }

    public static long parseDeadlineMillis(String date, String time) {
        if (date == null || date.trim().isEmpty()) return 0;
        String d = date.trim();
        String t = (time == null || time.trim().isEmpty()) ? "23:59" : time.trim();
        String[] patterns = new String[] {"yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm", "dd-MM-yyyy HH:mm"};
        for (String p : patterns) {
            try {
                Date parsed = new SimpleDateFormat(p, Locale.US).parse(d + " " + t);
                if (parsed != null) return parsed.getTime();
            } catch (ParseException ignored) {}
        }
        return 0;
    }

    public static boolean isToday(String date) {
        if (date == null) return false;
        long millis = parseDeadlineMillis(date, "12:00");
        if (millis == 0) return false;
        String key = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
        return todayKey().equals(key);
    }

    public static boolean isOverdue(String date, String time) {
        long millis = parseDeadlineMillis(date, time);
        return millis > 0 && millis < System.currentTimeMillis();
    }
}
