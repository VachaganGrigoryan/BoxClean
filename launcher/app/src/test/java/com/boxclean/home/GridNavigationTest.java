package com.boxclean.home;

import static com.boxclean.home.GridNavigation.DOWN;
import static com.boxclean.home.GridNavigation.DOWN_OUT;
import static com.boxclean.home.GridNavigation.LEFT;
import static com.boxclean.home.GridNavigation.RIGHT;
import static com.boxclean.home.GridNavigation.STAY;
import static com.boxclean.home.GridNavigation.UP;
import static com.boxclean.home.GridNavigation.UP_OUT;
import static com.boxclean.home.GridNavigation.next;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GridNavigationTest {
    // 10 tiles in 4 columns:  0 1 2 3 / 4 5 6 7 / 8 9
    @Test
    public void leftRightNeverWrap() {
        assertEquals(STAY, next(4, 10, 4, LEFT));
        assertEquals(4, next(5, 10, 4, LEFT));
        assertEquals(STAY, next(3, 10, 4, RIGHT));
        assertEquals(STAY, next(9, 10, 4, RIGHT));
        assertEquals(1, next(0, 10, 4, RIGHT));
    }

    @Test
    public void upDownMoveExactlyOneRow() {
        assertEquals(UP_OUT, next(2, 10, 4, UP));
        assertEquals(2, next(6, 10, 4, UP));
        assertEquals(6, next(2, 10, 4, DOWN));
        assertEquals(9, next(7, 10, 4, DOWN));   // short last row: nearest tile below
        assertEquals(DOWN_OUT, next(8, 10, 4, DOWN));
    }

    @Test
    public void singleRowDock() {
        assertEquals(UP_OUT, next(1, 4, 4, UP));
        assertEquals(DOWN_OUT, next(1, 4, 4, DOWN));
    }
}
