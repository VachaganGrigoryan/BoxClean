package com.boxclean.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Side rail home: a slim column of round app icons on the left edge, the rest of the screen a big clock and
 * the focused app's name and banner over its colour glow.
 */
final class RailHome extends BaseHome {
    private static final float SCALE = 1.12f;

    private final ScrollView scroll;
    private final ViewGroup rail;
    private final View ambient;
    private final View card;
    private final TextView greeting;
    private final TextView name;
    private final View[] buttons;

    RailHome(Activity activity) {
        super(activity, R.layout.activity_rail);
        scroll = find(R.id.scroll);
        rail = find(R.id.rail);
        ambient = find(R.id.ambient);
        card = find(R.id.card);
        greeting = find(R.id.greeting);
        name = find(R.id.name);
        buttons = new View[]{button(R.drawable.ic_wifi, R.string.wifi),
                button(R.drawable.ic_settings, R.string.settings), button(R.drawable.ic_layout, R.string.layout)};
        utilities(buttons[0], buttons[1], buttons[2]);
    }

    @Override
    public void resume() {
        greeting.setText(greetingRes());
        if (loadApps()) fill();
        View target = lastFocused == null ? null : rail.findViewWithTag(lastFocused);
        if (target == null && showApps && !rest.isEmpty()) target = rail.findViewWithTag(rest.get(0));
        showApps = false;
        if (target == null) target = first();
        if (target == first()) scroll.scrollTo(0, 0);
        rail.post(target::requestFocus);
    }

    @Override
    public void back() {
        scroll.smoothScrollTo(0, 0);
        first().requestFocus();
    }

    private View first() {
        return dock.isEmpty() && rest.isEmpty() ? buttons[0] : rail.getChildAt(0);
    }

    private void fill() {
        rail.removeAllViews();
        for (String pkg : dock) addApp(pkg);
        if (!dock.isEmpty() && !rest.isEmpty()) divider();
        for (String pkg : rest) addApp(pkg);
        divider();
        for (View b : buttons) rail.addView(b);
        animateIn(rail);
    }

    private void addApp(String pkg) {
        View item = item();
        ImageView icon = item.findViewById(R.id.icon);
        // The icon fills the circle, clipped round by the oval background's outline, with a ring on focus.
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        size(icon, 64, 64);
        item.setClipToOutline(true);
        item.setForeground(activity.getDrawable(R.drawable.rail_rim));
        try {
            icon.setImageDrawable(activity.getPackageManager().getApplicationIcon(pkg));
        } catch (PackageManager.NameNotFoundException ignored) {
            // Uninstalled between query and bind: the next resume drops it.
        }
        String label = labels.get(pkg);
        item.setTag(pkg);
        item.setContentDescription(label);
        item.setOnClickListener(v -> Tiles.launch(activity, pkg));
        item.setOnFocusChangeListener((v, focused) -> {
            lift(v, focused, SCALE);
            if (!focused) return;
            lastFocused = pkg;
            card.setVisibility(View.VISIBLE);
            bindCard(card, pkg, 16);
            swapText(name, label);
            Glow.fadeTo(ambient, colorOf(pkg, card));
        });
        rail.addView(item);
    }

    /** A Wi-Fi / Settings / Layout circle at the rail's foot, its icon dark on the white focus. */
    private View button(int icon, int label) {
        View item = item();
        ImageView image = item.findViewById(R.id.icon);
        image.setImageResource(icon);
        image.setImageTintList(activity.getColorStateList(R.color.pill_text));
        size(image, 26, 26);
        item.setContentDescription(activity.getString(label));
        item.setOnFocusChangeListener((v, focused) -> {
            lift(v, focused, SCALE);
            if (!focused) return;
            card.setVisibility(View.INVISIBLE);
            swapText(name, activity.getString(label));
        });
        return item;
    }

    private View item() {
        View item = activity.getLayoutInflater().inflate(R.layout.rail_item, rail, false);
        // One column: left and right go nowhere.
        item.setOnKeyListener((v, keyCode, event) ->
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
        return item;
    }

    private void divider() {
        rail.addView(activity.getLayoutInflater().inflate(R.layout.rail_divider, rail, false));
    }
}
