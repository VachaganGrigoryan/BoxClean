package com.boxclean.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apple TV style home. Look and motion follow CleanYourTV's launcher (MIT, see NOTICE): Outfit type, a big
 * name over a low dock, a colour glow sampled from the focused app, and one glass highlight that glides
 * between tiles. Plain framework views, no AndroidX, no network.
 */
public class MainActivity extends Activity {
    /** Set by KeyService for the remote's Apps button: open with the app grid focused. */
    static final String EXTRA_SHOW_APPS = "com.boxclean.home.SHOW_APPS";
    private static final int COLUMNS = 4;
    private static final float FOCUS_SCALE = 1.10f;

    private ScrollView scroll;
    private ViewGroup content;
    private ViewGroup dock;
    private ViewGroup grid;
    private View spacer;
    private View ambient;
    private TextView hero;
    private TextView greeting;
    private GlassView glass;
    private float density;

    private Map<String, String> labels = new HashMap<>();
    private final Map<String, Integer> colors = new HashMap<>();
    private List<String> shown = List.of();
    private String lastFocused;
    private boolean showApps;
    private View glassOn;
    /** Set when the bento layout is on; it then runs the whole screen and the classic fields stay unused. */
    private BentoHome bento;

    /** Which home layout to show: bento (the default) or this classic one. */
    static boolean bentoOn(Context context) {
        return context.getSharedPreferences("home", MODE_PRIVATE).getBoolean("bento", true);
    }

