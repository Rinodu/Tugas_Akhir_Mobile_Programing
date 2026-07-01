package com.campusmate.pro.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmate.pro.model.FocusSessionModel;

import java.util.List;

public class FocusHistoryAdapter extends RecyclerView.Adapter<FocusHistoryAdapter.Holder> {
    private final Context context;
    private final List<FocusSessionModel> items;
    private final boolean dark;

    public FocusHistoryAdapter(Context context, List<FocusSessionModel> items) {
        this.context = context; this.items = items;
        this.dark = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE).getBoolean("dark_mode", true);
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(round(dark ? "#111111" : "#FFFFFF", dark ? "#242424" : "#E4E7EC", 16));
        press(row);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(5), dp(16), dp(5));
        row.setLayoutParams(lp);

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = new TextView(context);
        title.setTextColor(dark ? Color.WHITE : Color.parseColor("#111827"));
        title.setTypeface(null, 1);
        title.setTextSize(14);
        body.addView(title);

        TextView date = new TextView(context);
        date.setTextColor(dark ? Color.parseColor("#B8C0CC") : Color.parseColor("#6B7280"));
        date.setTextSize(12);
        body.addView(date);

        TextView badge = new TextView(context);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        badge.setTextSize(12);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.parseColor("#059669"));
        badge.setBackground(round("#D1FAE5", "#D1FAE5", 18));
        row.addView(badge);
        return new Holder(row, title, date, badge);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        FocusSessionModel s = items.get(position);
        h.title.setText(s.focusMinutes + " menit fokus");
        h.date.setText(s.startedAt == null ? "" : s.startedAt);
        h.badge.setText(s.movementWarningCount > 0 ? "Selesai  ⚠ " + s.movementWarningCount : "Selesai");
    }

    @Override public int getItemCount() { return items.size(); }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView title, date, badge;
        public Holder(@NonNull View itemView, TextView title, TextView date, TextView badge) {
            super(itemView); this.title = title; this.date = date; this.badge = badge;
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
