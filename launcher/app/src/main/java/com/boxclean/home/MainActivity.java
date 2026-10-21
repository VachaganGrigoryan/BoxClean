package com.boxclean.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
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
    private int ambientColor;
    private View glassOn;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            sizeSpacer();
            if (glassOn != null) placeGlass(glassOn, false);
        });
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /** Home pressed again, or the Apps button: start over from the dock, or from the app grid. */
    private void handleIntent(Intent intent) {
        lastFocused = null;
        showApps = intent != null && intent.getBooleanExtra(EXTRA_SHOW_APPS, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        // Re-read on every resume: cheap, and catches installs/uninstalls without a receiver.
        labels = tvApps();
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
        // This is Home: Back returns to the first dock app at the top, like the stock launcher.
        View first = dock.getChildCount() > 0 ? cardAt(dock, 0) : cardAt(grid, 0);
        if (first == null) return;
        scroll.smoothScrollTo(0, 0);
        first.requestFocus();
    }

    // ---- apps ----

    private Map<String, String> tvApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Map<String, String> out = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            out.put(ri.activityInfo.packageName, ri.loadLabel(pm).toString());
        }
        return out;
    }

    private void fill(ViewGroup parent, List<String> pkgs, boolean inDock) {
        parent.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        PackageManager pm = getPackageManager();
        for (String pkg : pkgs) {
            View item = inflater.inflate(R.layout.tile, parent, false);
            View card = item.findViewById(R.id.card);
            TextView name = item.findViewById(R.id.name);
            String label = labels.get(pkg);
            Drawable banner = null;
            try {
                banner = pm.getApplicationBanner(pkg);
                if (banner == null) {
                    ImageView icon = item.findViewById(R.id.icon);
                    TextView text = item.findViewById(R.id.label);
                    icon.setImageDrawable(pm.getApplicationIcon(pkg));
                    icon.setVisibility(View.VISIBLE);
                    text.setText(label);
                    text.setVisibility(View.VISIBLE);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // Uninstalled between query and bind: the next resume drops it.
            }
            ((ImageView) item.findViewById(R.id.banner)).setImageDrawable(banner);
            name.setText(label);
            if (inDock) name.setVisibility(View.GONE);   // the dock's name is the big hero title instead
            card.setTag(pkg);
            card.setContentDescription(label);
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 16 * density);
                }
            });
            card.setClipToOutline(true);
            card.setOnClickListener(v -> launch(pkg));
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

    // ---- focus, navigation, glass ----

    /** Deterministic D-pad: one row at a time, no wrapping, dock and grid columns line up. */
    private boolean navigate(boolean inDock, int pos, int keyCode) {
        int dir;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: dir = GridNavigation.LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: dir = GridNavigation.RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_UP: dir = GridNavigation.UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: dir = GridNavigation.DOWN; break;
            default: return false;
        }
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
            reveal((View) card.getParent());
        }
    }

    /** Scrolls just enough to show a grid tile with its name, plus a little room (CleanYourTV's grid scroll). */
    private void reveal(View item) {
        scroll.post(() -> {
            Rect r = new Rect(0, 0, item.getWidth(), item.getHeight());
            content.offsetDescendantRectToMyCoords(item, r);
            int top = r.top - Math.round(12 * density), bottom = r.bottom + Math.round(12 * density);
            int y = scroll.getScrollY(), h = scroll.getHeight();
            if (bottom > y + h) scroll.smoothScrollTo(0, bottom - h);
            else if (top < y) scroll.smoothScrollTo(0, top);
        });
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
        pill.setOnClickListener(v -> start(intent));
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
        if (color == ambientColor && ambient.getBackground() != null) return;
        ambient.animate().cancel();
        if (ambient.getBackground() == null) {
            setAmbient(color);
            ambient.animate().alpha(1f).setDuration(600).start();
        } else {
            ambient.animate().alpha(0.35f).setDuration(200).withEndAction(() -> {
                setAmbient(color);
                ambient.animate().alpha(1f).setDuration(500).start();
            }).start();
        }
    }

    /**
     * A small white radial-gradient bitmap stretched over the screen and tinted: one GPU texture, where a
     * full-screen GradientDrawable is redrawn on the CPU (CleanYourTV's setAmbient, MIT, see NOTICE).
     */
    private void setAmbient(int color) {
        ambientColor = color;
        BitmapDrawable glow = (BitmapDrawable) ambient.getBackground();
        if (glow == null) {
            int w = 480, h = 270;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.32f, h * 0.42f, w * 0.62f,
                    new int[]{0x5CFFFFFF, 0x26FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP));
            new Canvas(bmp).drawRect(0, 0, w, h, paint);
            glow = new BitmapDrawable(getResources(), bmp);
            glow.setGravity(Gravity.FILL);
            ambient.setBackground(glow);
        }
        glow.setTint(0xFF000000 | color);   // SRC_IN keeps the mask's alpha, takes the colour
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res = hour < 5 ? R.string.good_night : hour < 12 ? R.string.good_morning
                : hour < 17 ? R.string.good_afternoon : hour < 22 ? R.string.good_evening : R.string.good_night;
        greeting.setText(res);
    }
}
