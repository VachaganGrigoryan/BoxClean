package com.boxclean.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private ScrollView scroll;
    private ViewGroup dock;
    private ViewGroup grid;
    private String lastFocused;
    private List<String> shown = List.of();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        scroll = findViewById(R.id.scroll);
        dock = findViewById(R.id.dock);
        grid = findViewById(R.id.grid);
        findViewById(R.id.settings).setOnClickListener(v -> start(new Intent(Settings.ACTION_SETTINGS)));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        lastFocused = null; // Home pressed while already home: back to the first dock app
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-read on every resume: cheap, and catches installs/uninstalls without a receiver.
        Map<String, String> labels = tvApps();
        List<String> dockPkgs = Apps.dock(
                Arrays.asList(getResources().getStringArray(R.array.dock_packages)), labels.keySet());
        List<String> gridPkgs = Apps.grid(dockPkgs, labels, getPackageName());
        List<String> all = new ArrayList<>(dockPkgs);
        all.addAll(gridPkgs);
        if (!all.equals(shown)) {
            fill(dock, dockPkgs, labels);
            fill(grid, gridPkgs, labels);
            dock.setVisibility(dockPkgs.isEmpty() ? View.GONE : View.VISIBLE);
            shown = all;
        }
        View target = lastFocused == null ? null : getWindow().getDecorView().findViewWithTag(lastFocused);
        if (target == null) target = dock.getChildCount() > 0 ? dock.getChildAt(0) : grid.getChildAt(0);
        if (target != null) target.requestFocus();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // This is Home: there is nothing to go back to.
    }

    private Map<String, String> tvApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Map<String, String> labels = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            labels.put(ri.activityInfo.packageName, ri.loadLabel(pm).toString());
        }
        return labels;
    }

    private void fill(ViewGroup parent, List<String> pkgs, Map<String, String> labels) {
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (String pkg : pkgs) {
            View tile = inflater.inflate(R.layout.tile, parent, false);
            ImageView banner = tile.findViewById(R.id.banner);
            TextView label = tile.findViewById(R.id.label);
            banner.setImageDrawable(art(pkg));
            label.setText(labels.get(pkg));
            tile.setTag(pkg);
            tile.setContentDescription(labels.get(pkg));
            tile.setOnClickListener(v -> launch(pkg));
            tile.setOnFocusChangeListener((v, focused) -> {
                float scale = focused ? 1.08f : 1f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
                label.animate().alpha(focused ? 1f : 0f).setDuration(150).start();
                if (focused) lastFocused = pkg;
                // Focus scrolling only brings the tile into view; at the dock, show the clock row too.
                if (focused && parent == dock) scroll.smoothScrollTo(0, 0);
            });
            parent.addView(tile);
        }
    }

    private Drawable art(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            Drawable banner = pm.getApplicationBanner(pkg);
            return banner != null ? banner : pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) start(intent);
    }

    private void start(Intent intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
