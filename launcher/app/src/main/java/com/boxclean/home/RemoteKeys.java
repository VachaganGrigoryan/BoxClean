package com.boxclean.home;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * app_keys entries ("KEYCODE_BUTTON_7:ru.kinopoisk.tv" or "194:ru.kinopoisk.tv") for KeyService.
 * Adapted from CleanYourTV's RemoteKeys (MIT, see NOTICE).
 */
final class RemoteKeys {
    private RemoteKeys() {}

    /** keycode -> package. Bad entries are skipped; for a keycode listed twice the last entry wins. */
    static Map<Integer, String> parseAppKeys(Iterable<String> entries, ToIntFunction<String> names) {
        Map<Integer, String> keys = new HashMap<>();
        for (String entry : entries) {
            if (entry == null) continue;
            String[] parts = entry.split(":", 2);
            if (parts.length != 2 || parts[1].trim().isEmpty()) continue;
            String key = parts[0].trim();
            int code;
            if (key.matches("[0-9]{1,6}")) code = Integer.parseInt(key);
            else if (key.matches("KEYCODE_[A-Z0-9_]+")) code = names.applyAsInt(key);
            else continue;
            if (code > 0) keys.put(code, parts[1].trim());
        }
        return keys;
    }

    /** Opens an app from a remote button: its TV entry if it has one, else its phone entry. */
    static void launch(Context ctx, String pkg) {
        Intent open = ctx.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (open == null) open = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (open == null) return;
        try {
            ctx.startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException ignored) {
            // App disabled since the key was mapped: the button does nothing, as on stock.
        }
    }
}
