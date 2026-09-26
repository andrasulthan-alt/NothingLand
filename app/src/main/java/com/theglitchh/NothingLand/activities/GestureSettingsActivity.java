package com.theglitchh.NothingLand.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.theglitchh.NothingLand.utils.QuickActions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings for what happens when the idle island is tapped, double-tapped,
 * long-pressed or swiped, plus the favorite apps shown in the apps card.
 */
public class GestureSettingsActivity extends AppCompatActivity {

    public static final String[] KEYS = {"gesture_tap", "gesture_double_tap", "gesture_long_press",
            "gesture_swipe_left", "gesture_swipe_right", "gesture_swipe_up", "gesture_swipe_down"};
    private static final String[] NAMES = {"Tap", "Double tap", "Long press",
            "Swipe left", "Swipe right", "Swipe up", "Swipe down"};
    /** Defaults when nothing has been chosen yet. */
    public static final String[] DEFAULTS = {QuickActions.NONE, QuickActions.FLASHLIGHT, QuickActions.SLIDERS,
            QuickActions.NONE, QuickActions.NONE, QuickActions.NONE, QuickActions.NOTIFICATIONS};
    public static final String PREF_FAVORITES = "favorite_apps";

    private SharedPreferences prefs;
    private ArrayAdapter<String> adapter;
    private final List<String> rows = new ArrayList<>();

    public static String actionFor(SharedPreferences prefs, String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return prefs.getString(key, DEFAULTS[i]);
        }
        return prefs.getString(key, QuickActions.NONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gestures and quick cards");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        ListView list = new ListView(this);
        int pad = dp(8);
        list.setPadding(pad, pad, pad, pad);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, rows) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                if (position < KEYS.length) {
                    t1.setText(NAMES[position]);
                    t2.setText(QuickActions.label(GestureSettingsActivity.this, actionFor(prefs, KEYS[position])));
                } else {
                    t1.setText("Favorite apps");
                    t2.setText(favoritesSummary());
                }
                return v;
            }
        };
        for (String n : NAMES) rows.add(n);
        rows.add("Favorite apps");
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < KEYS.length) pickAction(position);
            else pickFavorites();
        });

        // The app theme has no action bar, so draw a simple header ourselves.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Gestures and quick cards");
        title.setTextSize(24);
        title.setPadding(dp(20), dp(28), dp(20), dp(8));
        TextView hint = new TextView(this);
        hint.setText("What the island does when nothing is playing or active. Tap a gesture to choose its action.");
        hint.setTextSize(13);
        hint.setPadding(dp(20), 0, dp(20), dp(8));
        root.addView(title);
        root.addView(hint);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private String favoritesSummary() {
        String csv = prefs.getString(PREF_FAVORITES, "");
        if (csv == null || csv.trim().isEmpty()) return "None chosen (shown in the favorite apps card)";
        int n = csv.split(",").length;
        return n + (n == 1 ? " app" : " apps") + " in the favorite apps card";
    }

    private void pickAction(int gesture) {
        String current = actionFor(prefs, KEYS[gesture]);
        int checked = 0;
        for (int i = 0; i < QuickActions.IDS.length; i++) {
            if (QuickActions.IDS[i].equals(current)) checked = i;
        }
        if (current.startsWith(QuickActions.APP_PREFIX)) checked = QuickActions.IDS.length - 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle(NAMES[gesture])
                .setSingleChoiceItems(QuickActions.LABELS, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String id = QuickActions.IDS[which];
                    if (QuickActions.OPEN_APP.equals(id)) {
                        pickApp(gesture);
                    } else {
                        save(KEYS[gesture], id);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class AppEntry {
        String pkg, name;
    }

    private List<AppEntry> launchableApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        List<AppEntry> apps = new ArrayList<>();
        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            AppEntry e = new AppEntry();
            e.pkg = pkg;
            e.name = String.valueOf(ri.loadLabel(pm));
            apps.add(e);
        }
        Collections.sort(apps, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return apps;
    }

    private void pickApp(int gesture) {
        List<AppEntry> apps = launchableApps();
        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) names[i] = apps.get(i).name;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Open which app?")
                .setItems(names, (dialog, which) -> save(KEYS[gesture], QuickActions.APP_PREFIX + apps.get(which).pkg))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickFavorites() {
        List<AppEntry> apps = launchableApps();
        String csv = prefs.getString(PREF_FAVORITES, "");
        Set<String> chosen = new HashSet<>();
        if (csv != null) chosen.addAll(Arrays.asList(csv.split(",")));
        String[] names = new String[apps.size()];
        boolean[] checks = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            names[i] = apps.get(i).name;
            checks[i] = chosen.contains(apps.get(i).pkg);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Favorite apps (up to 8)")
                .setMultiChoiceItems(names, checks, (dialog, which, isChecked) -> checks[which] = isChecked)
                .setPositiveButton("Save", (dialog, which) -> {
                    StringBuilder sb = new StringBuilder();
                    int n = 0;
                    for (int i = 0; i < apps.size() && n < 8; i++) {
                        if (!checks[i]) continue;
                        if (sb.length() > 0) sb.append(",");
                        sb.append(apps.get(i).pkg);
                        n++;
                    }
                    save(PREF_FAVORITES, sb.toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void save(String key, String value) {
        prefs.edit().putString(key, value).apply();
        adapter.notifyDataSetChanged();
        // Tell the island service so the change applies right away.
        Intent intent = new Intent(getPackageName() + ".SETTINGS_CHANGED");
        Bundle b = new Bundle();
        for (java.util.Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Boolean) b.putBoolean(e.getKey(), (Boolean) v);
            else if (v instanceof String) b.putString(e.getKey(), (String) v);
        }
        intent.putExtra("settings", b);
        sendBroadcast(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
