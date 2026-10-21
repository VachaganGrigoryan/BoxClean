package com.boxclean.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bento home: a board of rounded cells (clock, four big apps, Wi-Fi, Settings, Layout) fills the first screen,
 * every other app sits in a grid below it. Same apps, glow and launching as the classic home; MainActivity
 * hands it the screen when the Layout switch is on bento.
 */
final class BentoHome {
    private static final int COLUMNS = 4;
    private static final int[] SLOTS = {R.id.app1, R.id.app2, R.id.app3, R.id.app4};
    private static final float CELL_SCALE = 1.04f;
    private static final float TILE_SCALE = 1.08f;
    /** Room left under the board on the first screen, so the "All apps" title shows there. */
    private static final int PEEK_DP = 60;

    private final Activity activity;
    private final ScrollView scroll;
    private final ViewGroup content;
    private final View board;
    private final ViewGroup grid;
    private final View ambient;
    private final TextView greeting;
    /** The board's bottom row, left to right; it lines up with the grid's 4 columns. */
    private final View[] bottomRow;
    private final float density;

    private final Map<String, Integer> colors = new HashMap<>();
    private Map<String, String> labels = new HashMap<>();
    private List<String> shown = List.of();
    private String lastFocused;
    private boolean showApps;

    BentoHome(Activity activity) {
        this.activity = activity;
        activity.setContentView(R.layout.activity_bento);
        density = activity.getResources().getDisplayMetrics().density;
        scroll = activity.findViewById(R.id.scroll);
        content = activity.findViewById(R.id.content);
        board = activity.findViewById(R.id.board);
        grid = activity.findViewById(R.id.grid);
        ambient = activity.findViewById(R.id.ambient);
        greeting = activity.findViewById(R.id.greeting);
        bottomRow = new View[]{activity.findViewById(R.id.app4), activity.findViewById(R.id.wifi),
                activity.findViewById(R.id.settings), activity.findViewById(R.id.layout)};

        for (int id : SLOTS) {
            View cell = activity.findViewById(id);
            cell.setOnFocusChangeListener((v, focused) -> {
                lift(v, focused, CELL_SCALE);
                if (focused) onAppFocus(v, (String) v.getTag());
            });
        }
        activity.findViewById(R.id.wifi).setOnClickListener(
                v -> Tiles.start(activity, new Intent(Settings.ACTION_WIFI_SETTINGS)));
        activity.findViewById(R.id.settings).setOnClickListener(
                v -> Tiles.start(activity, new Intent(Settings.ACTION_SETTINGS)));
        activity.findViewById(R.id.layout).setOnClickListener(v -> MainActivity.switchLayout(activity));
        for (int col = 0; col < bottomRow.length; col++) {
            int c = col;
            View cell = bottomRow[col];
            if (cell.getId() != R.id.app4) {
                cell.setOnFocusChangeListener((v, focused) -> {
                    lift(v, focused, CELL_SCALE);
                    if (focused) scroll.smoothScrollTo(0, 0);
                });
            }
            cell.setOnKeyListener((v, keyCode, event) -> event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && focusGrid(c));
        }
        scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> sizeBoard());
    }

    /** Home pressed again, or the Apps button: start over from the board, or from the app grid. */
    void handleIntent(Intent intent) {
        lastFocused = null;
        showApps = intent != null && intent.getBooleanExtra(MainActivity.EXTRA_SHOW_APPS, false);
    }

    void resume() {
        greeting.setText(MainActivity.greetingRes());
        // Re-read on every resume: cheap, and catches installs/uninstalls without a receiver.
        labels = Tiles.tvApps(activity.getPackageManager());
        List<String> dock = Apps.dock(
                Arrays.asList(activity.getResources().getStringArray(R.array.dock_packages)), labels.keySet());
        List<String> rest = Apps.grid(dock, labels, activity.getPackageName());
        List<String> onBoard = Apps.board(dock, rest, SLOTS.length);
        List<String> inGrid = new ArrayList<>(rest);
        inGrid.removeAll(onBoard);
        List<String> all = new ArrayList<>(onBoard);
        all.addAll(inGrid);
        if (!all.equals(shown)) {
            bindBoard(onBoard);
            fillGrid(inGrid);
            shown = all;
        }
        View target = lastFocused == null ? null : content.findViewWithTag(lastFocused);
        if (target == null && showApps) target = cardAt(0);
        showApps = false;
        if (target == null) target = firstCell();
        if (!isInGrid(target)) scroll.scrollTo(0, 0);
        content.post(target::requestFocus);   // after layout, so scrolling sees the tile's real position
    }

    /** This is Home: Back returns to the board's first cell at the top. */
    void back() {
        scroll.smoothScrollTo(0, 0);
        firstCell().requestFocus();
    }

    private View firstCell() {
        View app1 = activity.findViewById(R.id.app1);
        return app1.getVisibility() == View.VISIBLE ? app1 : activity.findViewById(R.id.wifi);
    }

    // ---- board ----

    private void bindBoard(List<String> pkgs) {
        PackageManager pm = activity.getPackageManager();
        for (int i = 0; i < SLOTS.length; i++) {
            View cell = activity.findViewById(SLOTS[i]);
            if (i >= pkgs.size()) {
                cell.setVisibility(View.GONE);
                cell.setTag(null);
                continue;
            }
            String pkg = pkgs.get(i);
            // Slots are reused across resumes: clear the icon fallback a previous app may have left.
            cell.findViewById(R.id.icon).setVisibility(View.GONE);
            cell.findViewById(R.id.label).setVisibility(View.GONE);
            Tiles.bind(cell, pkg, labels.get(pkg), pm, 24 * density);
            cell.setOnClickListener(v -> Tiles.launch(activity, pkg));
            cell.setVisibility(View.VISIBLE);
        }
    }

    /** Sizes the board to the first screen minus the peek, so the cells' weights share the TV's real height. */
    private void sizeBoard() {
        int height = scroll.getHeight() - content.getPaddingTop() - Math.round(PEEK_DP * density);
        ViewGroup.LayoutParams lp = board.getLayoutParams();
        if (height > 0 && Math.abs(height - lp.height) > 1) {
            lp.height = height;
            board.setLayoutParams(lp);
        }
    }

    // ---- app grid ----

    private void fillGrid(List<String> pkgs) {
        grid.removeAllViews();
        LayoutInflater inflater = activity.getLayoutInflater();
        PackageManager pm = activity.getPackageManager();
        int column = (activity.getResources().getDisplayMetrics().widthPixels
                - content.getPaddingLeft() - content.getPaddingRight()) / COLUMNS;
        for (String pkg : pkgs) {
            View item = inflater.inflate(R.layout.tile, grid, false);
            View card = item.findViewById(R.id.card);
            TextView name = item.findViewById(R.id.name);
            fitTile(item, card, name, column);
            String label = labels.get(pkg);
            Tiles.bind(card, pkg, label, pm, 16 * density);
            card.setForeground(activity.getDrawable(R.drawable.tile_rim));
            name.setText(label);
            card.setOnClickListener(v -> Tiles.launch(activity, pkg));
            card.setOnKeyListener((v, keyCode, event) ->
                    event.getAction() == KeyEvent.ACTION_DOWN && navigate(grid.indexOfChild(item), keyCode));
            card.setOnFocusChangeListener((v, focused) -> {
                lift(v, focused, TILE_SCALE);
                name.animate().cancel();
                name.animate().alpha(focused ? 1f : 0f)
                        .translationY((focused ? 4f : -6f) * density)
                        .setDuration(220).start();
                if (!focused) return;
                onAppFocus(v, pkg);
                Tiles.reveal(scroll, content, item);
            });
            grid.addView(item);
        }
        // Tiles rise into place one after another (CleanYourTV's tile_enter).
        grid.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(activity, R.anim.tile_enter), 0.06f));
        grid.scheduleLayoutAnimation();
    }

    /** Gives a tile.xml tile the board's column width and gaps, so the grid lines up under the cells. */
    private void fitTile(View item, View card, View name, int column) {
        int gap = Math.round(8 * density);
        int width = column - 2 * gap;
        int height = Math.round(width * 9 / 16f);
        ((ViewGroup.MarginLayoutParams) item.getLayoutParams()).setMargins(gap, 0, gap, 0);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        lp.width = width;
        lp.height = height;
        lp = (ViewGroup.MarginLayoutParams) name.getLayoutParams();
        lp.width = width;
        lp.topMargin = height + Math.round(7 * density);
    }

    private View cardAt(int i) {
        return i >= 0 && i < grid.getChildCount() ? grid.getChildAt(i).findViewById(R.id.card) : null;
    }

    private boolean isInGrid(View card) {
        return card.getParent() instanceof View && card.getParent().getParent() == grid;
    }

    /** Down from the board's bottom row: the grid tile in the same column (or the last one). */
    private boolean focusGrid(int col) {
        View target = cardAt(Math.min(col, grid.getChildCount() - 1));
        if (target != null) target.requestFocus();
        return true;   // nothing below: swallow so focus never falls out
    }

    /** Deterministic D-pad in the grid: one row at a time, no wrapping, up from the top row to the board. */
    private boolean navigate(int pos, int keyCode) {
        int dir = GridNavigation.direction(keyCode);
        if (dir < 0) return false;
        int next = GridNavigation.next(pos, grid.getChildCount(), COLUMNS, dir);
        View target = next >= 0 ? cardAt(next) : next == GridNavigation.UP_OUT ? bottomRow[pos % COLUMNS] : null;
        if (target != null) target.requestFocus();
        return true;   // STAY, or down from the last row: swallow
    }

    // ---- focus look ----

    /** A focused cell or tile grows a little and rises above its neighbours; the rim drawable does the rest. */
    private void lift(View v, boolean focused, float scale) {
        v.animate().cancel();
        float s = focused ? scale : 1f;
        v.animate().scaleX(s).scaleY(s).translationZ(focused ? 6 * density : 0f)
                .setDuration(focused ? 260 : 180)
                .setInterpolator(new OvershootInterpolator(focused ? 1.4f : 0f))
                .withLayer().start();
    }

    private void onAppFocus(View card, String pkg) {
        if (pkg == null) return;
        lastFocused = pkg;
        if (!isInGrid(card)) scroll.smoothScrollTo(0, 0);
        Integer color = colors.get(pkg);
        if (color == null) {
            color = Glow.vividColor(((ImageView) card.findViewById(R.id.banner)).getDrawable());
            colors.put(pkg, color);
        }
        Glow.fadeTo(ambient, color);
    }
}
