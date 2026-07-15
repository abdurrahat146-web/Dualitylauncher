package com.hiding.launcher;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.net.Uri;
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

    // System services
    private AudioManager audioManager;
    private NotificationManager notificationManager;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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

    private void applyLayout(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_landscape);
            bindWindowsUI();
            bindSlider();
        } else {
            setContentView(R.layout.activity_portrait);
            bindAndroidUI();
        }
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

    // ─────────────────────────────────────────────────────────────────────────
    // Landscape — Windows 11 desktop
    // ─────────────────────────────────────────────────────────────────────────

    private void bindWindowsUI() {
        // ── Start button ──────────────────────────────────────────────────────
        View startBtn    = findViewById(R.id.start_btn);
        View startPanel  = findViewById(R.id.start_panel);

        if (startBtn != null && startPanel != null) {
            startBtn.setOnClickListener(v -> toggleStartMenu(startPanel));
        }

        // ── Start menu: search + app grid ─────────────────────────────────────
        if (startPanel != null) {
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

        // Quick toggles in system tray — wrapped so one bad device/API quirk
        // can't crash the whole launcher (this is the default launcher, so a
        // crash here would crash-loop the entire home screen).
        if (systrayPanel != null) {
            safeBind("wifi",       () -> bindWifiToggle(systrayPanel));
            safeBind("bluetooth",  () -> bindBluetoothToggle(systrayPanel));
            safeBind("airplane",   () -> bindAirplaneToggle(systrayPanel));
            safeBind("focus/dnd",  () -> bindFocusToggle(systrayPanel));
            safeBind("brightness", () -> bindBrightnessSlider(systrayPanel));
            safeBind("volume",     () -> bindVolumeSlider(systrayPanel));
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

    // Runs a binding step, swallowing any exception so a single quirky API
    // call (e.g. Bluetooth returning null on some devices) can't crash the
    // whole launcher and trigger a crash-loop.
    private void safeBind(String label, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            android.util.Log.e("DualityLauncher", "safeBind failed for " + label, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quick settings — real system state where Android allows it
    // ─────────────────────────────────────────────────────────────────────────

    private void setToggleVisual(View tog, boolean on) {
        if (tog == null) return;
        tog.setBackgroundResource(on ? R.drawable.bg_toggle_on : R.drawable.bg_toggle_off);
        if (tog instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) tog;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(on ? 0xFFFFFFFF : 0x88FFFFFF);
                }
            }
        }
    }

    // WiFi — Android 10+ blocks apps from silently toggling WiFi.
    // We show real current state, and tapping opens the actual system WiFi panel.
    private void bindWifiToggle(View parent) {
        View tog = parent.findViewById(R.id.tog_wifi);
        if (tog == null) return;
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean on = wm != null && wm.isWifiEnabled();
        setToggleVisual(tog, on);
        tog.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.Panel.ACTION_WIFI));
            } catch (Exception e) {
                try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
                catch (Exception e2) { Toast.makeText(this, "Couldn't open Wi-Fi settings", Toast.LENGTH_SHORT).show(); }
            }
        });
    }

    // Bluetooth — same platform restriction as WiFi since Android 13.
    private void bindBluetoothToggle(View parent) {
        View tog = parent.findViewById(R.id.tog_bt);
        if (tog == null) return;
        boolean on = false;
        try {
            BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
            on = adapter != null && adapter.isEnabled();
        } catch (Exception ignored) {}
        setToggleVisual(tog, on);
        tog.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open Bluetooth settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Airplane mode — no app has been able to set this directly since Android 4.2.
    private void bindAirplaneToggle(View parent) {
        View tog = parent.findViewById(R.id.tog_air);
        if (tog == null) return;
        boolean on = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        setToggleVisual(tog, on);
        tog.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open Airplane mode settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Focus / Do Not Disturb — this one CAN be toggled directly, with permission.
    private void bindFocusToggle(View parent) {
        View tog = parent.findViewById(R.id.tog_focus);
        if (tog == null) return;
        boolean on = isDndOn();
        setToggleVisual(tog, on);
        tog.setOnClickListener(v -> {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Toast.makeText(this, "Allow Do Not Disturb access, then tap Focus again", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                } catch (Exception ignored) {}
                return;
            }
            boolean newOn = !isDndOn();
            notificationManager.setInterruptionFilter(
                newOn ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                      : NotificationManager.INTERRUPTION_FILTER_ALL);
            setToggleVisual(tog, newOn);
        });
    }

    private boolean isDndOn() {
        if (!notificationManager.isNotificationPolicyAccessGranted()) return false;
        int filter = notificationManager.getCurrentInterruptionFilter();
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    // Brightness slider — real, needs WRITE_SETTINGS (special permission screen).
    private void bindBrightnessSlider(View parent) {
        SeekBar slider = parent.findViewById(R.id.brightness_slider);
        if (slider == null) return;
        try {
            int cur = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            slider.setProgress((int) (cur / 255f * 100));
        } catch (Exception ignored) {}

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (!Settings.System.canWrite(MainActivity.this)) return;
                int value = (int) (progress / 100f * 255);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {
                if (!Settings.System.canWrite(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Allow \"Modify system settings\", then try again", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                    try { startActivity(i); } catch (Exception ignored) {}
                }
            }
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    // Volume slider — works immediately, no special permission needed.
    private void bindVolumeSlider(View parent) {
        SeekBar slider = parent.findViewById(R.id.volume_slider);
        if (slider == null || audioManager == null) return;
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        slider.setMax(max);
        slider.setProgress(cur);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
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
