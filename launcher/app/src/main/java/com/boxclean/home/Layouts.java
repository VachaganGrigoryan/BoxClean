package com.boxclean.home;

import android.content.Context;
import java.util.List;

/** The home layouts the picker offers, in its order, and the one the user chose. */
final class Layouts {
    static final String BENTO = "bento", RIBBON = "ribbon", SPOTLIGHT = "spotlight", EDITORIAL = "editorial",
            RAIL = "rail", CLASSIC = "classic";
    static final List<String> IDS = List.of(BENTO, RIBBON, SPOTLIGHT, EDITORIAL, RAIL, CLASSIC);

    private Layouts() {}

    /** A known layout id, or Bento for a missing or retired one. */
    static String byId(String id) {
        return id != null && IDS.contains(id) ? id : BENTO;   // List.of().contains(null) throws
    }

    static int nameRes(String id) {
        switch (id) {
            case CLASSIC: return R.string.layout_classic;
            case RIBBON: return R.string.layout_ribbon;
            case SPOTLIGHT: return R.string.layout_spotlight;
            case EDITORIAL: return R.string.layout_editorial;
            case RAIL: return R.string.layout_rail;
            default: return R.string.layout_bento;
        }
    }

    static int previewRes(String id) {
        switch (id) {
            case CLASSIC: return R.drawable.preview_classic;
            case RIBBON: return R.drawable.preview_ribbon;
            case SPOTLIGHT: return R.drawable.preview_spotlight;
            case EDITORIAL: return R.drawable.preview_editorial;
            case RAIL: return R.drawable.preview_rail;
            default: return R.drawable.preview_bento;
        }
    }

    static String saved(Context context) {
        return byId(context.getSharedPreferences("home", Context.MODE_PRIVATE).getString("layout", null));
    }

    static void save(Context context, String id) {
        context.getSharedPreferences("home", Context.MODE_PRIVATE).edit().putString("layout", id).apply();
    }
}
