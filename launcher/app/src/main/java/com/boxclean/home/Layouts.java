package com.boxclean.home;

import android.content.Context;
import java.util.List;

/** The home layouts the picker offers, in its order, and the one the user chose. */
final class Layouts {
    static final String BENTO = "bento", CLASSIC = "classic", RIBBON = "ribbon";
    static final List<String> IDS = List.of(BENTO, RIBBON, CLASSIC);

    private Layouts() {}

    /** A known layout id, or Bento for a missing or retired one. */
    static String byId(String id) {
        return id != null && IDS.contains(id) ? id : BENTO;   // List.of().contains(null) throws
    }

    static int nameRes(String id) {
        switch (id) {
            case CLASSIC: return R.string.layout_classic;
            case RIBBON: return R.string.layout_ribbon;
            default: return R.string.layout_bento;
        }
    }

    static int previewRes(String id) {
        switch (id) {
            case CLASSIC: return R.drawable.preview_classic;
            case RIBBON: return R.drawable.preview_ribbon;
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
