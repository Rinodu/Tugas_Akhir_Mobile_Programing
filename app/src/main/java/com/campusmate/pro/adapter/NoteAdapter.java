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

import com.campusmate.pro.model.NoteModel;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.Holder> {
    public interface Callback { void onOpen(NoteModel note); }
    private final Context context;
    private final List<NoteModel> items;
    private final Callback callback;
    private final boolean dark;

    public NoteAdapter(Context context, List<NoteModel> items, Callback callback) {
        this.context = context;
        this.items = items;
        this.callback = callback;
        this.dark = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE).getBoolean("dark_mode", true);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(round(dark ? "#1F1F1F" : "#FFFFFF", dark ? "#343434" : "#E4E7EC", 18));
        press(card);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(7), dp(16), dp(7));
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);

        TextView title = new TextView(context);
        title.setTextColor(dark ? Color.WHITE : Color.parseColor("#111827"));
        title.setTypeface(null, 1);
        title.setTextSize(15);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView chevron = new TextView(context);
        chevron.setText("›");
        chevron.setTextSize(24);
        chevron.setTextColor(dark ? Color.parseColor("#9CA3AF") : Color.parseColor("#9AA0A6"));
        row.addView(chevron);

        TextView course = new TextView(context);
        course.setTextColor(dark ? Color.parseColor("#9CA3AF") : Color.parseColor("#6B7280"));
        course.setTextSize(12);
        card.addView(course);

        TextView content = new TextView(context);
        content.setTextColor(dark ? Color.parseColor("#B8C0CC") : Color.parseColor("#4B5563"));
        content.setTextSize(13);
        content.setMaxLines(2);
        card.addView(content);

        TextView date = new TextView(context);
        date.setTextColor(dark ? Color.parseColor("#8A8A8A") : Color.parseColor("#6B7280"));
        date.setTextSize(11);
        date.setPadding(0, dp(8), 0, 0);
        card.addView(date);
        return new Holder(card, title, course, content, date);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        NoteModel n = items.get(position);
        h.title.setText((n.isPinned ? "📌 " : "") + n.title);
        h.course.setText(n.courseName == null ? "" : n.courseName);
        h.content.setText(n.content == null ? "" : n.content);
        h.date.setText(n.updatedAt == null ? "" : n.updatedAt);
        h.itemView.setOnClickListener(v -> callback.onOpen(n));
    }

    @Override public int getItemCount() { return items.size(); }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView title, course, content, date;
        public Holder(@NonNull View itemView, TextView title, TextView course, TextView content, TextView date) {
            super(itemView);
            this.title = title; this.course = course; this.content = content; this.date = date;
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
