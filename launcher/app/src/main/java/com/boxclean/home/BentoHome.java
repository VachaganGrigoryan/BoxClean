package com.boxclean.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Bento home: a board of rounded cells (clock, four big apps, Wi-Fi, Settings, Layout) fills the first screen,
 * every other app sits in a grid below it.
 */
final class BentoHome extends BaseHome {
    private static final int COLUMNS = 4;
    private static final int[] SLOTS = {R.id.app1, R.id.app2, R.id.app3, R.id.app4};
    private static final float CELL_SCALE = 1.04f;
    private static final float TILE_SCALE = 1.08f;
    /** Room left under the board on the first screen, so the "All apps" title shows there. */
    private static final int PEEK_DP = 60;

    private final ScrollView scroll;
    private final ViewGroup content;
    private final View board;
    private final ViewGroup grid;
    private final View ambient;
    private final TextView greeting;
    /** The board's bottom row, left to right; it lines up with the grid's 4 columns. */
    private final View[] bottomRow;

    BentoHome(Activity activity) {
        super(activity, R.layout.activity_bento);
        scroll = find(R.id.scroll);
        content = find(R.id.content);
        board = find(R.id.board);
        grid = find(R.id.grid);
        ambient = find(R.id.ambient);
        greeting = find(R.id.greeting);
        bottomRow = new View[]{find(R.id.app4), find(R.id.wifi), find(R.id.settings), find(R.id.layout)};

        for (int id : SLOTS) {
            View cell = find(id);
            cell.setOnFocusChangeListener((v, focused) -> {
                lift(v, focused, CELL_SCALE);
                if (focused) onAppFocus(v, (String) v.getTag());
            });
        }
        utilities(bottomRow[1], bottomRow[2], bottomRow[3]);
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

    @Override
    public void resume() {
        greeting.setText(greetingRes());
        if (loadApps()) {
            List<String> onBoard = Apps.board(dock, rest, SLOTS.length);
            List<String> inGrid = new ArrayList<>(rest);
            inGrid.removeAll(onBoard);
            bindBoard(onBoard);
            fillGrid(inGrid);
        }
        View target = lastFocused == null ? null : content.findViewWithTag(lastFocused);
        if (target == null && showApps) target = cardAt(0);
        showApps = false;
        if (target == null) target = firstCell();
        if (!isInGrid(target)) scroll.scrollTo(0, 0);
        content.post(target::requestFocus);   // after layout, so scrolling sees the tile's real position
    }

    @Override
    public void back() {
        scroll.smoothScrollTo(0, 0);
        firstCell().requestFocus();
    }

    private View firstCell() {
        View app1 = find(R.id.app1);
        return app1.getVisibility() == View.VISIBLE ? app1 : find(R.id.wifi);
    }

    // ---- board ----

    private void bindBoard(List<String> pkgs) {
        PackageManager pm = activity.getPackageManager();
        for (int i = 0; i < SLOTS.length; i++) {
            View cell = find(SLOTS[i]);
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
        int column = (activity.getResources().getDisplayMetrics().widthPixels
                - content.getPaddingLeft() - content.getPaddingRight()) / COLUMNS;
        for (String pkg : pkgs) {
            View item = tile(grid, pkg, 16);
            View card = item.findViewById(R.id.card);
            TextView name = item.findViewById(R.id.name);
            fitTile(item, card, name, column);
            card.setForeground(activity.getDrawable(R.drawable.tile_rim));
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
        animateIn(grid);
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

    private void onAppFocus(View card, String pkg) {
        if (pkg == null) return;
        lastFocused = pkg;
        if (!isInGrid(card)) scroll.smoothScrollTo(0, 0);
        Glow.fadeTo(ambient, colorOf(pkg, card));
    }
}
