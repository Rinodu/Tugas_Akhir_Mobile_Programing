package com.campusmate.pro.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmate.pro.model.MediaModel;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.Holder> {
    public interface Callback { void onOpen(MediaModel media); }
    private final Context context;
    private final List<MediaModel> items;
    private final Callback callback;
    private final boolean dark;

    public MediaAdapter(Context context, List<MediaModel> items, Callback callback) {
        this.context = context; this.items = items; this.callback = callback;
        this.dark = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE).getBoolean("dark_mode", true);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(round(dark ? "#1F1F1F" : "#FFFFFF", dark ? "#343434" : "#E4E7EC", 18));
        press(card);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.setMargins(dp(12), dp(8), dp(12), dp(8));
        card.setLayoutParams(lp);

        ImageView image = new ImageView(context);
        image.setBackground(round(dark ? "#242424" : "#E7F0F0", dark ? "#343434" : "#E4E7EC", 16));
        image.setClipToOutline(false);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(150)));

        TextView title = new TextView(context);
        title.setTextColor(dark ? Color.WHITE : Color.parseColor("#111827"));
        title.setTypeface(null, 1);
        title.setTextSize(15);
        title.setPadding(0, dp(9), 0, 0);
        card.addView(title);

        TextView meta = new TextView(context);
        meta.setTextColor(dark ? Color.parseColor("#B8C0CC") : Color.parseColor("#6B7280"));
        meta.setTextSize(12);
        card.addView(meta);
        return new Holder(card, image, title, meta);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        MediaModel m = items.get(position);
        h.title.setText(m.title);
        h.meta.setText((m.courseName == null ? "" : m.courseName) + " · " + m.mediaType);
        if ("Image".equals(m.mediaType)) {
            h.image.setImageURI(Uri.parse(m.fileUri));
        } else {
            h.image.setImageDrawable(null);
            h.image.setBackground(round(dark ? "#242424" : "#E7F0F0", dark ? "#343434" : "#E4E7EC", 16));
            h.image.setContentDescription(m.mediaType);
        }
        h.itemView.setOnClickListener(v -> callback.onOpen(m));
    }

    @Override public int getItemCount() { return items.size(); }

    public static class Holder extends RecyclerView.ViewHolder {
        ImageView image; TextView title, meta;
        public Holder(@NonNull View itemView, ImageView image, TextView title, TextView meta) {
            super(itemView); this.image = image; this.title = title; this.meta = meta;
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
