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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

    // Windows mode state
    private boolean startMenuOpen    = false;
    private boolean systrayPanelOpen = false;

    // Toggle states
    private boolean togWifi    = true;
    private boolean togBt      = true;
    private boolean togAir     = false;
    private boolean togFocus   = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

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
            startMenuOpen    = false;
            systrayPanelOpen = false;
            applyLayout(currentOrientation);
        }
    }

    @Override protected void onResume()  { super.onResume();  startClock(); }
    @Override protected void onPause()   { super.onPause();   stopClock();  }
    @Override public    void onBackPressed() { /* launcher eats back */ }

    // ─────────────────────────────────────────────────────────────────────────
    // App loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            installedApps.clear();
            for (ResolveInfo ri : list) {
                if (ri.activityInfo.packageName.equals(getPackageName())) continue;
                AppInfo a = new AppInfo();
                a.label        = ri.loadLabel(pm).toString();
                a.icon         = ri.loadIcon(pm);
                a.packageName  = ri.activityInfo.packageName;
                a.activityName = ri.activityInfo.name;
                installedApps.add(a);
            }
            installedApps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        } catch (Exception e) {
            Toast.makeText(this, "App load error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout switching
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Portrait — Android home screen
    // ─────────────────────────────────────────────────────────────────────────

    private void bindAndroidUI() {
        RecyclerView grid = findViewById(R.id.app_grid);
        if (grid != null) {
            grid.setLayoutManager(new GridLayoutManager(this, 4));
            grid.setAdapter(new AppGridAdapter(new ArrayList<>(installedApps), false));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Landscape — Windows 11 desktop
    // ─────────────────────────────────────────────────────────────────────────

    private void bindWindowsUI() {
        // Desktop grid
        RecyclerView desktop = findViewById(R.id.desktop_grid);
        if (desktop != null) {
            desktop.setLayoutManager(new GridLayoutManager(this, 7));
            desktop.setAdapter(new AppGridAdapter(new ArrayList<>(installedApps), true));
        }

        // ── Start button ──────────────────────────────────────────────────────
        View startBtn    = findViewById(R.id.start_btn);
        View startPanel  = findViewById(R.id.start_panel);

        if (startBtn != null && startPanel != null) {
            startBtn.setOnClickListener(v -> toggleStartMenu(startPanel));
        }

        // ── Start menu: search + app grid ─────────────────────────────────────
        if (startPanel != null) {
            // Dismiss when clicking the panel backdrop (but not the panel itself — it's clickable)
            View backdrop = findViewById(R.id.desktop_grid);
            if (backdrop != null) backdrop.setOnClickListener(v -> closeOverlays());

            // App grid inside start menu
            RecyclerView startApps = startPanel.findViewById(R.id.start_apps);
            final AppGridAdapter[] startAdapter = { new AppGridAdapter(new ArrayList<>(installedApps), true) };
            if (startApps != null) {
                startApps.setLayoutManager(new GridLayoutManager(this, 5));
                startApps.setAdapter(startAdapter[0]);
            }

            // Search box — live filter
            EditText search = startPanel.findViewById(R.id.start_search);
            if (search != null && startApps != null) {
                search.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                    @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                    @Override public void afterTextChanged(Editable s) {
                        String q = s.toString().toLowerCase(Locale.getDefault()).trim();
                        List<AppInfo> filtered = new ArrayList<>();
                        for (AppInfo a : installedApps) {
                            if (q.isEmpty() || a.label.toLowerCase(Locale.getDefault()).contains(q))
                                filtered.add(a);
                        }
                        AppGridAdapter fa = new AppGridAdapter(filtered, true);
                        startApps.setAdapter(fa);
                        startAdapter[0] = fa;
                    }
                });
            }

            // "Switch Launcher" in start menu bottom row
            View switchBtn = startPanel.findViewById(R.id.start_switch_btn);
            if (switchBtn != null) switchBtn.setOnClickListener(v -> {
                closeOverlays();
                switchLauncher();
            });
        }

        // ── System tray button ────────────────────────────────────────────────
        View systrayBtn   = findViewById(R.id.systray_btn);
        View systrayPanel = findViewById(R.id.systray_panel);

        if (systrayBtn != null && systrayPanel != null) {
            systrayBtn.setOnClickListener(v -> toggleSystrayPanel(systrayPanel));
        }

        // Quick toggles in system tray
        if (systrayPanel != null) {
            bindToggle(systrayPanel, R.id.tog_wifi,  () -> togWifi  = !togWifi);
            bindToggle(systrayPanel, R.id.tog_bt,    () -> togBt    = !togBt);
            bindToggle(systrayPanel, R.id.tog_air,   () -> togAir   = !togAir);
            bindToggle(systrayPanel, R.id.tog_focus, () -> togFocus = !togFocus);
        }

        // ── Chat button ───────────────────────────────────────────────────────
        View chatBtn = findViewById(R.id.chat_btn);
        if (chatBtn != null) chatBtn.setOnClickListener(v -> openChat());

        // ── Taskbar search ────────────────────────────────────────────────────
        View tbSearch = findViewById(R.id.taskbar_search);
        if (tbSearch != null) tbSearch.setOnClickListener(v -> {
            closeOverlays();
            toggleStartMenu(startPanel);
            // Focus the search box
            if (startPanel != null) {
                EditText box = startPanel.findViewById(R.id.start_search);
                if (box != null) box.requestFocus();
            }
        });

        // ── Notification button (decorative) ──────────────────────────────────
        View notifBtn = findViewById(R.id.notif_btn);
        if (notifBtn != null) notifBtn.setOnClickListener(v -> toggleSystrayPanel(systrayPanel));
    }

    private void bindToggle(View parent, int id, Runnable toggle) {
        View tog = parent.findViewById(id);
        if (tog == null) return;
        tog.setOnClickListener(v -> {
            toggle.run();
            boolean on = getToggleState(id);
            tog.setBackgroundResource(on ? R.drawable.bg_toggle_on : R.drawable.bg_toggle_off);
            for (int i = 0; i < ((ViewGroup) tog).getChildCount(); i++) {
                View child = ((ViewGroup) tog).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(on ? 0xFFFFFFFF : 0x88FFFFFF);
                }
            }
        });
    }

    private boolean getToggleState(int id) {
        if (id == R.id.tog_wifi)  return togWifi;
        if (id == R.id.tog_bt)    return togBt;
        if (id == R.id.tog_air)   return togAir;
        if (id == R.id.tog_focus) return togFocus;
        return false;
    }

    private void toggleStartMenu(View panel) {
        if (panel == null) return;
        if (systrayPanelOpen) closeSystrayPanel();
        startMenuOpen = !startMenuOpen;
        panel.setVisibility(startMenuOpen ? View.VISIBLE : View.GONE);
        if (startMenuOpen) {
            // Reset search when opening
            EditText box = panel.findViewById(R.id.start_search);
            if (box != null) { box.setText(""); }
            RecyclerView rv = panel.findViewById(R.id.start_apps);
            if (rv != null) rv.setAdapter(new AppGridAdapter(new ArrayList<>(installedApps), true));
        }
    }

    private void toggleSystrayPanel(View panel) {
        if (panel == null) return;
        if (startMenuOpen) {
            View sp = findViewById(R.id.start_panel);
            if (sp != null) sp.setVisibility(View.GONE);
            startMenuOpen = false;
        }
        systrayPanelOpen = !systrayPanelOpen;
        panel.setVisibility(systrayPanelOpen ? View.VISIBLE : View.GONE);
        if (systrayPanelOpen) {
            // Reset slider
            SeekBar s = panel.findViewById(R.id.switch_slider);
            if (s != null) s.setProgress(0);
            updateClocks();
        }
    }

    private void closeSystrayPanel() {
        View panel = findViewById(R.id.systray_panel);
        if (panel != null) panel.setVisibility(View.GONE);
        systrayPanelOpen = false;
    }

    private void closeOverlays() {
        View sp = findViewById(R.id.start_panel);
        View st = findViewById(R.id.systray_panel);
        if (sp != null) sp.setVisibility(View.GONE);
        if (st != null) st.setVisibility(View.GONE);
        startMenuOpen    = false;
        systrayPanelOpen = false;
    }

    private void openChat() {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_APP_MESSAGING);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                startActivity(Intent.createChooser(i, "Open Chat"));
            } catch (Exception e2) {
                Toast.makeText(this, "No messaging app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Switch launcher slider (works in both portrait + landscape)
    // ─────────────────────────────────────────────────────────────────────────

    private void bindSlider() {
        SeekBar slider     = findViewById(R.id.switch_slider);
        TextView sliderLbl = findViewById(R.id.slider_label);
        if (slider == null) return;

        slider.setProgress(0);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (sliderLbl != null)
                    sliderLbl.setText(p >= 80 ? "Release to switch! →" : "Slide → to switch launcher");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (s.getProgress() >= 90) {
                    closeOverlays();
                    switchLauncher();
                } else {
                    s.setProgress(0);
                    if (sliderLbl != null) sliderLbl.setText("Slide → to switch launcher");
                }
            }
        });
    }

    private void switchLauncher() {
        for (String action : new String[]{
            "android.settings.HOME_SETTINGS",
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
        }) {
            try { startActivity(new Intent(action)); return; } catch (Exception ignored) {}
        }
        Toast.makeText(this, "Settings → Apps → Default Apps → Home App", Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clocks
    // ─────────────────────────────────────────────────────────────────────────

    private void updateClocks() {
        Date now = new Date();
        String t24  = fmt("HH:mm",              now);
        String t12  = fmt("h:mm a",             now);
        String dLng = fmt("EEEE, MMMM d, yyyy", now);
        String dShrt= fmt("EEE  M/d",           now);

        setTxt(R.id.status_time,   t24);
        setTxt(R.id.main_clock,    t24);
        setTxt(R.id.main_date,     fmt("EEEE, MMMM d", now));
        setTxt(R.id.tray_clock,    t12);
        setTxt(R.id.tray_date,     dLng);

        TextView tb = findViewById(R.id.taskbar_clock);
        if (tb != null) tb.setText(t12 + "\n" + dShrt);
    }

    private String fmt(String pattern, Date d) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(d);
    }

    private void setTxt(int id, String s) {
        TextView v = findViewById(id);
        if (v != null) v.setText(s);
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

    // ─────────────────────────────────────────────────────────────────────────
    // App launch (with tactile scale animation)
    // ─────────────────────────────────────────────────────────────────────────

    private void launchApp(AppInfo app, View fromView) {
        closeOverlays();
        if (fromView != null) {
            fromView.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70)
                .withEndAction(() -> {
                    fromView.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
                    doLaunch(app);
                }).start();
        } else {
            doLaunch(app);
        }
    }

    private void doLaunch(AppInfo app) {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName(app.packageName, app.activityName));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data + Adapter
    // ─────────────────────────────────────────────────────────────────────────

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
            int res = windowsStyle ? R.layout.item_app_windows : R.layout.item_app_android;
            return new VH(LayoutInflater.from(parent.getContext()).inflate(res, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            AppInfo app = list.get(pos);
            h.icon.setImageDrawable(app.icon);
            h.label.setText(app.label);
            h.itemView.setOnClickListener(v -> launchApp(app, h.itemView));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon; TextView label;
            VH(View v) { super(v); icon = v.findViewById(R.id.app_icon); label = v.findViewById(R.id.app_label); }
        }
    }
}
