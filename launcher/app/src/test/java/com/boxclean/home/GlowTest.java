package com.boxclean.home;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import org.junit.Test;

public class GlowTest {
    @Test
    public void boxBlurKeepsAFlatImageFlat() {
        int[] flat = new int[12];
        Arrays.fill(flat, 0xFF336699);
        assertArrayEquals(flat, Glow.boxBlur(flat, 4, 3, 2));
    }

    @Test
    public void boxBlurSpreadsOneBrightPixelAcrossItsRadius() {
        // 5x5 black with a white centre: radius 2 reaches every pixel exactly once per axis, so 255 / 25 = 10.
        int[] px = new int[25];
        Arrays.fill(px, 0xFF000000);
        px[12] = 0xFFFFFFFF;
        int[] want = new int[25];
        Arrays.fill(want, 0xFF0A0A0A);
        assertArrayEquals(want, Glow.boxBlur(px, 5, 5, 2));
    }
}
