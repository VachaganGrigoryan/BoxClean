package com.boxclean.home;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class RemoteKeysTest {
    // KeyEvent.keyCodeFromString is not on the JVM; 0 means "not a key", as on Android.
    private static int names(String name) {
        return name.equals("KEYCODE_BUTTON_7") ? 194 : name.equals("KEYCODE_BUTTON_5") ? 192 : 0;
    }

    @Test
    public void parsesNamesAndNumbers() {
        Map<Integer, String> keys = RemoteKeys.parseAppKeys(
                Arrays.asList("KEYCODE_BUTTON_7:ru.kinopoisk.tv", " 192 : com.techcraeft.kinodaran "),
                RemoteKeysTest::names);
        assertEquals(Map.of(194, "ru.kinopoisk.tv", 192, "com.techcraeft.kinodaran"), keys);
    }

    @Test
    public void skipsBadEntriesAndLastDuplicateWins() {
        Map<Integer, String> keys = RemoteKeys.parseAppKeys(
                Arrays.asList("nocolon", "KEYCODE_NOPE:x.y", "194:", "lower:x.y", "0:x.y", null,
                        "194:first.app", "KEYCODE_BUTTON_7:second.app"),
                RemoteKeysTest::names);
        assertEquals(Map.of(194, "second.app"), keys);
    }
}
