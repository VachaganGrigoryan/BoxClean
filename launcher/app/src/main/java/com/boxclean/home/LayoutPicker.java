package com.boxclean.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/** The Layout button's panel: one card per home layout. OK switches to it, Back closes the panel. */
final class LayoutPicker {
    private LayoutPicker() {}

    @SuppressLint("InflateParams")   // a dialog's content view has no parent to inflate against
    static void show(Activity activity) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        View root = activity.getLayoutInflater().inflate(R.layout.layout_picker, null);
        ViewGroup cards = root.findViewById(R.id.cards);
        String current = Layouts.saved(activity);
        View focus = null;
        for (String id : Layouts.IDS) {
            View card = activity.getLayoutInflater().inflate(R.layout.picker_card, cards, false);
            String name = activity.getString(Layouts.nameRes(id));
            ((ImageView) card.findViewById(R.id.preview)).setImageResource(Layouts.previewRes(id));
            ((TextView) card.findViewById(R.id.name)).setText(name);
            card.findViewById(R.id.current).setVisibility(id.equals(current) ? View.VISIBLE : View.INVISIBLE);
            card.setContentDescription(name);
            card.setOnFocusChangeListener((v, focused) -> {
                float s = focused ? 1.06f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(200).start();
            });
            card.setOnClickListener(v -> {
                dialog.dismiss();
                if (id.equals(current)) return;
                Layouts.save(activity, id);
                activity.recreate();
            });
            cards.addView(card);
            if (id.equals(current)) focus = card;
        }
        dialog.setContentView(root);
        dialog.show();
        // After the window takes focus, which would otherwise land on the first card.
        if (focus != null) root.post(focus::requestFocus);
    }
}