    /** The Layout button on either home: flip to the other layout and rebuild the screen. */
    static void switchLayout(Activity activity) {
        activity.getSharedPreferences("home", MODE_PRIVATE).edit().putBoolean("bento", !bentoOn(activity)).apply();
        activity.recreate();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (bentoOn(this)) {
            bento = new BentoHome(this);
            bento.handleIntent(getIntent());
            return;
        }
        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;
        scroll = findViewById(R.id.scroll);
        content = findViewById(R.id.content);
        dock = findViewById(R.id.dock);
        grid = findViewById(R.id.grid);
        spacer = findViewById(R.id.spacer);
        ambient = findViewById(R.id.ambient);
        hero = findViewById(R.id.hero);
        greeting = findViewById(R.id.greeting);
        glass = findViewById(R.id.glass);

        // The glass is the focused card at FOCUS_SCALE plus 6dp of glow on each side.
        ViewGroup.LayoutParams lp = glass.getLayoutParams();
        lp.width = Math.round((180 * FOCUS_SCALE + 12) * density);
        lp.height = Math.round((101 * FOCUS_SCALE + 12) * density);
        glass.setLayoutParams(lp);
        glass.setRadius(16 * FOCUS_SCALE * density);

        setUpPill(findViewById(R.id.wifi), new Intent(Settings.ACTION_WIFI_SETTINGS));
        setUpPill(findViewById(R.id.settings), new Intent(Settings.ACTION_SETTINGS));
        setUpPill(findViewById(R.id.layout), null);
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            sizeSpacer();
            if (glassOn != null) placeGlass(glassOn, false);
        });
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (bento != null) bento.handleIntent(intent);
        else handleIntent(intent);
    }

    /** Home pressed again, or the Apps button: start over from the dock, or from the app grid. */
    private void handleIntent(Intent intent) {
        lastFocused = null;
        showApps = intent != null && intent.getBooleanExtra(EXTRA_SHOW_APPS, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bento != null) {
            bento.resume();
            return;
        }
        updateGreeting();
        // Re-read on every resume: cheap, and catches installs/uninstalls without a receiver.
        labels = Tiles.tvApps(getPackageManager());
        List<String> dockPkgs = Apps.dock(
                Arrays.asList(getResources().getStringArray(R.array.dock_packages)), labels.keySet());
        List<String> gridPkgs = Apps.grid(dockPkgs, labels, getPackageName());
        List<String> all = new ArrayList<>(dockPkgs);
        all.addAll(gridPkgs);
        if (!all.equals(shown)) {
            fill(dock, dockPkgs, true);
            fill(grid, gridPkgs, false);
            dock.setVisibility(dockPkgs.isEmpty() ? View.GONE : View.VISIBLE);
            shown = all;
        }
        View target = lastFocused == null ? null : content.findViewWithTag(lastFocused);
        if (target == null && showApps) target = cardAt(grid, 0);
        if (target == null) target = dock.getChildCount() > 0 ? cardAt(dock, 0) : cardAt(grid, 0);
        showApps = false;
        if (target != null) {
            View t = target;
            if (t.getParent().getParent() == dock) scroll.scrollTo(0, 0);
            content.post(t::requestFocus);   // after layout, so the glass lands on the tile's real position
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (bento != null) {
            bento.back();
            return;
        }
        // This is Home: Back returns to the first dock app at the top, like the stock launcher.
        View first = dock.getChildCount() > 0 ? cardAt(dock, 0) : cardAt(grid, 0);
        if (first == null) return;
        scroll.smoothScrollTo(0, 0);
        first.requestFocus();
    }

    // ---- apps ----

    private void fill(ViewGroup parent, List<String> pkgs, boolean inDock) {
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (String pkg : pkgs) {
            View item = inflater.inflate(R.layout.tile, parent, false);
            View card = item.findViewById(R.id.card);
            TextView name = item.findViewById(R.id.name);
            String label = labels.get(pkg);
            Tiles.bind(card, pkg, label, getPackageManager(), 16 * density);
            name.setText(label);
            if (inDock) name.setVisibility(View.GONE);   // the dock's name is the big hero title instead
            card.setOnClickListener(v -> Tiles.launch(this, pkg));
            card.setOnKeyListener((v, keyCode, event) ->
                    event.getAction() == KeyEvent.ACTION_DOWN && navigate(inDock, parent.indexOfChild(item), keyCode));
            card.setOnFocusChangeListener((v, focused) -> onTileFocus(v, name, pkg, inDock, focused));
            parent.addView(item);
        }
        // Tiles rise into place one after another (CleanYourTV's tile_enter).
        parent.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, R.anim.tile_enter), 0.06f));
        parent.scheduleLayoutAnimation();
    }

    private static View cardAt(ViewGroup parent, int i) {
        return i >= 0 && i < parent.getChildCount() ? parent.getChildAt(i).findViewById(R.id.card) : null;
    }

    // ---- focus, navigation, glass ----

    /** Deterministic D-pad: one row at a time, no wrapping, dock and grid columns line up. */
    private boolean navigate(boolean inDock, int pos, int keyCode) {
        int dir = GridNavigation.direction(keyCode);
        if (dir < 0) return false;
        ViewGroup here = inDock ? dock : grid;
        int next = GridNavigation.next(pos, here.getChildCount(), COLUMNS, dir);
        View target;
        if (next >= 0) {
            target = cardAt(here, next);
        } else if (next == GridNavigation.UP_OUT) {
            if (inDock) return false;   // up from the dock: the framework finds the top-bar pills
            target = cardAt(dock, Math.min(pos % COLUMNS, dock.getChildCount() - 1));
        } else if (next == GridNavigation.DOWN_OUT && inDock) {
            target = cardAt(grid, Math.min(pos, grid.getChildCount() - 1));
        } else {
            return true;   // STAY, or down from the last row: swallow so focus never falls out
        }
        if (target != null) target.requestFocus();
        return true;
    }

    private void onTileFocus(View card, TextView name, String pkg, boolean inDock, boolean focused) {
        card.animate().cancel();
        name.animate().cancel();
        float s = focused ? FOCUS_SCALE : 1f;
        card.animate().scaleX(s).scaleY(s)
                .setDuration(focused ? 260 : 180)
                .setInterpolator(new OvershootInterpolator(focused ? 1.6f : 0f))
                .withLayer().start();
        name.animate().alpha(focused ? 1f : 0f)
                .translationY((focused ? 4f : -6f) * density)
                .setDuration(220).start();
        if (!focused) return;
        lastFocused = pkg;
        placeGlass(card, true);
        if (inDock) {
            scroll.smoothScrollTo(0, 0);   // focus scrolling only reveals the tile; show the header too
            showHero(pkg);
        } else {
            Tiles.reveal(scroll, content, (View) card.getParent());
        }
    }

    /** Glides the glass onto a card; the first time (or after the pills) it fades in in place. */
    private void placeGlass(View card, boolean animate) {
        Rect r = new Rect(0, 0, card.getWidth(), card.getHeight());
        content.offsetDescendantRectToMyCoords(card, r);
        float x = r.centerX() - glass.getWidth() / 2f;
        float y = r.centerY() - glass.getHeight() / 2f;
        boolean hidden = glassOn == null;
        glassOn = card;
        if (!animate || hidden) {
            glass.animate().translationX(x).translationY(y).setDuration(0).start();
            if (hidden) glass.animate().alpha(1f).setDuration(180).start();
        } else {
            glass.animate().translationX(x).translationY(y).alpha(1f)
                    .setDuration(260).setInterpolator(new OvershootInterpolator(1.0f)).start();
        }
    }

    private void setUpPill(View pill, Intent intent) {
        // No intent: the Layout pill.
        pill.setOnClickListener(v -> {
            if (intent == null) switchLayout(this);
            else Tiles.start(this, intent);
        });
        pill.setOnFocusChangeListener((v, focused) -> {
            if (!focused) return;
            glassOn = null;   // the pill has its own focused look
            glass.animate().alpha(0f).setDuration(150).start();
        });
    }

    /** Grows the spacer so the dock's bottom sits 56dp above the screen's bottom on the first screen. */
    private void sizeSpacer() {
        if (dock.getVisibility() != View.VISIBLE || scroll.getHeight() == 0) return;
        View column = (View) dock.getParent();
        int dockBottom = column.getTop() + dock.getBottom();
        int delta = scroll.getHeight() - Math.round(56 * density) - dockBottom;
        ViewGroup.LayoutParams lp = spacer.getLayoutParams();
        int height = Math.max(0, lp.height + delta);
        if (Math.abs(height - lp.height) > 1) {
            lp.height = height;
            spacer.setLayoutParams(lp);
        }
    }

    // ---- hero title and ambient glow ----

    private void showHero(String pkg) {
        String label = labels.get(pkg);
        Integer cached = colors.get(pkg);
        int color;
        if (cached != null) {
            color = cached;
        } else {
            ImageView banner = content.findViewWithTag(pkg).findViewById(R.id.banner);
            color = Glow.vividColor(banner.getDrawable());
            colors.put(pkg, color);
        }
        if (label != null && !label.contentEquals(hero.getText())) {
            hero.animate().cancel();
            if (hero.getAlpha() == 0f) {
                hero.setText(label);
                hero.setTranslationX(0f);
                hero.animate().alpha(1f).setDuration(260).start();
            } else {
                hero.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction(() -> {
                    hero.setText(label);
                    hero.setTranslationX(12 * density);
                    hero.animate().alpha(1f).translationX(0f).setDuration(200).start();
                }).start();
            }
        }
        Glow.fadeTo(ambient, color);
    }

    private void updateGreeting() {
        greeting.setText(greetingRes());
    }

    static int greetingRes() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 5 ? R.string.good_night : hour < 12 ? R.string.good_morning
                : hour < 17 ? R.string.good_afternoon : hour < 22 ? R.string.good_evening : R.string.good_night;
    }
}
