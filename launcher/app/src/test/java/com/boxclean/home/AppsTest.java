package com.boxclean.home;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class AppsTest {
    private final Map<String, String> labels = Map.of(
            "com.netflix.ninja", "Netflix",
            "com.yt", "YouTube",
            "com.b", "beta",
            "com.a", "Alpha",
            "com.boxclean.home", "BoxClean Home");

    @Test
    public void dockKeepsConfigOrderAndSkipsMissing() {
        assertEquals(List.of("com.yt", "com.netflix.ninja"),
                Apps.dock(List.of("com.missing", "com.yt", "com.netflix.ninja"), labels.keySet()));
    }

    @Test
    public void gridIsEverythingElseByLabelIgnoringCaseWithoutSelf() {
        assertEquals(List.of("com.a", "com.b", "com.netflix.ninja"),
                Apps.grid(List.of("com.yt"), labels, "com.boxclean.home"));
    }
}
