package com.boxclean.home;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

/**
 * The ambient glow: an app's colour, sampled from its banner, and the tinted backdrop that shows it.
 * Ported from CleanYourTV's AppAdapter and setAmbient (MIT, see NOTICE).
 */
final class Glow {
    private Glow() {}

    /** Crossfades the full-screen ambient view to a colour, through a dim step; a no-op if it already shows it. */
    static void fadeTo(View ambient, int color) {
        if (ambient.getBackground() != null && Integer.valueOf(color).equals(ambient.getTag())) return;
        ambient.animate().cancel();
        if (ambient.getBackground() == null) {
            tint(ambient, color);
            ambient.animate().alpha(1f).setDuration(600).start();
        } else {
            ambient.animate().alpha(0.35f).setDuration(200).withEndAction(() -> {
                tint(ambient, color);
                ambient.animate().alpha(1f).setDuration(500).start();
            }).start();
        }
    }

    /**
     * Shows a banner blurred across a full-screen ImageView, crossfading through a dim step. API 30 has no
     * RenderEffect, so it is drawn 32x18, box-blurred, and upscaled smoothly to 192x108 for the GPU to stretch;
     * a GPU stretch of the tiny bitmap alone shows its pixels.
     */
    static void backdrop(ImageView view, Drawable banner) {
        if (banner == null) return;
        int w = 32, h = 18;
        Bitmap small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Drawable d = copy(banner);
        d.setBounds(0, 0, w, h);
        d.draw(new Canvas(small));
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) px = boxBlur(px, w, h, 2);   // three box passes look Gaussian
        small.setPixels(px, 0, w, 0, 0, w, h);
        Bitmap big = Bitmap.createScaledBitmap(small, w * 6, h * 6, true);
        small.recycle();
        BitmapDrawable blurred = new BitmapDrawable(view.getResources(), big);
        blurred.setFilterBitmap(true);
        view.animate().cancel();
        view.animate().alpha(0f).setDuration(view.getDrawable() == null ? 0 : 160).withEndAction(() -> {
            view.setImageDrawable(blurred);
            view.animate().alpha(1f).setDuration(420).start();
        }).start();
    }

    /** One horizontal then one vertical box-blur pass of radius r over ARGB pixels, clamped at the edges. */
    static int[] boxBlur(int[] px, int w, int h, int r) {
        int[] tmp = new int[px.length], out = new int[px.length];
        for (int pass = 0; pass < 2; pass++) {
            int[] src = pass == 0 ? px : tmp, dst = pass == 0 ? tmp : out;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int a = 0, red = 0, green = 0, blue = 0;
                    for (int k = -r; k <= r; k++) {
                        int sx = pass == 0 ? Math.min(w - 1, Math.max(0, x + k)) : x;
                        int sy = pass == 0 ? y : Math.min(h - 1, Math.max(0, y + k));
                        int p = src[sy * w + sx];
                        a += p >>> 24;
                        red += (p >> 16) & 0xFF;
                        green += (p >> 8) & 0xFF;
                        blue += p & 0xFF;
                    }
                    int n = 2 * r + 1;
                    dst[y * w + x] = (a / n) << 24 | (red / n) << 16 | (green / n) << 8 | (blue / n);
                }
            }
        }
        return out;
    }

    /** A private copy: changing the bounds of the drawable an ImageView shows would blank that view. */
    private static Drawable copy(Drawable live) {
        return live.getConstantState() != null ? live.getConstantState().newDrawable().mutate() : live;
    }

    /**
     * A small white radial-gradient bitmap stretched over the screen and tinted: one GPU texture, where a
     * full-screen GradientDrawable is redrawn on the CPU (CleanYourTV's setAmbient, MIT, see NOTICE).
     */
    private static void tint(View ambient, int color) {
        ambient.setTag(color);
        BitmapDrawable glow = (BitmapDrawable) ambient.getBackground();
        if (glow == null) {
            int w = 480, h = 270;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(w * 0.32f, h * 0.42f, w * 0.62f,
                    new int[]{0x5CFFFFFF, 0x26FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP));
            new Canvas(bmp).drawRect(0, 0, w, h, paint);
            glow = new BitmapDrawable(ambient.getResources(), bmp);
            glow.setGravity(Gravity.FILL);
            ambient.setBackground(glow);
        }
        glow.setTint(0xFF000000 | color);   // SRC_IN keeps the mask's alpha, takes the colour
    }

    static int vividColor(Drawable live) {
        if (live == null) return 0xFF444444;
        Drawable d = copy(live);
        Rect saved = new Rect(d.getBounds());
        int n = 12;
        Bitmap bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, n, n);
        d.draw(new Canvas(bmp));
        d.setBounds(saved);
        int[] px = new int[n * n];
        bmp.getPixels(px, 0, n, 0, 0, n, n);
        bmp.recycle();
        float[] hsv = new float[3];
        int best = 0xFF444444;
        float bestScore = -1f;
        long r = 0, g = 0, b = 0;
        for (int p : px) {
            int pr = (p >> 16) & 0xFF, pg = (p >> 8) & 0xFF, pb = p & 0xFF;
            r += pr;
            g += pg;
            b += pb;
            int max = Math.max(pr, Math.max(pg, pb)), min = Math.min(pr, Math.min(pg, pb));
            float sat = max == 0 ? 0f : (max - min) / (float) max;
            float score = sat * Math.min(1f, max / 255f * 1.4f);   // saturated and not too dark
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        if (bestScore < 0.25f) {   // nearly monochrome banner (e.g. white): use its average, lifted
            Color.colorToHSV(Color.rgb((int) (r / (n * n)), (int) (g / (n * n)), (int) (b / (n * n))), hsv);
            hsv[2] = Math.max(hsv[2], 0.55f);
            return Color.HSVToColor(hsv);
        }
        Color.colorToHSV(best, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.1f);
        hsv[2] = Math.max(0.5f, hsv[2]);
        return Color.HSVToColor(hsv);
    }
}
