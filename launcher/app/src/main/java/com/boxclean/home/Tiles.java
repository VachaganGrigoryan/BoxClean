package com.boxclean.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

/** What both home layouts share: the installed TV apps, binding one into a card, launching, scrolling. */
final class Tiles {
    private Tiles() {}

    /** Package to label for every app with a leanback launcher entry. */
    static Map<String, String> tvApps(PackageManager pm) {
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Map<String, String> out = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            out.put(ri.activityInfo.packageName, ri.loadLabel(pm).toString());
        }
        return out;
    }

    /**
     * Shows the app's banner in the card's {@code banner} view, or its icon and label when it has none, and
     * rounds the card's corners.
     */
    static void bind(View card, String pkg, String label, PackageManager pm, float radius) {
        Drawable banner = null;
        try {
            banner = pm.getApplicationBanner(pkg);
            if (banner == null) {
                ImageView icon = card.findViewById(R.id.icon);
                TextView text = card.findViewById(R.id.label);
                icon.setImageDrawable(pm.getApplicationIcon(pkg));
                icon.setVisibility(View.VISIBLE);
                text.setText(label);
                text.setVisibility(View.VISIBLE);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Uninstalled between query and bind: the next resume drops it.
        }
        ((ImageView) card.findViewById(R.id.banner)).setImageDrawable(banner);
        card.setTag(pkg);
        card.setContentDescription(label);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);
    }

    static void launch(Activity activity, String pkg) {
        PackageManager pm = activity.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) start(activity, intent);
    }

    static void start(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Scrolls just enough to show a grid tile with its name, plus a little room (CleanYourTV's grid scroll). */
    static void reveal(ScrollView scroll, ViewGroup content, View item) {
        scroll.post(() -> {
            int room = Math.round(12 * item.getResources().getDisplayMetrics().density);
            Rect r = new Rect(0, 0, item.getWidth(), item.getHeight());
            content.offsetDescendantRectToMyCoords(item, r);
            int top = r.top - room, bottom = r.bottom + room;
            int y = scroll.getScrollY(), h = scroll.getHeight();
            if (bottom > y + h) scroll.smoothScrollTo(0, bottom - h);
            else if (top < y) scroll.smoothScrollTo(0, top);
        });
    }
}
