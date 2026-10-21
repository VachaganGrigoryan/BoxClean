package com.boxclean.home;

import android.app.Activity;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/**
 * Apple TV style home. Look and motion follow CleanYourTV's launcher (MIT, see NOTICE): Outfit type, a big
 * name over a low dock, a colour glow sampled from the focused app, and one glass highlight that glides
 * between tiles.
 */
final class ClassicHome extends BaseHome {
    private static final int COLUMNS = 4;
    private static final float FOCUS_SCALE = 1.10f;

    private final ScrollView scroll;
    private final ViewGroup content;
    private final ViewGroup dockRow;
    private final ViewGroup grid;
    private final View spacer;
    private final View ambient;
    private final TextView hero;
    private final TextView greeting;
    private final GlassView glass;
    private View glassOn;

    ClassicHome(Activity activity) {
        super(activity, R.layout.activity_main);
        scroll = find(R.id.scroll);
        content = find(R.id.content);
        dockRow = find(R.id.dock);
        grid = find(R.id.grid);
        spacer = find(R.id.spacer);
        ambient = find(R.id.ambient);
        hero = find(R.id.hero);
        greeting = find(R.id.greeting);
        glass = find(R.id.glass);

        // The glass is the focused card at FOCUS_SCALE plus 6dp of glow on each side.
        ViewGroup.LayoutParams lp = glass.getLayoutParams();
        lp.width = Math.round((180 * FOCUS_SCALE + 12) * density);
        lp.height = Math.round((101 * FOCUS_SCALE + 12) * density);
        glass.setLayoutParams(lp);
        glass.setRadius(16 * FOCUS_SCALE * density);

        View wifi = find(R.id.wifi), settings = find(R.id.settings), layout = find(R.id.layout);
        utilities(wifi, settings, layout);
        for (View pill : new View[]{wifi, settings, layout}) {
            pill.setOnFocusChangeListener((v, focused) -> {
                if (!focused) return;
                glassOn = null;   // the pill has its own focused look
                glass.animate().alpha(0f).setDuration(150).start();
            });
        }
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            sizeSpacer();
            if (glassOn != null) placeGlass(glassOn, false);
        });
    }

    @Override
    public void resume() {
        greeting.setText(greetingRes());
        if (loadApps()) {
            fill(dockRow, dock, true);
            fill(grid, rest, false);
            dockRow.setVisibility(dock.isEmpty() ? View.GONE : View.VISIBLE);
        }
        View target = lastFocused == null ? null : content.findViewWithTag(lastFocused);
        if (target == null && showApps) target = cardAt(grid, 0);
        if (target == null) target = first();
        showApps = false;
        if (target != null) {
            View t = target;
            if (t.getParent().getParent() == dockRow) scroll.scrollTo(0, 0);
            content.post(t::requestFocus);   // after layout, so the glass lands on the tile's real position
        }
    }

    @Override
    public void back() {
        View first = first();
        if (first == null) return;
        scroll.smoothScrollTo(0, 0);
        first.requestFocus();
    }

    private View first() {
        return dockRow.getChildCount() > 0 ? cardAt(dockRow, 0) : cardAt(grid, 0);
    }

    private void fill(ViewGroup parent, List<String> pkgs, boolean inDock) {
        parent.removeAllViews();
        for (String pkg : pkgs) {
            View item = tile(parent, pkg, 16);
            View card = item.findViewById(R.id.card);
            TextView name = item.findViewById(R.id.name);
            if (inDock) name.setVisibility(View.GONE);   // the dock's name is the big hero title instead
            card.setOnKeyListener((v, keyCode, event) ->
                    event.getAction() == KeyEvent.ACTION_DOWN && navigate(inDock, parent.indexOfChild(item), keyCode));
            card.setOnFocusChangeListener((v, focused) -> onTileFocus(v, name, pkg, inDock, focused));
            parent.addView(item);
        }
        animateIn(parent);
    }

    private static View cardAt(ViewGroup parent, int i) {
        return i >= 0 && i < parent.getChildCount() ? parent.getChildAt(i).findViewById(R.id.card) : null;
    }

    // ---- focus, navigation, glass ----

    /** Deterministic D-pad: one row at a time, no wrapping, dock and grid columns line up. */
    private boolean navigate(boolean inDock, int pos, int keyCode) {
        int dir = GridNavigation.direction(keyCode);
        if (dir < 0) return false;
        ViewGroup here = inDock ? dockRow : grid;
        int next = GridNavigation.next(pos, here.getChildCount(), COLUMNS, dir);
        View target;
        if (next >= 0) {
            target = cardAt(here, next);
        } else if (next == GridNavigation.UP_OUT) {
            if (inDock) return false;   // up from the dock: the framework finds the top-bar pills
            target = cardAt(dockRow, Math.min(pos % COLUMNS, dockRow.getChildCount() - 1));
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
            showHero(pkg, card);
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

    /** Grows the spacer so the dock's bottom sits 56dp above the screen's bottom on the first screen. */
    private void sizeSpacer() {
        if (dockRow.getVisibility() != View.VISIBLE || scroll.getHeight() == 0) return;
        View column = (View) dockRow.getParent();
        int dockBottom = column.getTop() + dockRow.getBottom();
        int delta = scroll.getHeight() - Math.round(56 * density) - dockBottom;
        ViewGroup.LayoutParams lp = spacer.getLayoutParams();
        int height = Math.max(0, lp.height + delta);
        if (Math.abs(height - lp.height) > 1) {
            lp.height = height;
            spacer.setLayoutParams(lp);
        }
    }

    // ---- hero title and ambient glow ----

    private void showHero(String pkg, View card) {
        swapText(hero, labels.get(pkg));
        Glow.fadeTo(ambient, colorOf(pkg, card));
    }
}
