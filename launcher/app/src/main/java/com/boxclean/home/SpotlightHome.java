package com.boxclean.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Cinema home: one app at a time fills the screen (blurred backdrop, its banner, a huge name). Left and right
 * flip through the dock apps, OK opens one, Down raises a strip of every app.
 */
final class SpotlightHome extends BaseHome {
    private static final int PAGES = 4;

    private final View stage;
    private final View card;
    private final HorizontalScrollView strip;
    private final ViewGroup stripRow;
    private final ViewGroup dots;
    private final ImageView backdrop;
    private final TextView name;
    private List<String> pages = List.of();
    private int page;

    SpotlightHome(Activity activity) {
        super(activity, R.layout.activity_spotlight);
        stage = find(R.id.stage);
        card = find(R.id.card);
        strip = find(R.id.strip);
        stripRow = find(R.id.stripRow);
        dots = find(R.id.dots);
        backdrop = find(R.id.backdrop);
        name = find(R.id.name);
        utilities(find(R.id.wifi), find(R.id.settings), find(R.id.layout));
        stage.setOnClickListener(v -> {
            if (!pages.isEmpty()) Tiles.launch(activity, pages.get(page));
        });
        stage.setOnKeyListener((v, keyCode, event) -> event.getAction() == KeyEvent.ACTION_DOWN && stageKey(keyCode));
    }

    @Override
    public void resume() {
        if (loadApps()) fill();
        page = Math.max(0, pages.indexOf(lastFocused));
        boolean apps = showApps && !rest.isEmpty();
        showApps = false;
        closeStrip(false);
        show(page);
        if (apps) stage.post(() -> openStrip(dock.size()));
        else stage.post(stage::requestFocus);
    }

    @Override
    public void back() {
        closeStrip(false);
        show(0);
        stage.requestFocus();
    }

    private void fill() {
        pages = Apps.board(dock, rest, PAGES);
        dots.removeAllViews();
        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(activity);
            dot.setBackgroundResource(R.drawable.dot);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    Math.round(8 * density), Math.round(8 * density));
            lp.rightMargin = Math.round(10 * density);
            dots.addView(dot, lp);
        }
        stripRow.removeAllViews();
        List<String> all = new ArrayList<>(dock);
        all.addAll(rest);
        for (String pkg : all) {
            View item = tile(stripRow, pkg, 16);
            View tile = item.findViewById(R.id.card);
            item.findViewById(R.id.name).setVisibility(View.GONE);   // the big name shows it instead
            size(tile, 176, 99);
            tile.setForeground(activity.getDrawable(R.drawable.tile_rim));
            tile.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) closeStrip(true);
                    return true;
                }
                return keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
            });
            tile.setOnFocusChangeListener((v, focused) -> {
                lift(v, focused, 1.08f);
                if (focused) preview(pkg);
            });
            stripRow.addView(item);
        }
    }

    /** Left/right flip the pages (no wrap), Down opens the strip; Up goes to the pills above. */
    private boolean stageKey(int keyCode) {
        int dir = GridNavigation.direction(keyCode);
        if (dir == GridNavigation.LEFT || dir == GridNavigation.RIGHT) {
            int next = GridNavigation.next(page, pages.size(), Math.max(1, pages.size()), dir);
            if (next >= 0) show(next);
            return true;
        }
        if (dir == GridNavigation.DOWN) {
            openStrip(page);
            return true;
        }
        return false;
    }

    private void show(int i) {
        if (pages.isEmpty()) return;
        page = i;
        lastFocused = pages.get(i);
        for (int d = 0; d < dots.getChildCount(); d++) dots.getChildAt(d).setAlpha(d == i ? 1f : 0.35f);
        preview(pages.get(i));
    }

    /** Puts an app on the stage: its banner, name and blurred backdrop. */
    private void preview(String pkg) {
        bindCard(card, pkg, 20);
        card.setAlpha(0.6f);
        card.animate().alpha(1f).setDuration(240).start();
        swapText(name, labels.get(pkg));
        Glow.backdrop(backdrop, ((ImageView) card.findViewById(R.id.banner)).getDrawable());
    }

    private void openStrip(int index) {
        if (stripRow.getChildCount() == 0) return;
        strip.setVisibility(View.VISIBLE);
        strip.setTranslationY(24 * density);
        strip.animate().alpha(1f).translationY(0f).setDuration(260).start();
        View target = stripRow.getChildAt(Math.min(index, stripRow.getChildCount() - 1)).findViewById(R.id.card);
        target.requestFocus();
    }

    /** Hides the strip; when animated (Up from it) the stage takes focus back with its own page. */
    private void closeStrip(boolean animate) {
        if (!animate) {
            strip.setAlpha(0f);
            strip.setVisibility(View.INVISIBLE);
            return;
        }
        stage.requestFocus();
        show(page);
        strip.animate().alpha(0f).translationY(24 * density).setDuration(200)
                .withEndAction(() -> strip.setVisibility(View.INVISIBLE)).start();
    }
}
