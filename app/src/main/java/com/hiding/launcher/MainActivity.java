package com.hiding.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final List<AppInfo> installedApps = new ArrayList<>();
    private final Handler clockHandler = new Handler();
    private Runnable clockRunnable;
    private int currentOrientation = -1;

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadApps();
        currentOrientation = getResources().getConfiguration().orientation;
        applyLayout(currentOrientation);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != currentOrientation) {
            currentOrientation = newConfig.orientation;
            applyLayout(currentOrientation);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startClock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
    }

    @Override
    public void onBackPressed() {
        // Launchers swallow the back button
    }

    // ──────────────────────────────────────────────────────────────────────────
    // App loading
    // ──────────────────────────────────────────────────────────────────────────

    private void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
            installedApps.clear();
            for (ResolveInfo ri : list) {
                if (ri.activityInfo.packageName.equals(getPackageName())) continue;
                AppInfo info = new AppInfo();
                info.label       = ri.loadLabel(pm).toString();
                info.icon        = ri.loadIcon(pm);
                info.packageName = ri.activityInfo.packageName;
                info.activityName = ri.activityInfo.name;
                installedApps.add(info);
            }
            installedApps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        } catch (Exception e) {
            Toast.makeText(this, "Could not load apps: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Layout switching
    // ──────────────────────────────────────────────────────────────────────────

    private void applyLayout(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_landscape);
            bindWindowsUI();
        } else {
            setContentView(R.layout.activity_portrait);
            bindAndroidUI();
        }
        bindSlider();
        updateClocks();
    }

    // ── Portrait: Android home ─────────────────────────────────────────────────

    private void bindAndroidUI() {
        RecyclerView grid = findViewById(R.id.app_grid);
        if (grid != null) {
            grid.setLayoutManager(new GridLayoutManager(this, 4));
            grid.setAdapter(new AppGridAdapter(installedApps, false));
        }
    }

    // ── Landscape: Windows desktop ─────────────────────────────────────────────

    private void bindWindowsUI() {
        // Desktop icon grid
        RecyclerView desktop = findViewById(R.id.desktop_grid);
        if (desktop != null) {
            desktop.setLayoutManager(new GridLayoutManager(this, 6));
            desktop.setAdapter(new AppGridAdapter(installedApps, true));
        }

        // Start button scrolls desktop back to top
        View startBtn = findViewById(R.id.start_btn);
        if (startBtn != null && desktop != null) {
            startBtn.setOnClickListener(v -> desktop.scrollToPosition(0));
        }

        // ⇄ button opens the switch-launcher overlay
        View switchBtn    = findViewById(R.id.switch_btn);
        View switchOverlay = findViewById(R.id.switch_overlay);
        if (switchBtn != null && switchOverlay != null) {
            switchBtn.setOnClickListener(v -> {
                switchOverlay.setVisibility(View.VISIBLE);
                SeekBar s = switchOverlay.findViewById(R.id.switch_slider);
                if (s != null) s.setProgress(0);
                TextView lbl = switchOverlay.findViewById(R.id.slider_label);
                if (lbl != null) lbl.setText("Slide → to switch launcher");
            });
            // Tap outside the dialog panel to close overlay
            switchOverlay.setOnClickListener(v -> switchOverlay.setVisibility(View.GONE));
            // Dialog panel itself absorbs touches so outer click doesn't fire
            View dialogPanel = switchOverlay.findViewById(R.id.switch_dialog);
            if (dialogPanel != null) {
                dialogPanel.setOnClickListener(v -> { /* consume */ });
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Slide-to-switch launcher
    // ──────────────────────────────────────────────────────────────────────────

    private void bindSlider() {
        // Works for both portrait (slider at bottom) and
        // landscape (slider inside the switch overlay)
        SeekBar slider = findViewById(R.id.switch_slider);
        if (slider == null) return;

        slider.setProgress(0);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                TextView lbl = findViewById(R.id.slider_label);
                if (lbl != null) {
                    if (progress >= 80) {
                        lbl.setText("Release to switch! →");
                    } else {
                        lbl.setText("Slide → to switch launcher");
                    }
                }
            }

            @Override public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                if (s.getProgress() >= 90) {
                    // Hide overlay if visible (Windows mode)
                    View overlay = findViewById(R.id.switch_overlay);
                    if (overlay != null) overlay.setVisibility(View.GONE);
                    switchLauncher();
                } else {
                    s.setProgress(0);
                    TextView lbl = findViewById(R.id.slider_label);
                    if (lbl != null) lbl.setText("Slide → to switch launcher");
                }
            }
        });
    }

    private void switchLauncher() {
        // Try to open the Home App chooser page directly
        String[] actions = {
            "android.settings.HOME_SETTINGS",
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
        };
        for (String action : actions) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this,
            "Go to: Settings → Apps → Default Apps → Home App",
            Toast.LENGTH_LONG).show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Clock
    // ──────────────────────────────────────────────────────────────────────────

    private void updateClocks() {
        String time24    = new SimpleDateFormat("HH:mm",           Locale.getDefault()).format(new Date());
        String time12    = new SimpleDateFormat("h:mm a",          Locale.getDefault()).format(new Date());
        String dateLong  = new SimpleDateFormat("EEEE, MMMM d",    Locale.getDefault()).format(new Date());
        String dateShort = new SimpleDateFormat("EEE  M/d",        Locale.getDefault()).format(new Date());

        setTxt(R.id.main_clock,    time24);
        setTxt(R.id.main_date,     dateLong);
        setTxt(R.id.status_time,   time24);

        TextView taskbarClock = findViewById(R.id.taskbar_clock);
        if (taskbarClock != null) taskbarClock.setText(time12 + "\n" + dateShort);
    }

    private void setTxt(int id, String text) {
        TextView v = findViewById(id);
        if (v != null) v.setText(text);
    }

    private void startClock() {
        stopClock();
        clockRunnable = new Runnable() {
            @Override public void run() {
                updateClocks();
                clockHandler.postDelayed(this, 30_000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void stopClock() {
        if (clockRunnable != null) clockHandler.removeCallbacks(clockRunnable);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // App launching
    // ──────────────────────────────────────────────────────────────────────────

    private void launchApp(AppInfo app) {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(app.packageName, app.activityName));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data + Adapter
    // ──────────────────────────────────────────────────────────────────────────

    static class AppInfo {
        String label, packageName, activityName;
        Drawable icon;
    }

    class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.VH> {
        private final List<AppInfo> list;
        private final boolean windowsStyle;

        AppGridAdapter(List<AppInfo> list, boolean windowsStyle) {
            this.list = list;
            this.windowsStyle = windowsStyle;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            int layoutRes = windowsStyle ? R.layout.item_app_windows : R.layout.item_app_android;
            View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            AppInfo app = list.get(pos);
            h.icon.setImageDrawable(app.icon);
            h.label.setText(app.label);
            h.itemView.setOnClickListener(v -> launchApp(app));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView  label;
            VH(View v) {
                super(v);
                icon  = v.findViewById(R.id.app_icon);
                label = v.findViewById(R.id.app_label);
            }
        }
    }
}
