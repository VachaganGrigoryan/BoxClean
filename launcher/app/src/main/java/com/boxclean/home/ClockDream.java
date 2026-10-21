package com.boxclean.home;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import java.util.Random;

/**
 * Screensaver: a thin clock and date that drift to a new spot every minute over a soft glow, so nothing burns
 * in. Adapted from CleanYourTV's ClockDream/ClockScene (MIT, see NOTICE), without the 8 s colour fade: a
 * full-screen GradientDrawable is CPU-drawn on this firmware, so the glow changes colour only while hidden.
 */
public class ClockDream extends DreamService {
    private static final long MOVE_MS = 60_000L;
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final GradientDrawable glow = new GradientDrawable();
    private View content;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            content.animate().alpha(0f).setDuration(900).withEndAction(() -> {
                place();
                content.animate().alpha(1f).setDuration(1200).start();
            }).start();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the box back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        content = findViewById(R.id.dreamContent);
        View ambient = findViewById(R.id.dreamAmbient);
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> glow.setGradientRadius((r - l) * 0.55f));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        content.setAlpha(0f);
        content.post(() -> {
            place();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.postDelayed(tick, MOVE_MS);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    /** A random spot that keeps the whole clock on screen, with a new glow colour centred behind it. */
    private void place() {
        View parent = (View) content.getParent();
        float x = Math.max(0, parent.getWidth() - content.getWidth()) * (0.1f + 0.8f * random.nextFloat());
        float y = Math.max(0, parent.getHeight() - content.getHeight()) * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
        int c = PALETTE[random.nextInt(PALETTE.length)] & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, c});
    }
}
