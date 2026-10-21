package com.boxclean.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Magazine contents page: the apps as a numbered list of big names on the left, the focused one's banner
 * framed on the right over its blurred backdrop.
 */
final class EditorialHome extends BaseHome {
    private final ScrollView scroll;
    private final ViewGroup list;
    private final View card;
    private final ImageView backdrop;
    /** The focusable rows in order, without the divider. */
    private final List<View> rows = new ArrayList<>();

    EditorialHome(Activity activity) {
        super(activity, R.layout.activity_editorial);
        scroll = find(R.id.scroll);
        list = find(R.id.list);
        card = find(R.id.card);
        backdrop = find(R.id.backdrop);
        utilities(find(R.id.wifi), find(R.id.settings), find(R.id.layout));
    }

    @Override
    public void resume() {
        if (loadApps()) fill();
        View target = lastFocused == null ? null : list.findViewWithTag(lastFocused);
        if (target == null && showApps && dock.size() < rows.size()) target = rows.get(dock.size());
        showApps = false;
        if (target == null && !rows.isEmpty()) target = rows.get(0);
        if (target == null) target = find(R.id.wifi);
        if (!rows.isEmpty() && target == rows.get(0)) scroll.scrollTo(0, 0);
        list.post(target::requestFocus);
    }

    @Override
    public void back() {
        if (rows.isEmpty()) return;
        scroll.smoothScrollTo(0, 0);
        rows.get(0).requestFocus();
    }

    private void fill() {
        list.removeAllViews();
        rows.clear();
        for (String pkg : dock) addRow(pkg);
        if (!dock.isEmpty() && !rest.isEmpty()) {
            list.addView(activity.getLayoutInflater().inflate(R.layout.editorial_divider, list, false));
        }
        for (String pkg : rest) addRow(pkg);
        animateIn(list);
    }

    private void addRow(String pkg) {
        View row = activity.getLayoutInflater().inflate(R.layout.editorial_row, list, false);
        String label = labels.get(pkg);
        ((TextView) row.findViewById(R.id.number)).setText(String.format(Locale.ROOT, "%02d", rows.size() + 1));
        ((TextView) row.findViewById(R.id.title)).setText(label);
        View bar = row.findViewById(R.id.bar);
        row.setTag(pkg);
        row.setContentDescription(label);
        row.setOnClickListener(v -> Tiles.launch(activity, pkg));
        // One column: left and right go nowhere.
        row.setOnKeyListener((v, keyCode, event) ->
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
        row.setOnFocusChangeListener((v, focused) -> {
            v.animate().alpha(focused ? 1f : 0.45f).translationX(focused ? 12 * density : 0f).setDuration(200).start();
            bar.animate().alpha(focused ? 1f : 0f).scaleY(focused ? 1f : 0.3f).setDuration(200).start();
            if (!focused) return;
            lastFocused = pkg;
            bindCard(card, pkg, 24);
            card.setAlpha(0.6f);
            card.animate().alpha(1f).setDuration(240).start();
            Glow.backdrop(backdrop, ((ImageView) card.findViewById(R.id.banner)).getDrawable());
        });
        list.addView(row);
        rows.add(row);
    }
}
