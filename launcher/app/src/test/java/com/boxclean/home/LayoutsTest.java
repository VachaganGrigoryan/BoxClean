package com.boxclean.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class LayoutsTest {
    @Test
    public void bentoComesFirstAndIdsAreUnique() {
        List<String> ids = Layouts.IDS;
        assertEquals(Layouts.BENTO, ids.get(0));
        assertEquals(ids.size(), new HashSet<>(ids).size());
    }

    @Test
    public void unknownOrMissingIdFallsBackToBento() {
        assertEquals(Layouts.BENTO, Layouts.byId(null));
        assertEquals(Layouts.BENTO, Layouts.byId("gone"));
        assertEquals(Layouts.CLASSIC, Layouts.byId(Layouts.CLASSIC));
    }

    @Test
    public void everyLayoutHasANameAndAPreview() {
        for (String id : Layouts.IDS) {
            assertTrue(id, Layouts.nameRes(id) != 0 && Layouts.previewRes(id) != 0);
        }
    }
}
