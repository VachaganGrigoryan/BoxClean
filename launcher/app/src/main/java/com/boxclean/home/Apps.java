package com.boxclean.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Which apps go in the dock and which in the grid. Plain Java, so it is unit-tested off the TV. */
final class Apps {
    private Apps() {}

    static List<String> dock(List<String> wanted, Collection<String> installed) {
        List<String> out = new ArrayList<>();
        for (String p : wanted) if (installed.contains(p) && !out.contains(p)) out.add(p);
        return out;
    }

    static List<String> grid(List<String> dock, Map<String, String> labels, String self) {
        List<String> out = new ArrayList<>();
        for (String p : labels.keySet()) if (!dock.contains(p) && !p.equals(self)) out.add(p);
        out.sort((a, b) -> labels.get(a).compareToIgnoreCase(labels.get(b)));
        return out;
    }
}
