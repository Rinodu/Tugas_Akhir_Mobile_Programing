package com.campusmate.pro;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.MotionEvent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmate.pro.adapter.FocusHistoryAdapter;
import com.campusmate.pro.adapter.MediaAdapter;
import com.campusmate.pro.adapter.NoteAdapter;
import com.campusmate.pro.adapter.TaskAdapter;
import com.campusmate.pro.model.FocusSessionModel;
import com.campusmate.pro.model.MediaModel;
import com.campusmate.pro.model.NoteModel;
import com.campusmate.pro.model.TaskModel;
import com.campusmate.pro.model.UserProfileModel;
import com.campusmate.pro.notification.NotificationHelper;
import com.campusmate.pro.storage.DataStore;
import com.campusmate.pro.util.DateHelper;
import com.campusmate.pro.util.IdHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private int BG;
    private int CARD;
    private int CARD_STROKE;
    private int WHITE; // foreground text color; white in dark mode, near-black in light mode
    private int MUTED;
    private int SUBTLE;
    private int NAV_BG;
    private int NAV_SELECTED;
    private int INPUT_BG;
    private boolean darkMode = true;

    private static final int TEAL = Color.parseColor("#16B8B8");
    private static final int CYAN = Color.parseColor("#109DB8");

    private static final int REQ_PICK_MEDIA = 4401;
    private static final int REQ_SPEECH = 4402;

    private DataStore store;
    private FrameLayout content;
    private LinearLayout bottomNav;
    private int selectedNavId = R.id.nav_dashboard;
    private String currentRoot = "dashboard";
    private String taskFilter = "Semua";
    private String noteFilter = "Semua";
    private String taskSearch = "";
    private String noteSearch = "";
    private String mediaSearch = "";

    private Uri pendingMediaUri;
    private String pendingMediaType = "Image";
    private TextView pendingMediaLabel;

    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;

    private Handler timerHandler = new Handler();
    private boolean timerRunning = false;
    private int selectedMinutes = 25;
    private int secondsLeft = 25 * 60;
    private String timerType = "Fokus";
    private long focusStartedAt = 0;
    private int movementWarnings = 0;
    private TextView timerText;
    private TextView timerSubText;
    private Button playButton;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastAccel = 0f;
    private long lastWarningAt = 0L;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            if (secondsLeft > 0) {
                secondsLeft--;
                updateTimerLabels();
                timerHandler.postDelayed(this, 1000);
            } else {
                completeFocusSession();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new DataStore(this);
        darkMode = getSharedPreferences("ui_prefs", MODE_PRIVATE).getBoolean("dark_mode", true);
        applyThemePalette();
        store.seedIfNeeded();
        NotificationHelper.createChannel(this);
        requestInitialPermissions();
        setupTts();
        setupSensor();
        buildRoot();
        showDashboard();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCurrentRoot();
            }
        });
    }

    private void requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 300);
        }
    }

    private void setupTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(new Locale("id", "ID"));
        });
    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setBackground(roundedTopBg(NAV_BG, CARD_STROKE, 24));
        bottomNav.setPadding(dp(6), dp(6), dp(6), dp(6));
        bottomNav.addView(navItem(R.id.nav_dashboard, "▦", "Dashboard"), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(navItem(R.id.nav_tasks, "☑", "Tugas"), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(navItem(R.id.nav_notes, "□", "Catatan"), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(navItem(R.id.nav_media, "▧", "Media"), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(navItem(R.id.nav_focus, "⏱", "Fokus"), new LinearLayout.LayoutParams(0, -1, 1));
        bottomNav.addView(navItem(R.id.nav_profile, "♙", "Profil"), new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(72)));
        setContentView(root);
    }

    private View navItem(int id, String icon, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(0, dp(4), 0, dp(4));
        item.setTag(id);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(17);
        iconView.setGravity(Gravity.CENTER);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(10);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        item.addView(iconView, new LinearLayout.LayoutParams(-1, 0, 1));
        item.addView(labelView, new LinearLayout.LayoutParams(-1, 0, 1));

        applyPressAnimation(item);
        item.setOnClickListener(v -> {
            int navId = (int) v.getTag();
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                if (navId == R.id.nav_dashboard) showDashboard();
                else if (navId == R.id.nav_tasks) showTasks();
                else if (navId == R.id.nav_notes) showNotes();
                else if (navId == R.id.nav_media) showMedia();
                else if (navId == R.id.nav_focus) showFocus();
                else if (navId == R.id.nav_profile) showProfile();
            }).start();
        });
        return item;
    }

    private void refreshBottomNav() {
        if (bottomNav == null) return;
        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View child = bottomNav.getChildAt(i);
            Object tag = child.getTag();
            boolean selected = tag instanceof Integer && ((Integer) tag) == selectedNavId;
            child.setBackground(selected ? rippleBg(NAV_SELECTED, CARD_STROKE, 18) : roundedBg(Color.TRANSPARENT, Color.TRANSPARENT, 18));
            child.animate().scaleX(selected ? 1.04f : 1f).scaleY(selected ? 1.04f : 1f).setDuration(160).start();
            if (child instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) child;
                for (int j = 0; j < ll.getChildCount(); j++) {
                    View tv = ll.getChildAt(j);
                    if (tv instanceof TextView) {
                        ((TextView) tv).setTextColor(selected ? WHITE : SUBTLE);
                    }
                }
            }
        }
    }

    private void setScreen(View view) {
        content.removeAllViews();
        view.setAlpha(0f);
        view.setTranslationX(dp(12));
        content.addView(view, new FrameLayout.LayoutParams(-1, -1));
        view.animate().alpha(1f).translationX(0).setDuration(180).start();
    }

    private void showCurrentRoot() {
        if ("tasks".equals(currentRoot)) showTasks();
        else if ("notes".equals(currentRoot)) showNotes();
        else if ("media".equals(currentRoot)) showMedia();
        else if ("focus".equals(currentRoot)) showFocus();
        else if ("profile".equals(currentRoot)) showProfile();
        else showDashboard();
    }

    private void selectNav(int id) {
        selectedNavId = id;
        refreshBottomNav();
    }

    // ===================== DASHBOARD =====================
    private void showDashboard() {
        currentRoot = "dashboard";
        selectNav(R.id.nav_dashboard);
        UserProfileModel profile = store.getProfile();
        List<TaskModel> tasks = store.getTasks();
        List<NoteModel> notes = store.getNotes();
        List<MediaModel> media = store.getMedia();
        List<FocusSessionModel> sessions = store.getFocusSessions();
        int active = 0, done = 0, focusMinutes = 0;
        for (TaskModel t : tasks) if (t.isDone()) done++; else active++;
        for (FocusSessionModel s : sessions) if (s.completed) focusMinutes += s.focusMinutes;
        int completion = tasks.isEmpty() ? 0 : Math.round(done * 100f / tasks.size());

        LinearLayout body = page();
        body.addView(text(DateHelper.prettyToday(), 14, MUTED, false));
        body.addView(text("Halo, " + safe(profile == null ? "Mahasiswa" : profile.studentName) + " 👋", 25, WHITE, true));
        addSpace(body, 30);

        LinearLayout grid1 = row();
        grid1.addView(statCard("☑", String.valueOf(active), "Tugas Aktif"), new LinearLayout.LayoutParams(0, -2, 1));
        grid1.addView(statCard("↗", String.valueOf(done), "Tugas Selesai"), new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(grid1);
        addSpace(body, 18);

        LinearLayout grid2 = row();
        grid2.addView(statCard("□", String.valueOf(notes.size()), "Catatan"), new LinearLayout.LayoutParams(0, -2, 1));
        grid2.addView(statCard("⏱", String.valueOf(focusMinutes), "Menit Fokus"), new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(grid2);
        addSpace(body, 34);

        body.addView(text("✧  Rekomendasi Hari Ini", 16, WHITE, true));
        for (String rec : recommendations(tasks, notes, focusMinutes, completion)) {
            TextView r = text("•  " + rec, 14, MUTED, false);
            r.setPadding(0, dp(8), 0, 0);
            body.addView(r);
        }
        addSpace(body, 45);

        LinearLayout grid3 = row();
        grid3.addView(statCard("▧", String.valueOf(media.size()), "Total Media"), new LinearLayout.LayoutParams(0, -2, 1));
        grid3.addView(statCard("↗", completion + "%", "Penyelesaian"), new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(grid3);
        setScreen(scroll(body));
    }

    private List<String> recommendations(List<TaskModel> tasks, List<NoteModel> notes, int focusMinutes, int completion) {
        List<String> r = new ArrayList<>();
        int today = 0, high = 0;
        for (TaskModel t : tasks) {
            if (!t.isDone() && DateHelper.isToday(t.deadlineDate)) today++;
            if (!t.isDone() && "Tinggi".equals(t.priority)) high++;
        }
        if (today > 0) r.add("Ada " + today + " tugas deadline hari ini. Prioritaskan yang paling penting.");
        if (high > 0) r.add("Selesaikan tugas prioritas tinggi terlebih dahulu.");
        if (notes.isEmpty()) r.add("Buat catatan pertama untuk materi kuliah.");
        if (focusMinutes == 0) r.add("Mulai sesi fokus 25 menit hari ini.");
        if (completion >= 80) r.add("Penyelesaian tugas sudah tinggi. Pertahankan ritmenya.");
        if (r.isEmpty()) r.add("Kondisi akademik stabil. Lanjutkan task terdekat.");
        return r;
    }

    private View statCard(String icon, String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(8), dp(10), dp(8), dp(10));
        box.setBackground(roundedBg(Color.TRANSPARENT, Color.TRANSPARENT, 18));
        applyPressAnimation(box);
        TextView ic = text(icon, 22, TEAL, true);
        ic.setGravity(Gravity.CENTER);
        box.addView(ic, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(value, 25, WHITE, true));
        col.addView(text(label, 12, MUTED, false));
        box.addView(col);
        return box;
    }

    // ===================== TASKS =====================
    private void showTasks() {
        currentRoot = "tasks";
        selectNav(R.id.nav_tasks);
        LinearLayout body = page();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Tugas Kuliah", 22, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = primaryButton("+ Tambah");
        add.setOnClickListener(v -> showTaskForm(null));
        header.addView(add, new LinearLayout.LayoutParams(dp(110), dp(52)));
        body.addView(header);
        addSpace(body, 14);

        EditText search = input("Cari tugas...");
        search.setText(taskSearch);
        search.setSingleLine(true);
        search.setOnEditorActionListener((v, actionId, event) -> { taskSearch = v.getText().toString(); hideKeyboard(v); showTasks(); return true; });
        search.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) { taskSearch = ((EditText)v).getText().toString(); showTasks(); } });
        body.addView(search);
        addSpace(body, 14);

        LinearLayout filters = row();
        for (String f : new String[]{"Semua", "Aktif", "Selesai", "Hari Ini"}) {
            Button b = chip(f, f.equals(taskFilter));
            b.setOnClickListener(v -> { taskFilter = f; showTasks(); });
            filters.addView(b);
        }
        body.addView(filters);
        addSpace(body, 12);

        List<TaskModel> filtered = filterTasks(store.getTasks());
        if (filtered.isEmpty()) {
            body.addView(emptyState("☑", "Belum ada tugas", "Tekan tombol + untuk membuat tugas pertama."));
        } else {
            RecyclerView rv = new RecyclerView(this);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setNestedScrollingEnabled(false);
            rv.setAdapter(new TaskAdapter(this, filtered, new TaskAdapter.Callback() {
                @Override public void onToggle(TaskModel task) { toggleTask(task); }
                @Override public void onOpen(TaskModel task) { showTaskDetail(task.id); }
            }));
            body.addView(rv, new LinearLayout.LayoutParams(-1, -2));
        }
        setScreen(scroll(body));
    }

    private List<TaskModel> filterTasks(List<TaskModel> all) {
        List<TaskModel> out = new ArrayList<>();
        String q = taskSearch == null ? "" : taskSearch.toLowerCase(Locale.US);
        for (TaskModel t : all) {
            boolean ok = true;
            if ("Aktif".equals(taskFilter)) ok = !t.isDone();
            if ("Selesai".equals(taskFilter)) ok = t.isDone();
            if ("Hari Ini".equals(taskFilter)) ok = DateHelper.isToday(t.deadlineDate);
            boolean match = q.isEmpty() || safe(t.title).toLowerCase(Locale.US).contains(q) || safe(t.courseName).toLowerCase(Locale.US).contains(q);
            if (ok && match) out.add(t);
        }
        Collections.sort(out, (a, b) -> {
            if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
            long da = DateHelper.parseDeadlineMillis(a.deadlineDate, a.deadlineTime);
            long db = DateHelper.parseDeadlineMillis(b.deadlineDate, b.deadlineTime);
            return Long.compare(da, db);
        });
        return out;
    }

    private void toggleTask(TaskModel task) {
        List<TaskModel> tasks = store.getTasks();
        for (TaskModel t : tasks) {
            if (t.id.equals(task.id)) {
                t.status = t.isDone() ? "Belum selesai" : "Selesai";
                t.completedAt = t.isDone() ? DateHelper.now() : "";
                break;
            }
        }
        store.saveTasks(tasks);
        Snackbar.make(content, "Status tugas diperbarui", Snackbar.LENGTH_SHORT).show();
        showTasks();
    }

    private void showTaskForm(@Nullable TaskModel existing) {
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false);
        back.setPadding(0, 0, 0, dp(24));
        back.setOnClickListener(v -> showTasks());
        body.addView(back);
        body.addView(text(existing == null ? "Tugas Baru" : "Edit Tugas", 22, WHITE, true));
        addSpace(body, 22);

        EditText title = input("Contoh: Laporan Praktikum");
        EditText course = input("Contoh: Pemrograman Mobile");
        EditText desc = input("Detail tugas...");
        desc.setMinLines(3);
        EditText date = readOnlyPickerField("Pilih tanggal deadline");
        EditText time = readOnlyPickerField("Pilih waktu deadline");
        EditText reminderDate = readOnlyPickerField("Pilih tanggal pengingat");
        EditText reminderTime = readOnlyPickerField("Pilih waktu pengingat");
        Spinner priority = spinner(new String[]{"Rendah", "Sedang", "Tinggi"});

        attachDatePicker(date);
        attachTimePicker(time);
        attachDatePicker(reminderDate);
        attachTimePicker(reminderTime);

        if (existing != null) {
            title.setText(existing.title);
            course.setText(existing.courseName);
            desc.setText(existing.description);
            date.setText(existing.deadlineDate);
            time.setText(existing.deadlineTime);
            reminderDate.setText(safe(existing.reminderDate));
            reminderTime.setText(safe(existing.reminderTime));
            setSpinner(priority, existing.priority);
        } else {
            setSpinner(priority, "Sedang");
            time.setText("23:59");
        }

        addField(body, "Judul Tugas *", title);
        addField(body, "Mata Kuliah *", course);
        addField(body, "Deskripsi", desc);

        LinearLayout dt = row();
        LinearLayout.LayoutParams colParam = new LinearLayout.LayoutParams(0, -2, 1);
        colParam.setMargins(0, 0, dp(10), 0);
        LinearLayout.LayoutParams lastColParam = new LinearLayout.LayoutParams(0, -2, 1);
        LinearLayout dcol = new LinearLayout(this); dcol.setOrientation(LinearLayout.VERTICAL); addField(dcol, "Deadline *", date);
        LinearLayout tcol = new LinearLayout(this); tcol.setOrientation(LinearLayout.VERTICAL); addField(tcol, "Waktu", time);
        dt.addView(dcol, colParam);
        dt.addView(tcol, lastColParam);
        body.addView(dt);

        addField(body, "Prioritas", priority);

        LinearLayout reminderRow = row();
        LinearLayout.LayoutParams reminderColParam = new LinearLayout.LayoutParams(0, -2, 1);
        reminderColParam.setMargins(0, 0, dp(10), 0);
        LinearLayout.LayoutParams reminderLastColParam = new LinearLayout.LayoutParams(0, -2, 1);
        LinearLayout rDateCol = new LinearLayout(this); rDateCol.setOrientation(LinearLayout.VERTICAL); addField(rDateCol, "Pengingat Tanggal", reminderDate);
        LinearLayout rTimeCol = new LinearLayout(this); rTimeCol.setOrientation(LinearLayout.VERTICAL); addField(rTimeCol, "Pengingat Waktu", reminderTime);
        reminderRow.addView(rDateCol, reminderColParam);
        reminderRow.addView(rTimeCol, reminderLastColParam);
        body.addView(reminderRow);
        body.addView(text("Pengingat akan memunculkan notifikasi dan bunyi alarm sebelum deadline terjadi.", 12, MUTED, false));
        addSpace(body, 8);

        Button save = primaryButton(existing == null ? "Tambah Tugas" : "Simpan Perubahan");
        save.setOnClickListener(v -> {
            if (title.getText().toString().trim().isEmpty() || course.getText().toString().trim().isEmpty() || date.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Judul, mata kuliah, dan deadline wajib diisi.", Toast.LENGTH_SHORT).show(); return;
            }
            long deadlineMillis = DateHelper.parseDeadlineMillis(date.getText().toString().trim(), time.getText().toString().trim());
            if (deadlineMillis == 0) {
                Toast.makeText(this, "Tanggal atau waktu deadline belum valid.", Toast.LENGTH_SHORT).show(); return;
            }
            String rDate = reminderDate.getText().toString().trim();
            String rTime = reminderTime.getText().toString().trim();
            if ((!rDate.isEmpty() && rTime.isEmpty()) || (rDate.isEmpty() && !rTime.isEmpty())) {
                Toast.makeText(this, "Pengingat harus berisi tanggal dan waktu sekaligus.", Toast.LENGTH_SHORT).show(); return;
            }
            if (!rDate.isEmpty()) {
                long reminderMillis = DateHelper.parseDeadlineMillis(rDate, rTime);
                if (reminderMillis == 0) {
                    Toast.makeText(this, "Tanggal atau waktu pengingat belum valid.", Toast.LENGTH_SHORT).show(); return;
                }
                if (reminderMillis >= deadlineMillis) {
                    Toast.makeText(this, "Pengingat harus lebih awal dari deadline.", Toast.LENGTH_SHORT).show(); return;
                }
            }
            List<TaskModel> tasks = store.getTasks();
            TaskModel t = existing == null ? new TaskModel() : existing;
            if (existing == null) { t.id = IdHelper.newId(); t.createdAt = DateHelper.now(); t.status = "Belum selesai"; }
            t.title = title.getText().toString().trim();
            t.courseName = course.getText().toString().trim();
            t.description = desc.getText().toString().trim();
            t.deadlineDate = date.getText().toString().trim();
            t.deadlineTime = time.getText().toString().trim();
            t.priority = priority.getSelectedItem().toString();
            t.reminderDate = rDate;
            t.reminderTime = rTime;
            t.reminderType = (rDate.isEmpty() ? "none" : "custom");
            if (existing == null) tasks.add(t); else replaceTask(tasks, t);
            store.saveTasks(tasks);
            scheduleReminderIfNeeded(t);
            Toast.makeText(this, "Tugas berhasil disimpan", Toast.LENGTH_SHORT).show();
            showTasks();
        });
        body.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));
        setScreen(scroll(body));
    }

    private void replaceTask(List<TaskModel> tasks, TaskModel updated) {
        for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).id.equals(updated.id)) { tasks.set(i, updated); return; }
    }

    private void scheduleReminderIfNeeded(TaskModel t) {
        if (safe(t.reminderDate).isEmpty() || safe(t.reminderTime).isEmpty()) return;
        long trigger = DateHelper.parseDeadlineMillis(t.reminderDate, t.reminderTime);
        long deadline = DateHelper.parseDeadlineMillis(t.deadlineDate, t.deadlineTime);
        if (trigger == 0 || deadline == 0 || trigger >= deadline) return;
        boolean scheduled = NotificationHelper.scheduleTaskReminder(this, trigger, t.title, t.courseName);
        if (!scheduled) {
            Toast.makeText(this, "Tugas tersimpan, tetapi pengingat tidak bisa dijadwalkan di perangkat ini.", Toast.LENGTH_LONG).show();
        }
    }

    private void showTaskDetail(String id) {
        TaskModel task = findTask(id);
        if (task == null) { showTasks(); return; }
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false);
        back.setOnClickListener(v -> showTasks());
        body.addView(back);
        addSpace(body, 20);
        LinearLayout h = row(); h.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(text(task.title, 23, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button edit = darkButton("Edit"); edit.setOnClickListener(v -> showTaskForm(task)); h.addView(edit);
        body.addView(h);
        addSpace(body, 12);
        body.addView(infoCard("Mata Kuliah", task.courseName));
        body.addView(infoCard("Deadline", task.deadlineDate + " " + safe(task.deadlineTime)));
        body.addView(infoCard("Prioritas", task.priority));
        body.addView(infoCard("Status", task.status));
        body.addView(infoCard("Pengingat", safe(task.reminderDate).isEmpty() ? "Tidak ada pengingat" : (task.reminderDate + " " + safe(task.reminderTime))));
        body.addView(infoCard("Deskripsi", safe(task.description).isEmpty() ? "Tidak ada deskripsi." : task.description));
        Button toggle = primaryButton(task.isDone() ? "Tandai Belum Selesai" : "Tandai Selesai");
        toggle.setOnClickListener(v -> { toggleTask(task); });
        body.addView(toggle, new LinearLayout.LayoutParams(-1, dp(56)));
        Button del = dangerButton("Hapus Tugas");
        del.setOnClickListener(v -> confirm("Hapus tugas?", "Data tugas akan dihapus permanen.", () -> deleteTask(task.id)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56)); lp.setMargins(0, dp(12), 0, 0); body.addView(del, lp);
        setScreen(scroll(body));
    }

    private TaskModel findTask(String id) {
        for (TaskModel t : store.getTasks()) if (t.id.equals(id)) return t;
        return null;
    }

    private void deleteTask(String id) {
        List<TaskModel> tasks = store.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) if (tasks.get(i).id.equals(id)) tasks.remove(i);
        store.saveTasks(tasks);
        Snackbar.make(content, "Tugas dihapus", Snackbar.LENGTH_SHORT).show();
        showTasks();
    }

    // ===================== NOTES =====================
    private void showNotes() {
        currentRoot = "notes";
        selectNav(R.id.nav_notes);
        LinearLayout body = page();
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Catatan Belajar", 22, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = primaryButton("+ Tambah"); add.setOnClickListener(v -> showNoteForm(null)); header.addView(add, new LinearLayout.LayoutParams(dp(110), dp(52)));
        body.addView(header);
        addSpace(body, 14);
        EditText search = input("Cari catatan..."); search.setText(noteSearch);
        search.setSingleLine(true);
        search.setOnEditorActionListener((v, actionId, event) -> { noteSearch = v.getText().toString(); hideKeyboard(v); showNotes(); return true; });
        search.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) { noteSearch = ((EditText)v).getText().toString(); showNotes(); } });
        body.addView(search);
        addSpace(body, 14);
        LinearLayout filters = row();
        List<String> courses = noteCourses(); courses.add(0, "Semua");
        for (String f : courses) {
            Button b = chip(f, f.equals(noteFilter)); b.setOnClickListener(v -> { noteFilter = f; showNotes(); }); filters.addView(b);
        }
        body.addView(horizontalScroll(filters));
        addSpace(body, 12);
        List<NoteModel> filtered = filterNotes(store.getNotes());
        if (filtered.isEmpty()) body.addView(emptyState("□", "Belum ada catatan", "Tekan tombol + untuk membuat catatan pertama."));
        else {
            RecyclerView rv = new RecyclerView(this); rv.setLayoutManager(new LinearLayoutManager(this)); rv.setNestedScrollingEnabled(false);
            rv.setAdapter(new NoteAdapter(this, filtered, note -> showNoteDetail(note.id)));
            body.addView(rv);
        }
        setScreen(scroll(body));
    }

    private List<String> noteCourses() {
        Set<String> set = new HashSet<>();
        for (NoteModel n : store.getNotes()) if (!safe(n.courseName).isEmpty()) set.add(n.courseName);
        return new ArrayList<>(set);
    }

    private List<NoteModel> filterNotes(List<NoteModel> all) {
        List<NoteModel> out = new ArrayList<>();
        String q = noteSearch == null ? "" : noteSearch.toLowerCase(Locale.US);
        for (NoteModel n : all) {
            boolean ok = "Semua".equals(noteFilter) || safe(n.courseName).equals(noteFilter);
            boolean match = q.isEmpty() || safe(n.title).toLowerCase(Locale.US).contains(q) || safe(n.content).toLowerCase(Locale.US).contains(q);
            if (ok && match) out.add(n);
        }
        Collections.sort(out, (a, b) -> Boolean.compare(b.isPinned, a.isPinned));
        return out;
    }

    private void showNoteForm(@Nullable NoteModel existing) {
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false); back.setOnClickListener(v -> showNotes()); body.addView(back);
        addSpace(body, 24);
        body.addView(text(existing == null ? "Catatan Baru" : "Edit Catatan", 22, WHITE, true));
        addSpace(body, 22);
        EditText title = input("Judul catatan");
        EditText course = input("Contoh: Basis Data");
        EditText contentInput = input("Tulis catatan di sini atau gunakan input suara..."); contentInput.setMinLines(8); contentInput.setGravity(Gravity.TOP);
        EditText tags = input("penting, ujian, materi");
        CheckBox pinned = new CheckBox(this); pinned.setText("Pin Catatan"); pinned.setTextColor(WHITE);
        if (existing != null) { title.setText(existing.title); course.setText(existing.courseName); contentInput.setText(existing.content); tags.setText(existing.tags); pinned.setChecked(existing.isPinned); }
        addField(body, "Judul *", title);
        addField(body, "Mata Kuliah", course);
        LinearLayout labelRow = row(); labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.addView(text("Isi Catatan *", 14, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button voice = darkButton("🎙 Suara");
        voice.setOnClickListener(v -> startSpeechInput());
        labelRow.addView(voice, new LinearLayout.LayoutParams(dp(110), dp(48)));
        body.addView(labelRow);
        body.addView(contentInput);
        contentInput.setTag("note_content");
        addField(body, "Tags", tags);
        body.addView(pinned);
        Button save = primaryButton(existing == null ? "Simpan Catatan" : "Simpan Perubahan");
        save.setOnClickListener(v -> {
            EditText ci = findNoteContentEditText();
            String c = ci == null ? contentInput.getText().toString() : ci.getText().toString();
            if (title.getText().toString().trim().isEmpty() || c.trim().isEmpty()) { Toast.makeText(this, "Judul dan isi catatan wajib diisi.", Toast.LENGTH_SHORT).show(); return; }
            List<NoteModel> notes = store.getNotes();
            NoteModel n = existing == null ? new NoteModel() : existing;
            if (existing == null) { n.id = IdHelper.newId(); n.createdAt = DateHelper.now(); }
            n.title = title.getText().toString().trim(); n.courseName = course.getText().toString().trim(); n.content = c.trim(); n.tags = tags.getText().toString().trim(); n.isPinned = pinned.isChecked(); n.updatedAt = DateHelper.now();
            if (existing == null) notes.add(n); else replaceNote(notes, n);
            store.saveNotes(notes); Toast.makeText(this, "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show(); showNotes();
        });
        body.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));
        setScreen(scroll(body));
    }

    private EditText findNoteContentEditText() {
        return findTaggedEditText(content, "note_content");
    }

    private EditText findTaggedEditText(View root, String tag) {
        if (root instanceof EditText && tag.equals(root.getTag())) return (EditText) root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                EditText found = findTaggedEditText(vg.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void startSpeechInput() {
        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 301);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan catatan...");
        try { startActivityForResult(intent, REQ_SPEECH); }
        catch (Exception e) { Toast.makeText(this, "Speech Recognition tidak tersedia di perangkat ini.", Toast.LENGTH_SHORT).show(); }
    }

    private void replaceNote(List<NoteModel> notes, NoteModel updated) {
        for (int i = 0; i < notes.size(); i++) if (notes.get(i).id.equals(updated.id)) { notes.set(i, updated); return; }
    }

    private NoteModel findNote(String id) { for (NoteModel n : store.getNotes()) if (n.id.equals(id)) return n; return null; }

    private void showNoteDetail(String id) {
        NoteModel n = findNote(id); if (n == null) { showNotes(); return; }
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false); back.setOnClickListener(v -> { if (textToSpeech != null) textToSpeech.stop(); showNotes(); }); body.addView(back);
        addSpace(body, 22);
        LinearLayout h = row(); h.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(text(n.title, 23, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button edit = darkButton("Edit"); edit.setOnClickListener(v -> showNoteForm(n)); h.addView(edit);
        body.addView(h);
        body.addView(text(safe(n.courseName), 13, MUTED, false));
        addSpace(body, 15);
        body.addView(infoCard("Isi Catatan", n.content));
        body.addView(infoCard("Tags", safe(n.tags).isEmpty() ? "-" : n.tags));
        Button speak = primaryButton("Bacakan Catatan"); speak.setOnClickListener(v -> { if (textToSpeech != null) textToSpeech.speak(n.content, TextToSpeech.QUEUE_FLUSH, null, n.id); });
        body.addView(speak, new LinearLayout.LayoutParams(-1, dp(56)));
        Button stop = darkButton("Berhenti Membaca"); stop.setOnClickListener(v -> { if (textToSpeech != null) textToSpeech.stop(); });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(56)); sp.setMargins(0, dp(10), 0, 0); body.addView(stop, sp);
        Button del = dangerButton("Hapus Catatan"); del.setOnClickListener(v -> confirm("Hapus catatan?", "Catatan akan dihapus permanen.", () -> deleteNote(n.id)));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, dp(56)); dp.setMargins(0, dp(10), 0, 0); body.addView(del, dp);
        setScreen(scroll(body));
    }

    private void deleteNote(String id) {
        List<NoteModel> notes = store.getNotes();
        for (int i = notes.size() - 1; i >= 0; i--) if (notes.get(i).id.equals(id)) notes.remove(i);
        store.saveNotes(notes); Snackbar.make(content, "Catatan dihapus", Snackbar.LENGTH_SHORT).show(); showNotes();
    }

    // ===================== MEDIA =====================
    private void showMedia() {
        currentRoot = "media";
        selectNav(R.id.nav_media);
        LinearLayout body = page();
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Galeri Media", 22, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = primaryButton("+ Tambah"); add.setOnClickListener(v -> showMediaForm()); header.addView(add, new LinearLayout.LayoutParams(dp(110), dp(52)));
        body.addView(header); addSpace(body, 14);
        EditText search = input("Cari media..."); search.setText(mediaSearch); search.setSingleLine(true);
        search.setOnEditorActionListener((v, actionId, event) -> { mediaSearch = v.getText().toString(); hideKeyboard(v); showMedia(); return true; });
        search.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) { mediaSearch = ((EditText)v).getText().toString(); showMedia(); } });
        body.addView(search); addSpace(body, 60);
        List<MediaModel> media = filterMedia(store.getMedia());
        if (media.isEmpty()) body.addView(emptyState("▧", "Belum ada media", "Tekan tombol + untuk menambah foto, audio, atau video."));
        else {
            RecyclerView rv = new RecyclerView(this); rv.setLayoutManager(new LinearLayoutManager(this)); rv.setNestedScrollingEnabled(false);
            rv.setAdapter(new MediaAdapter(this, media, this::showMediaDetail)); body.addView(rv);
        }
        setScreen(scroll(body));
    }

    private List<MediaModel> filterMedia(List<MediaModel> all) {
        if (mediaSearch == null || mediaSearch.trim().isEmpty()) return all;
        String q = mediaSearch.toLowerCase(Locale.US);
        List<MediaModel> out = new ArrayList<>();
        for (MediaModel m : all) if (safe(m.title).toLowerCase(Locale.US).contains(q) || safe(m.courseName).toLowerCase(Locale.US).contains(q)) out.add(m);
        return out;
    }

    private void showMediaForm() {
        pendingMediaUri = null; pendingMediaType = "Image";
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false); back.setOnClickListener(v -> showMedia()); body.addView(back); addSpace(body, 20);
        body.addView(text("Tambah Media", 22, WHITE, true)); addSpace(body, 20);
        EditText title = input("Judul media"); EditText course = input("Nama mata kuliah"); EditText desc = input("Deskripsi singkat");
        addField(body, "Judul *", title); addField(body, "Mata Kuliah", course); addField(body, "Deskripsi", desc);
        pendingMediaLabel = text("Belum ada file dipilih", 13, MUTED, false);
        Button choose = darkButton("Choose File"); choose.setOnClickListener(v -> pickMediaFile());
        LinearLayout.LayoutParams chooseLp = new LinearLayout.LayoutParams(dp(138), dp(54));
        chooseLp.setMargins(0, 0, dp(14), 0);
        body.addView(text("File", 14, WHITE, true));
        LinearLayout fileRow = row();
        fileRow.setGravity(Gravity.CENTER_VERTICAL);
        fileRow.addView(choose, chooseLp);
        pendingMediaLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        fileRow.addView(pendingMediaLabel);
        body.addView(fileRow);
        addSpace(body, 18);
        Button upload = primaryButton("Upload Media");
        upload.setOnClickListener(v -> {
            if (title.getText().toString().trim().isEmpty() || pendingMediaUri == null) { Toast.makeText(this, "Judul dan file wajib diisi.", Toast.LENGTH_SHORT).show(); return; }
            MediaModel m = new MediaModel(); m.id = IdHelper.newId(); m.title = title.getText().toString().trim(); m.courseName = course.getText().toString().trim(); m.description = desc.getText().toString().trim(); m.mediaType = pendingMediaType; m.fileUri = pendingMediaUri.toString(); m.createdAt = DateHelper.now();
            List<MediaModel> list = store.getMedia(); list.add(m); store.saveMedia(list); Toast.makeText(this, "Media berhasil ditambahkan", Toast.LENGTH_SHORT).show(); showMedia();
        });
        body.addView(upload, new LinearLayout.LayoutParams(-1, dp(56)));
        setScreen(scroll(body));
    }

    private void pickMediaFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "audio/*", "video/*"});
        startActivityForResult(intent, REQ_PICK_MEDIA);
    }

    private void showMediaDetail(MediaModel m) {
        LinearLayout body = page();
        TextView back = text("← Kembali", 14, MUTED, false); back.setOnClickListener(v -> { stopMedia(); showMedia(); }); body.addView(back); addSpace(body, 20);
        body.addView(text(m.title, 23, WHITE, true)); body.addView(text(safe(m.courseName) + " · " + safe(m.mediaType), 13, MUTED, false)); addSpace(body, 18);
        Uri uri = Uri.parse(m.fileUri);
        if ("Image".equals(m.mediaType)) {
            ImageView img = new ImageView(this); img.setImageURI(uri); img.setAdjustViewBounds(true); img.setScaleType(ImageView.ScaleType.FIT_CENTER); body.addView(img, new LinearLayout.LayoutParams(-1, dp(360)));
        } else if ("Video".equals(m.mediaType)) {
            VideoView vv = new VideoView(this); vv.setVideoURI(uri); vv.setMediaController(new MediaController(this)); body.addView(vv, new LinearLayout.LayoutParams(-1, dp(300))); vv.start();
        } else {
            body.addView(emptyState("♪", "Audio Materi", "Tekan tombol putar untuk mendengarkan file audio."));
            Button play = primaryButton("Putar Audio"); play.setOnClickListener(v -> playAudio(uri)); body.addView(play, new LinearLayout.LayoutParams(-1, dp(56)));
            Button stop = darkButton("Berhenti"); stop.setOnClickListener(v -> stopMedia()); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56)); lp.setMargins(0, dp(10), 0, 0); body.addView(stop, lp);
        }
        body.addView(infoCard("Deskripsi", safe(m.description).isEmpty() ? "Tidak ada deskripsi." : m.description));
        Button del = dangerButton("Hapus Media"); del.setOnClickListener(v -> confirm("Hapus media?", "Data media akan dihapus dari daftar lokal.", () -> deleteMedia(m.id))); LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, dp(56)); dl.setMargins(0, dp(12), 0, 0); body.addView(del, dl);
        setScreen(scroll(body));
    }

    private void playAudio(Uri uri) {
        try {
            stopMedia(); mediaPlayer = MediaPlayer.create(this, uri); mediaPlayer.start();
        } catch (Exception e) { Toast.makeText(this, "Audio gagal diputar.", Toast.LENGTH_SHORT).show(); }
    }

    private void stopMedia() {
        if (mediaPlayer != null) { try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {} mediaPlayer = null; }
    }

    private void deleteMedia(String id) {
        List<MediaModel> list = store.getMedia(); for (int i = list.size()-1; i>=0; i--) if (list.get(i).id.equals(id)) list.remove(i);
        store.saveMedia(list); showMedia();
    }

    // ===================== FOCUS =====================
    private void showFocus() {
        currentRoot = "focus";
        selectNav(R.id.nav_focus);
        LinearLayout body = page();
        body.addView(text("Mode Fokus", 22, WHITE, true)); addSpace(body, 18);
        LinearLayout presets = row();
        presets.addView(presetButton("Fokus 25m", 25, "Fokus"), new LinearLayout.LayoutParams(0, dp(52), 1));
        presets.addView(presetButton("Istirahat 5m", 5, "Istirahat"), new LinearLayout.LayoutParams(0, dp(52), 1));
        presets.addView(presetButton("Fokus 50m", 50, "Fokus"), new LinearLayout.LayoutParams(0, dp(52), 1));
        body.addView(presets); addSpace(body, 45);

        LinearLayout circle = new LinearLayout(this); circle.setGravity(Gravity.CENTER); circle.setOrientation(LinearLayout.VERTICAL); circle.setBackgroundColor(BG); circle.setPadding(0, dp(50), 0, dp(50));
        timerText = text(formatTime(secondsLeft), 38, WHITE, true); timerText.setGravity(Gravity.CENTER); circle.addView(timerText);
        timerSubText = text(timerType, 13, MUTED, false); timerSubText.setGravity(Gravity.CENTER); circle.addView(timerSubText);
        body.addView(circle, new LinearLayout.LayoutParams(-1, dp(230)));

        LinearLayout controls = row(); controls.setGravity(Gravity.CENTER);
        Button reset = darkButton("↻"); reset.setOnClickListener(v -> resetTimer()); controls.addView(reset, new LinearLayout.LayoutParams(dp(64), dp(60)));
        playButton = primaryButton(timerRunning ? "Pause" : "▷"); playButton.setOnClickListener(v -> toggleTimer()); LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(78), dp(64)); pp.setMargins(dp(16), 0, 0, 0); controls.addView(playButton, pp);
        body.addView(controls); addSpace(body, 30);

        int todayMinutes = 0; for (FocusSessionModel s : store.getFocusSessions()) if (s.completed) todayMinutes += s.focusMinutes;
        LinearLayout stat = row(); stat.setGravity(Gravity.CENTER_VERTICAL); stat.addView(text("⏱  Fokus Hari Ini", 16, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1)); stat.addView(text(todayMinutes + " menit", 20, WHITE, true)); body.addView(stat);
        addSpace(body, 28);
        body.addView(text("↺  Riwayat Sesi", 16, WHITE, true));
        RecyclerView rv = new RecyclerView(this); rv.setLayoutManager(new LinearLayoutManager(this)); rv.setNestedScrollingEnabled(false); rv.setAdapter(new FocusHistoryAdapter(this, store.getFocusSessions())); body.addView(rv);
        setScreen(scroll(body));
        updateTimerLabels();
    }

    private Button presetButton(String label, int minutes, String type) {
        Button b = "".equals(label) ? darkButton(label) : darkButton(label);
        boolean selected = selectedMinutes == minutes && timerType.equals(type);
        if (selected) { b.setBackgroundColor(Color.WHITE); b.setTextColor(Color.BLACK); }
        b.setOnClickListener(v -> { if (!timerRunning) { selectedMinutes = minutes; secondsLeft = minutes * 60; timerType = type; showFocus(); } });
        return b;
    }

    private void toggleTimer() {
        if (timerRunning) {
            timerRunning = false; unregisterMotion(); updateTimerLabels();
        } else {
            timerRunning = true; if (focusStartedAt == 0) focusStartedAt = System.currentTimeMillis(); registerMotion(); timerHandler.post(timerRunnable); updateTimerLabels();
        }
    }

    private void resetTimer() {
        timerRunning = false; secondsLeft = selectedMinutes * 60; movementWarnings = 0; focusStartedAt = 0; unregisterMotion(); updateTimerLabels();
    }

    private void updateTimerLabels() {
        if (timerText != null) timerText.setText(formatTime(secondsLeft));
        if (timerSubText != null) timerSubText.setText(timerType);
        if (playButton != null) playButton.setText(timerRunning ? "Pause" : "▷");
    }

    private String formatTime(int sec) {
        int m = sec / 60, s = sec % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    private void completeFocusSession() {
        timerRunning = false; unregisterMotion();
        FocusSessionModel fs = new FocusSessionModel(); fs.id = IdHelper.newId(); fs.focusMinutes = "Istirahat".equals(timerType) ? 0 : selectedMinutes; fs.breakMinutes = "Istirahat".equals(timerType) ? selectedMinutes : 0; fs.startedAt = DateHelper.now(); fs.endedAt = DateHelper.now(); fs.completed = true; fs.movementWarningCount = movementWarnings;
        List<FocusSessionModel> sessions = store.getFocusSessions(); sessions.add(0, fs); store.saveFocusSessions(sessions);
        NotificationHelper.show(this, "Sesi selesai", timerType + " " + selectedMinutes + " menit selesai.", 88);
        Toast.makeText(this, "Sesi selesai", Toast.LENGTH_SHORT).show();
        secondsLeft = selectedMinutes * 60; movementWarnings = 0; focusStartedAt = 0; showFocus();
    }

    private void registerMotion() { if (sensorManager != null && accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL); }
    private void unregisterMotion() { if (sensorManager != null) sensorManager.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!timerRunning || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float accel = Math.abs(x) + Math.abs(y) + Math.abs(z);
        float delta = Math.abs(accel - lastAccel);
        lastAccel = accel;
        long now = System.currentTimeMillis();
        if (delta > 18 && now - lastWarningAt > 4000) {
            movementWarnings++; lastWarningAt = now;
            Snackbar.make(content, "Perangkat sering bergerak. Tetap fokus.", Snackbar.LENGTH_SHORT).show();
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ===================== PROFILE =====================
    private void showProfile() {
        currentRoot = "profile";
        selectNav(R.id.nav_profile);
        UserProfileModel profile = store.getProfile();
        if (profile == null) profile = new UserProfileModel("Riyadhu Jawhar", "Universitas Teknologi", "Teknik Informatika", 5, "IPK 3.7 semester ini", 4);
        List<TaskModel> tasks = store.getTasks(); List<NoteModel> notes = store.getNotes(); List<FocusSessionModel> sessions = store.getFocusSessions(); List<MediaModel> media = store.getMedia();
        int done = 0, focus = 0; for (TaskModel t : tasks) if (t.isDone()) done++; for (FocusSessionModel s : sessions) focus += s.focusMinutes;
        int completion = tasks.isEmpty() ? 0 : Math.round(done * 100f / tasks.size());

        LinearLayout body = page();
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(0, dp(16), 0, dp(18));
        TextView avatar = text("♙", 42, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundedBg(TEAL, TEAL, 24));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(78), dp(78)));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(14), 0, 0, 0); info.addView(text(profile.studentName, 20, WHITE, true)); info.addView(text(profile.university, 14, MUTED, false)); info.addView(text(profile.major + "   Semester " + profile.semester, 12, WHITE, true)); header.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        Button edit = darkButton("✎"); UserProfileModel finalProfile = profile; edit.setOnClickListener(v -> showEditProfileDialog(finalProfile)); header.addView(edit, new LinearLayout.LayoutParams(dp(60), dp(54)));
        body.addView(header);
        body.addView(text("◎  " + profile.studyGoal, 14, MUTED, false)); addSpace(body, 35);
        body.addView(text("♜  Statistik Belajar", 16, WHITE, true)); addSpace(body, 20);
        LinearLayout pr = row(); pr.addView(text("Penyelesaian Tugas", 15, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1)); pr.addView(text(completion + "%", 18, WHITE, true)); body.addView(pr);
        body.addView(progressBar(completion));
        LinearLayout progressMeta = row();
        progressMeta.addView(text(done + " selesai", 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView activeText = text((tasks.size() - done) + " aktif", 12, MUTED, false);
        activeText.setGravity(Gravity.END);
        progressMeta.addView(activeText, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(progressMeta);
        addSpace(body, 30);
        LinearLayout g1 = row(); g1.addView(profileStat("☑", String.valueOf(tasks.size()), "Total Tugas"), new LinearLayout.LayoutParams(0, -2, 1)); g1.addView(profileStat("□", String.valueOf(notes.size()), "Total Catatan"), new LinearLayout.LayoutParams(0, -2, 1)); body.addView(g1); addSpace(body, 24);
        LinearLayout g2 = row(); g2.addView(profileStat("⏱", String.valueOf(focus), "Menit Fokus"), new LinearLayout.LayoutParams(0, -2, 1)); g2.addView(profileStat("↗", String.valueOf(media.size()), "Total Media"), new LinearLayout.LayoutParams(0, -2, 1)); body.addView(g2);
        addSpace(body, 36); body.addView(text("MATA KULIAH TERBANYAK", 12, MUTED, true));
        for (View line : topCourseViews(tasks)) body.addView(line);
        addSpace(body, 28);
        Button themeToggle = darkButton(darkMode ? "☀  Ubah ke Light Mode" : "🌙  Ubah ke Dark Mode");
        themeToggle.setOnClickListener(v -> {
            darkMode = !darkMode;
            getSharedPreferences("ui_prefs", MODE_PRIVATE).edit().putBoolean("dark_mode", darkMode).apply();
            applyThemePalette();
            Toast.makeText(this, darkMode ? "Dark mode aktif" : "Light mode aktif", Toast.LENGTH_SHORT).show();
            buildRoot();
            showProfile();
        });
        body.addView(themeToggle, new LinearLayout.LayoutParams(-1, dp(56)));
        addSpace(body, 14);
        Button reset = dangerButton("Reset Semua Data"); reset.setOnClickListener(v -> confirm("Reset semua data?", "Tugas, catatan, media, dan riwayat fokus akan dihapus.", () -> { store.resetActivityData(); showProfile(); })); body.addView(reset, new LinearLayout.LayoutParams(-1, dp(56)));
        Button delete = dangerButton("🗑  Hapus Akun"); LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(-1, dp(56)); delLp.setMargins(0, dp(14), 0, 0); delete.setOnClickListener(v -> confirm("Hapus akun?", "Semua data lokal akan dihapus dan sample data akan dimuat ulang saat app dibuka ulang.", () -> { store.deleteAccount(); store.seedIfNeeded(); showDashboard(); })); body.addView(delete, delLp);
        addSpace(body, 20); TextView footer = text("Dibuat oleh Riyadhu Jawhar", 12, SUBTLE, false); footer.setGravity(Gravity.CENTER); body.addView(footer);
        setScreen(scroll(body));
    }

    private View profileStat(String icon, String value, String label) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(8), dp(10), dp(8), dp(10)); box.setBackground(roundedBg(Color.TRANSPARENT, Color.TRANSPARENT, 18)); applyPressAnimation(box);
        box.addView(text(icon, 20, TEAL, true)); box.addView(text(value, 21, WHITE, true)); box.addView(text(label, 11, MUTED, false)); return box;
    }

    private List<View> topCourseViews(List<TaskModel> tasks) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (TaskModel t : tasks) {
            String key = safe(t.courseName).isEmpty() ? "Tanpa Mata Kuliah" : safe(t.courseName);
            map.put(key, map.getOrDefault(key, 0) + 1);
        }
        List<View> rows = new ArrayList<>();
        if (map.isEmpty()) {
            rows.add(text("Belum ada data mata kuliah.", 15, WHITE, false));
            return rows;
        }
        for (String k : map.keySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            TextView left = text(k, 15, WHITE, true);
            TextView right = text(map.get(k) + " tugas", 15, WHITE, false);
            right.setGravity(Gravity.END);
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(right, new LinearLayout.LayoutParams(-2, -2));
            rows.add(row);
        }
        return rows;
    }

    private void showEditProfileDialog(UserProfileModel p) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(16), dp(22), dp(16)); box.setBackgroundColor(BG);
        EditText name = input("Nama"); name.setText(p.studentName);
        EditText uni = input("Kampus"); uni.setText(p.university);
        EditText major = input("Program Studi"); major.setText(p.major);
        EditText sem = input("Semester"); sem.setText(String.valueOf(p.semester));
        EditText hours = input("Jam Belajar/Hari"); hours.setText(String.valueOf(p.dailyStudyHours));
        EditText goal = input("Target Belajar"); goal.setText(p.studyGoal);
        addField(box, "Nama", name); addField(box, "Kampus", uni); addField(box, "Program Studi", major); addField(box, "Semester", sem); addField(box, "Jam Belajar/Hari", hours); addField(box, "Target Belajar", goal);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Edit Profil").setView(box).setPositiveButton("Simpan", null).setNegativeButton("Batal", null).create();
        d.setOnShowListener(dialog -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty() || major.getText().toString().trim().isEmpty()) { Toast.makeText(this, "Nama dan program studi wajib diisi.", Toast.LENGTH_SHORT).show(); return; }
            p.studentName = name.getText().toString().trim(); p.university = uni.getText().toString().trim(); p.major = major.getText().toString().trim(); p.studyGoal = goal.getText().toString().trim();
            try { p.semester = Integer.parseInt(sem.getText().toString().trim()); } catch (Exception ignored) { p.semester = 1; }
            try { p.dailyStudyHours = Integer.parseInt(hours.getText().toString().trim()); } catch (Exception ignored) { p.dailyStudyHours = 1; }
            store.saveProfile(p); d.dismiss(); showProfile();
        }));
        d.show();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(roundedBg(BG, CARD_STROKE, 20));
    }

    // ===================== ACTIVITY RESULT =====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_PICK_MEDIA) {
            pendingMediaUri = data.getData();
            if (pendingMediaUri != null) {
                try { getContentResolver().takePersistableUriPermission(pendingMediaUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                String type = getContentResolver().getType(pendingMediaUri);
                if (type != null && type.startsWith("audio")) pendingMediaType = "Audio";
                else if (type != null && type.startsWith("video")) pendingMediaType = "Video";
                else pendingMediaType = "Image";
                if (pendingMediaLabel != null) pendingMediaLabel.setText("File dipilih: " + pendingMediaType);
            }
        } else if (requestCode == REQ_SPEECH) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                EditText contentBox = findNoteContentEditText();
                if (contentBox != null) {
                    String old = contentBox.getText().toString();
                    contentBox.setText(old + (old.isEmpty() ? "" : " ") + results.get(0));
                    contentBox.setSelection(contentBox.length());
                }
            }
        }
    }

    // ===================== UI HELPERS =====================
    private void applyThemePalette() {
        if (darkMode) {
            BG = Color.parseColor("#050505");
            CARD = Color.parseColor("#1F1F1F");
            CARD_STROKE = Color.parseColor("#333333");
            WHITE = Color.parseColor("#F8FAFC");
            MUTED = Color.parseColor("#B8C0CC");
            SUBTLE = Color.parseColor("#8A8A8A");
            NAV_BG = Color.parseColor("#FFFFFF");
            NAV_SELECTED = Color.parseColor("#222222");
            INPUT_BG = Color.parseColor("#080808");
        } else {
            BG = Color.parseColor("#F4F7FA");
            CARD = Color.parseColor("#FFFFFF");
            CARD_STROKE = Color.parseColor("#E4E7EC");
            WHITE = Color.parseColor("#111827");
            MUTED = Color.parseColor("#4B5563");
            SUBTLE = Color.parseColor("#6B7280");
            NAV_BG = Color.parseColor("#FFFFFF");
            NAV_SELECTED = Color.parseColor("#E7F8F8");
            INPUT_BG = Color.parseColor("#FFFFFF");
        }
    }

    private GradientDrawable roundedBg(int fill, int stroke, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill);
        gd.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) gd.setStroke(dp(1), stroke);
        return gd;
    }

    private GradientDrawable roundedTopBg(int fill, int stroke, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill);
        gd.setStroke(dp(1), stroke);
        float r = dp(radiusDp);
        gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return gd;
    }

    private android.graphics.drawable.Drawable rippleBg(int fill, int stroke, int radiusDp) {
        GradientDrawable base = roundedBg(fill, stroke, radiusDp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int rippleColor = darkMode ? Color.parseColor("#33FFFFFF") : Color.parseColor("#22000000");
            return new RippleDrawable(ColorStateList.valueOf(rippleColor), base, null);
        }
        return base;
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.92f).setDuration(70).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
            }
            return false;
        });
    }

    private LinearLayout page() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(28), statusBarTop() + dp(10), dp(28), dp(30));
        body.setBackgroundColor(BG);
        return body;
    }


    private int statusBarTop() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) return getResources().getDimensionPixelSize(id);
        return dp(18);
    }

    private EditText readOnlyPickerField(String hint) {
        EditText e = input(hint);
        e.setFocusable(false);
        e.setClickable(true);
        e.setCursorVisible(false);
        e.setLongClickable(false);
        return e;
    }

    private void attachDatePicker(EditText target) {
        View.OnClickListener open = v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        };
        target.setOnClickListener(open);
        target.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) open.onClick(v); });
    }

    private void attachTimePicker(EditText target) {
        View.OnClickListener open = v -> {
            Calendar c = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                target.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
            dialog.show();
        };
        target.setOnClickListener(open);
        target.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) open.onClick(v); });
    }

    private ScrollView scroll(View child) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(false);
        sv.setBackgroundColor(BG);
        sv.addView(child, new ScrollView.LayoutParams(-1, -2));
        return sv;
    }

    private View horizontalScroll(View child) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(child);
        return hsv;
    }

    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView tv = new TextView(this); tv.setText(s == null ? "" : s); tv.setTextSize(sp); tv.setTextColor(color); if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD); tv.setIncludeFontPadding(true); return tv;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(SUBTLE);
        e.setTextColor(WHITE);
        e.setTextSize(15);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setSingleLine(false);
        e.setBackground(rippleBg(INPUT_BG, CARD_STROKE, 14));
        e.setMinHeight(dp(54));
        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(WHITE);
                tv.setTextSize(15);
                tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setPadding(dp(14), 0, dp(14), 0);
                tv.setBackgroundColor(INPUT_BG);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(WHITE);
                tv.setTextSize(15);
                tv.setPadding(dp(16), dp(14), dp(16), dp(14));
                tv.setBackgroundColor(CARD);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setBackground(rippleBg(INPUT_BG, CARD_STROKE, 14));
        sp.setPadding(0, 0, 0, 0);
        return sp;
    }

    private void setSpinner(Spinner spinner, String value) { if (value == null) return; for (int i = 0; i < spinner.getCount(); i++) if (value.equals(spinner.getItemAtPosition(i).toString())) { spinner.setSelection(i); return; } }

    private void addField(LinearLayout body, String label, View field) {
        TextView l = text(label, 14, WHITE, true);
        l.setPadding(0, dp(10), 0, dp(4));
        body.addView(l);
        int height = dp(54);
        if (field instanceof EditText) {
            EditText et = (EditText) field;
            if (et.getMinLines() >= 3) height = dp(110);
            if (et.getMinLines() >= 8) height = dp(180);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, height);
        body.addView(field, params);
    }

    private Button primaryButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.BLACK);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rippleBg(TEAL, TEAL, 16));
        applyPressAnimation(b);
        return b;
    }

    private Button darkButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rippleBg(CARD, CARD_STROKE, 16));
        applyPressAnimation(b);
        return b;
    }

    private Button dangerButton(String s) {
        Button b = darkButton(s);
        b.setTextColor(Color.parseColor("#DC2626"));
        b.setBackground(rippleBg(BG, Color.parseColor("#552626"), 16));
        return b;
    }

    private Button chip(String s, boolean selected) {
        Button b = selected ? primaryButton(s) : darkButton(s);
        if (selected) {
            b.setTextColor(darkMode ? Color.BLACK : Color.parseColor("#064E4E"));
            b.setBackground(rippleBg(darkMode ? Color.WHITE : Color.parseColor("#DDF8F8"), CARD_STROKE, 18));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(46));
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private View emptyState(String icon, String title, String desc) {
        LinearLayout e = new LinearLayout(this);
        e.setOrientation(LinearLayout.VERTICAL);
        e.setGravity(Gravity.CENTER);
        e.setPadding(0, dp(70), 0, dp(70));
        TextView ic = text(icon, 40, WHITE, true);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(roundedBg(CARD, CARD_STROKE, 18));
        ic.setPadding(dp(16), dp(10), dp(16), dp(10));
        e.addView(ic);
        TextView t = text(title, 19, WHITE, true); t.setGravity(Gravity.CENTER); t.setPadding(0, dp(14), 0, 0); e.addView(t);
        TextView d = text(desc, 14, MUTED, false); d.setGravity(Gravity.CENTER); e.addView(d);
        return e;
    }

    private View infoCard(String title, String value) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(rippleBg(CARD, CARD_STROKE, 18));
        applyPressAnimation(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, dp(8));
        c.setLayoutParams(lp);
        c.addView(text(title, 12, MUTED, true));
        TextView v = text(value, 15, WHITE, false);
        v.setPadding(0, dp(4), 0, 0);
        c.addView(v);
        return c;
    }

    private View progressBar(int percent) {
        LinearLayout outer = new LinearLayout(this);
        outer.setBackground(roundedBg(darkMode ? Color.parseColor("#3B3B3B") : Color.parseColor("#E5E7EB"), Color.TRANSPARENT, 8));
        outer.setPadding(0, 0, 0, 0);
        LinearLayout inner = new LinearLayout(this);
        inner.setBackground(roundedBg(TEAL, TEAL, 8));
        outer.addView(inner, new LinearLayout.LayoutParams(Math.max(dp(1), dp(300) * percent / 100), dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(8));
        lp.setMargins(0, dp(8), 0, dp(6));
        outer.setLayoutParams(lp);
        return outer;
    }

    private void addSpace(LinearLayout body, int dp) { SpaceView sp = new SpaceView(this); body.addView(sp, new LinearLayout.LayoutParams(1, dp(dp))); }

    public static class SpaceView extends View { public SpaceView(Context c) { super(c); } }

    private void confirm(String title, String msg, Runnable ok) { new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("Ya", (d, w) -> ok.run()).setNegativeButton("Batal", null).show(); }

    private void hideKeyboard(View v) { try { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0); v.clearFocus(); } catch (Exception ignored) {} }

    private String safe(String s) { return s == null ? "" : s; }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { super.onDestroy(); timerRunning = false; timerHandler.removeCallbacksAndMessages(null); unregisterMotion(); stopMedia(); if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); } }
}
