package com.campusmate.pro.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmate.pro.model.TaskModel;
import com.campusmate.pro.util.DateHelper;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.Holder> {
    public interface Callback {
        void onToggle(TaskModel task);
        void onOpen(TaskModel task);
    }

    private final Context context;
    private final List<TaskModel> items;
    private final Callback callback;
    private final boolean dark;

    public TaskAdapter(Context context, List<TaskModel> items, Callback callback) {
        this.context = context;
        this.items = items;
        this.callback = callback;
        this.dark = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE).getBoolean("dark_mode", true);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(14), dp(16));
        card.setBackground(round(dark ? "#1F1F1F" : "#FFFFFF", dark ? "#343434" : "#E4E7EC", 18));
        press(card);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(7), dp(16), dp(7));
        card.setLayoutParams(lp);

        TextView check = new TextView(context);
        check.setTextSize(24);
        check.setGravity(Gravity.CENTER);
        check.setTextColor(Color.parseColor("#10B981"));
        card.addView(check, new LinearLayout.LayoutParams(dp(44), dp(54)));

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(8), 0, dp(8), 0);
        card.addView(body, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = new TextView(context);
        title.setTextSize(15);
        title.setTextColor(dark ? Color.WHITE : Color.parseColor("#111827"));
        title.setTypeface(null, 1);
        body.addView(title);

        TextView meta = new TextView(context);
        meta.setTextSize(12);
        meta.setTextColor(dark ? Color.parseColor("#B8C0CC") : Color.parseColor("#6B7280"));
        body.addView(meta);

        TextView badge = new TextView(context);
        badge.setTextSize(12);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        card.addView(badge, new LinearLayout.LayoutParams(-2, -2));

        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setTextSize(28);
        chevron.setGravity(Gravity.CENTER);
        chevron.setTextColor(dark ? Color.parseColor("#9CA3AF") : Color.parseColor("#9AA0A6"));
        card.addView(chevron, new LinearLayout.LayoutParams(dp(34), dp(52)));

        return new Holder(card, check, title, meta, badge);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        TaskModel t = items.get(position);
        h.check.setText(t.isDone() ? "✓" : "○");
        h.title.setText(t.title);
        h.title.setPaintFlags(t.isDone() ? h.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : h.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        String deadline = t.deadlineDate == null ? "" : t.deadlineDate;
        String overdue = DateHelper.isOverdue(t.deadlineDate, t.deadlineTime) && !t.isDone() ? " · Terlambat" : "";
        h.meta.setText((t.courseName == null ? "" : t.courseName) + " · " + deadline + overdue);
        h.meta.setTextColor(overdue.isEmpty() ? (dark ? Color.parseColor("#B8C0CC") : Color.parseColor("#6B7280")) : Color.parseColor("#EF4444"));
        h.badge.setText(t.priority == null ? "Sedang" : t.priority);
        if ("Tinggi".equals(t.priority)) {
            h.badge.setTextColor(Color.parseColor("#DC2626"));
            h.badge.setBackground(round("#FEE2E2", "#FEE2E2", 18));
        } else if ("Rendah".equals(t.priority)) {
            h.badge.setTextColor(Color.parseColor("#059669"));
            h.badge.setBackground(round("#D1FAE5", "#D1FAE5", 18));
        } else {
            h.badge.setTextColor(Color.parseColor("#D97706"));
            h.badge.setBackground(round("#FEF3C7", "#FEF3C7", 18));
        }
        h.check.setOnClickListener(v -> callback.onToggle(t));
        h.itemView.setOnClickListener(v -> callback.onOpen(t));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView check, title, meta, badge;
        public Holder(@NonNull View itemView, TextView check, TextView title, TextView meta, TextView badge) {
            super(itemView);
            this.check = check;
            this.title = title;
            this.meta = meta;
            this.badge = badge;
        }
    }

    private GradientDrawable round(String fill, String stroke, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(fill));
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), Color.parseColor(stroke));
        return gd;
    }

    private void press(View view) {
        view.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.985f).scaleY(0.985f).alpha(0.94f).setDuration(70).start();
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
            return false;
        });
    }

    private int dp(int v) { return (int)(v * context.getResources().getDisplayMetrics().density); }
}
