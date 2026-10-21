package com.boxclean.home;

import android.view.KeyEvent;

/**
 * D-pad moves in a row-major grid: left/right never wrap at a row end, up/down move exactly one row.
 * Pure, so it is unit-tested off the TV. Idea from CleanYourTV's GridNavigation (MIT, see NOTICE).
 */
final class GridNavigation {
    static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
    /** Results that are not an index: keep focus where it is, or leave the grid above / below. */
    static final int STAY = -1, UP_OUT = -2, DOWN_OUT = -3;

    private GridNavigation() {}

    /** The direction a D-pad key moves in, or -1 for any other key. */
    static int direction(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP: return UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return DOWN;
            default: return -1;
        }
    }

    static int next(int pos, int count, int columns, int direction) {
        int row = pos / columns, col = pos % columns, lastRow = (count - 1) / columns;
        switch (direction) {
            case LEFT: return col == 0 ? STAY : pos - 1;
            case RIGHT: return col == columns - 1 || pos == count - 1 ? STAY : pos + 1;
            case UP: return row == 0 ? UP_OUT : pos - columns;
            default: return row == lastRow ? DOWN_OUT : Math.min(pos + columns, count - 1);
        }
    }
}
