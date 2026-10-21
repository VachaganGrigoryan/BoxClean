package com.boxclean.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * LG webOS style home: the focused app's banner, blurred, fills the screen, and one ribbon of cards runs along
 * the bottom (dock apps, then the rest, then Wi-Fi, Settings and Layout). The focused card lifts out of it.
 */
final class RibbonHome extends BaseHome {
    private static final int CARD_W = 176, CARD_H = 99, GROUP_GAP = 28;
    private static final float LIFT_DP = 18, SCALE = 1.08f;

    private final HorizontalScrollView scroll;
    private final ViewGroup ribbon;
    private final ImageView backdrop;
    private final TextView hero;
    private final View[] buttons;

    RibbonHome(Activity activity) {
        super(activity, R.layout.activity_ribbon);
        scroll = find(R.id.scroll);
        ribbon = find(R.id.ribbon);
        backdrop = find(R.id.backdrop);
        hero = find(R.id.hero);
        buttons = new View[]{button(R.drawable.ic_wifi, R.string.wifi),
                button(R.drawable.ic_settings, R.string.settings), button(R.drawable.ic_layout, R.string.layout)};
        utilities(buttons[0], buttons[1], buttons[2]);
    }

    @Override
    public void resume() {
        if (loadApps()) fill();
        View target = lastFocused == null ? null : ribbon.findViewWithTag(lastFocused);
        if (target == null && showApps) target = cardAt(dock.size());
        showApps = false;
        if (target == null) target = cardAt(0);
        if (target == null) target = buttons[0];
        if (target == cardAt(0)) scroll.scrollTo(0, 0);
        ribbon.post(target::requestFocus);
    }

    @Override
    public void back() {
        View first = cardAt(0);
        scroll.smoothScrollTo(0, 0);
        (first != null ? first : buttons[0]).requestFocus();
    }

    private void fill() {
        ribbon.removeAllViews();
        int i = 0;
        for (List<String> group : List.of(dock, rest)) {
            for (String pkg : group) {
                View item = tile(ribbon, pkg, 16);
                View card = item.findViewById(R.id.card);
                item.findViewById(R.id.name).setVisibility(View.GONE);   // the hero title names it instead
                ViewGroup.LayoutParams lp = card.getLayoutParams();
                lp.width = Math.round(CARD_W * density);
                lp.height = Math.round(CARD_H * density);
                card.setForeground(activity.getDrawable(R.drawable.tile_rim));
                hold(card);
                card.setOnFocusChangeListener((v, focused) -> {
                    rise(v, focused);
                    if (!focused) return;
                    lastFocused = pkg;
                    swapText(hero, labels.get(pkg));
                    Glow.backdrop(backdrop, ((ImageView) v.findViewById(R.id.banner)).getDrawable());
                });
                gapBefore(item, i == dock.size() && i > 0);
                ribbon.addView(item);
                i++;
            }
        }
        for (int b = 0; b < buttons.length; b++) {
            gapBefore(buttons[b], b == 0 && i > 0);
            ribbon.addView(buttons[b]);
        }
        animateIn(ribbon);
    }

    /** A square Wi-Fi / Settings / Layout card at the ribbon's end, in Bento's cell look. */
    private View button(int icon, int label) {
        View b = activity.getLayoutInflater().inflate(R.layout.ribbon_button, ribbon, false);
        ((ImageView) b.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) b.findViewById(R.id.label)).setText(label);
        b.setContentDescription(activity.getString(label));
        hold(b);
        b.setOnFocusChangeListener((v, focused) -> {
            rise(v, focused);
            if (focused) swapText(hero, activity.getString(label));
        });
        return b;
    }

    /** The ribbon is one row: up and down go nowhere, left and right are the framework's. */
    private static void hold(View v) {
        v.setOnKeyListener((view, keyCode, event) ->
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN);
    }

    private void gapBefore(View item, boolean gap) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
        lp.leftMargin = Math.round(((gap ? GROUP_GAP : 0) + 8) * density);
        lp.rightMargin = Math.round(8 * density);
    }

    /** The focused card lifts out of the ribbon and grows; the rest sit back down. */
    private void rise(View v, boolean focused) {
        v.animate().cancel();
        float s = focused ? SCALE : 1f;
        v.animate().scaleX(s).scaleY(s).translationY(focused ? -LIFT_DP * density : 0f)
                .translationZ(focused ? 6 * density : 0f)
                .setDuration(focused ? 280 : 180)
                .setInterpolator(new OvershootInterpolator(focused ? 1.3f : 0f))
                .withLayer().start();
    }

    private View cardAt(int i) {
        if (i < 0 || i >= dock.size() + rest.size() || i >= ribbon.getChildCount()) return null;
        return ribbon.getChildAt(i).findViewById(R.id.card);
    }
}
