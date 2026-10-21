package com.boxclean.home;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** What every home layout shares: the app lists, the colour cache, the focus lift and the utility buttons. */
abstract class BaseHome implements HomeLayout {
    protected final Activity activity;
    protected final float density;
    protected Map<String, String> labels = new HashMap<>();
    /** The configured dock apps that are installed, then every other app by label. */
    protected List<String> dock = List.of(), rest = List.of();
    /** The app to focus again on resume; null starts over from the first app. */
    protected String lastFocused;
    /** The remote's Apps button asked for this resume to focus the "all apps" part. */
    protected boolean showApps;
    private final Map<String, Integer> colors = new HashMap<>();
    private List<String> shown = List.of();

    BaseHome(Activity activity, int layoutRes) {
        this.activity = activity;
        activity.setContentView(layoutRes);
        density = activity.getResources().getDisplayMetrics().density;
    }

    @Override
    public void handleIntent(Intent intent) {
        lastFocused = null;
        showApps = intent != null && intent.getBooleanExtra(MainActivity.EXTRA_SHOW_APPS, false);
    }

    /**
     * Re-reads the installed apps into {@link #dock} and {@link #rest}: cheap, and catches installs and uninstalls
     * without a receiver. True when they changed and the views need rebuilding.
     */
    protected boolean loadApps() {
        labels = Tiles.tvApps(activity.getPackageManager());
        dock = Apps.dock(
                Arrays.asList(activity.getResources().getStringArray(R.array.dock_packages)), labels.keySet());
        rest = Apps.grid(dock, labels, activity.getPackageName());
        List<String> all = new ArrayList<>(dock);
        all.addAll(rest);
        if (all.equals(shown)) return false;
        shown = all;
        return true;
    }

    protected <T extends View> T find(int id) {
        return activity.findViewById(id);
    }

    /** The app's glow colour, sampled once from the banner in its card. */
    protected int colorOf(String pkg, View card) {
        Integer color = colors.get(pkg);
        if (color == null) {
            color = Glow.vividColor(((ImageView) card.findViewById(R.id.banner)).getDrawable());
            colors.put(pkg, color);
        }
        return color;
    }

    /** A tile.xml tile for one app: bound, named and launching on OK. The caller adds focus and keys. */
    protected View tile(ViewGroup parent, String pkg, float radiusDp) {
        View item = activity.getLayoutInflater().inflate(R.layout.tile, parent, false);
        View card = item.findViewById(R.id.card);
        String label = labels.get(pkg);
        Tiles.bind(card, pkg, label, activity.getPackageManager(), radiusDp * density);
        ((TextView) item.findViewById(R.id.name)).setText(label);
        card.setOnClickListener(v -> Tiles.launch(activity, pkg));
        return item;
    }

    /** Shows an app in a reusable card (cell_app.xml views), clearing the icon fallback a previous app left. */
    protected void bindCard(View card, String pkg, float radiusDp) {
        card.findViewById(R.id.icon).setVisibility(View.GONE);
        card.findViewById(R.id.label).setVisibility(View.GONE);
        Tiles.bind(card, pkg, labels.get(pkg), activity.getPackageManager(), radiusDp * density);
    }

    protected void size(View v, int widthDp, int heightDp) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = Math.round(widthDp * density);
        lp.height = Math.round(heightDp * density);
    }

    /** Tiles rise into place one after another (CleanYourTV's tile_enter). */
    protected void animateIn(ViewGroup parent) {
        parent.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(activity, R.anim.tile_enter), 0.06f));
        parent.scheduleLayoutAnimation();
    }

    /** A focused view grows a little and rises above its neighbours. */
    protected void lift(View v, boolean focused, float scale) {
        v.animate().cancel();
        float s = focused ? scale : 1f;
        v.animate().scaleX(s).scaleY(s).translationZ(focused ? 6 * density : 0f)
                .setDuration(focused ? 260 : 180)
                .setInterpolator(new OvershootInterpolator(focused ? 1.4f : 0f))
                .withLayer().start();
    }

    /** Wires the Wi-Fi, Settings and Layout buttons every layout has. */
    protected void utilities(View wifi, View settings, View layout) {
        wifi.setOnClickListener(v -> Tiles.start(activity, new Intent(Settings.ACTION_WIFI_SETTINGS)));
        settings.setOnClickListener(v -> Tiles.start(activity, new Intent(Settings.ACTION_SETTINGS)));
        layout.setOnClickListener(v -> LayoutPicker.show(activity));
    }

    /** Swaps a title's text: the old one slides out left, the new one in from the right. */
    protected void swapText(TextView view, String text) {
        if (text == null || text.contentEquals(view.getText())) return;
        view.animate().cancel();
        if (view.getAlpha() == 0f) {
            view.setText(text);
            view.setTranslationX(0f);
            view.animate().alpha(1f).setDuration(260).start();
        } else {
            view.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction(() -> {
                view.setText(text);
                view.setTranslationX(12 * density);
                view.animate().alpha(1f).translationX(0f).setDuration(200).start();
            }).start();
        }
    }

    static int greetingRes() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 5 ? R.string.good_night : hour < 12 ? R.string.good_morning
                : hour < 17 ? R.string.good_afternoon : hour < 22 ? R.string.good_evening : R.string.good_night;
    }
}
