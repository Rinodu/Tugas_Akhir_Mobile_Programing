package com.campusmate.pro.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.campusmate.pro.model.FocusSessionModel;
import com.campusmate.pro.model.MediaModel;
import com.campusmate.pro.model.NoteModel;
import com.campusmate.pro.model.TaskModel;
import com.campusmate.pro.model.UserProfileModel;
import com.campusmate.pro.util.DateHelper;
import com.campusmate.pro.util.IdHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final String PREF = "campusmate_pro_local";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_FOCUS = "focus";
    private static final String KEY_SEEDED = "seeded";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public DataStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public UserProfileModel getProfile() {
        String json = prefs.getString(KEY_PROFILE, null);
        if (json == null) return null;
        return gson.fromJson(json, UserProfileModel.class);
    }

    public void saveProfile(UserProfileModel profile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply();
    }

    public List<TaskModel> getTasks() {
        Type type = new TypeToken<List<TaskModel>>(){}.getType();
        return readList(KEY_TASKS, type);
    }

    public void saveTasks(List<TaskModel> data) {
        writeList(KEY_TASKS, data);
    }

    public List<NoteModel> getNotes() {
        Type type = new TypeToken<List<NoteModel>>(){}.getType();
        return readList(KEY_NOTES, type);
    }

    public void saveNotes(List<NoteModel> data) {
        writeList(KEY_NOTES, data);
    }

    public List<MediaModel> getMedia() {
        Type type = new TypeToken<List<MediaModel>>(){}.getType();
        return readList(KEY_MEDIA, type);
    }

    public void saveMedia(List<MediaModel> data) {
        writeList(KEY_MEDIA, data);
    }

    public List<FocusSessionModel> getFocusSessions() {
        Type type = new TypeToken<List<FocusSessionModel>>(){}.getType();
        return readList(KEY_FOCUS, type);
    }

    public void saveFocusSessions(List<FocusSessionModel> data) {
        writeList(KEY_FOCUS, data);
    }

    private <T> List<T> readList(String key, Type type) {
        String json = prefs.getString(key, "[]");
        List<T> result = gson.fromJson(json, type);
        return result == null ? new ArrayList<>() : result;
    }

    private void writeList(String key, Object data) {
        prefs.edit().putString(key, gson.toJson(data)).apply();
    }

    public void resetActivityData() {
        prefs.edit()
                .putString(KEY_TASKS, "[]")
                .putString(KEY_NOTES, "[]")
                .putString(KEY_MEDIA, "[]")
                .putString(KEY_FOCUS, "[]")
                .apply();
    }

    public void deleteAccount() {
        prefs.edit().clear().apply();
    }

    public void seedIfNeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false)) return;
        saveProfile(new UserProfileModel("Riyadhu Jawhar", "Universitas Teknologi", "Teknik Informatika", 5, "IPK 3.7 semester ini", 4));

        List<TaskModel> tasks = new ArrayList<>();
        tasks.add(task("Laporan Praktikum Basis Data", "Basis Data", "Susun laporan praktikum normalisasi database.", "2026-07-20", "23:59", "Tinggi", "Belum selesai"));
        tasks.add(task("Tugas UTS Pemrograman Mobile", "Pemrograman Mobile", "Buat aplikasi Android dengan minimal 5 modul.", "2026-07-22", "23:59", "Tinggi", "Belum selesai"));
        tasks.add(task("Presentasi Kelompok AI", "Kecerdasan Buatan", "Siapkan slide dan pembagian materi.", "2026-07-25", "10:00", "Sedang", "Belum selesai"));
        tasks.add(task("Resume Bab 4 Jaringan Komputer", "Jaringan Komputer", "Rangkum konsep routing dan subnetting.", "2026-07-18", "20:00", "Sedang", "Selesai"));
        tasks.add(task("Kuis Online Statistika", "Statistika", "Kerjakan kuis distribusi normal.", "2026-07-19", "21:00", "Rendah", "Selesai"));
        saveTasks(tasks);

        List<NoteModel> notes = new ArrayList<>();
        notes.add(note("Normalisasi Database", "Basis Data", "Normalisasi adalah proses mengorganisasi data dalam database untuk mengurangi redundansi. 1NF: Setiap kolom hanya berisi nilai atomik. 2NF: Setiap atribut non-kunci bergantung penuh pada primary key. 3NF: Tidak ada dependensi transitif.", "penting, ujian, materi", true));
        notes.add(note("Activity Lifecycle Android", "Pemrograman Mobile", "Lifecycle Activity Android: onCreate() saat Activity pertama dibuat, onStart() saat mulai terlihat, onResume() saat siap berinteraksi, onPause() saat sebagian tertutup, onStop() saat tidak terlihat, onDestroy() saat dihancurkan.", "android, mobile", false));
        notes.add(note("Rumus Distribusi Normal", "Statistika", "Distribusi Normal atau Gaussian adalah kurva lonceng simetris. Mean = Median = Modus. Sekitar 68% data berada dalam 1 standar deviasi, 95% dalam 2 standar deviasi.", "rumus, statistik", false));
        saveNotes(notes);

        List<FocusSessionModel> sessions = new ArrayList<>();
        sessions.add(session(25, 0, "17 Jul, 15:00", 1));
        sessions.add(session(25, 0, "17 Jul, 21:00", 0));
        sessions.add(session(50, 0, "16 Jul, 16:00", 2));
        saveFocusSessions(sessions);
        saveMedia(new ArrayList<MediaModel>());
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    private TaskModel task(String title, String course, String desc, String date, String time, String priority, String status) {
        TaskModel t = new TaskModel();
        t.id = IdHelper.newId();
        t.title = title;
        t.courseName = course;
        t.description = desc;
        t.deadlineDate = date;
        t.deadlineTime = time;
        t.priority = priority;
        t.status = status;
        t.createdAt = DateHelper.now();
        t.completedAt = "Selesai".equals(status) ? DateHelper.now() : "";
        t.reminderType = "none";
        return t;
    }

    private NoteModel note(String title, String course, String content, String tags, boolean pinned) {
        NoteModel n = new NoteModel();
        n.id = IdHelper.newId();
        n.title = title;
        n.courseName = course;
        n.content = content;
        n.tags = tags;
        n.isPinned = pinned;
        n.createdAt = "25 Jun 2026, 18:11";
        n.updatedAt = n.createdAt;
        return n;
    }

    private FocusSessionModel session(int minutes, int breakMinutes, String startedAt, int warnings) {
        FocusSessionModel s = new FocusSessionModel();
        s.id = IdHelper.newId();
        s.focusMinutes = minutes;
        s.breakMinutes = breakMinutes;
        s.startedAt = startedAt;
        s.endedAt = DateHelper.now();
        s.completed = true;
        s.movementWarningCount = warnings;
        return s;
    }
}
