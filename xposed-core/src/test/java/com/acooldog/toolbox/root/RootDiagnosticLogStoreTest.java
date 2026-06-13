package com.acooldog.toolbox.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

public class RootDiagnosticLogStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void append_groupsLogsByDateAndSortsNewestFirst() throws Exception {
        RootDiagnosticLogStore store = new RootDiagnosticLogStore(temporaryFolder.newFolder("logs"));
        store.append(new RootDiagnosticEvent(
                1704067200000L,
                "diag-test",
                "com.company.internal",
                RootDiagnosticEvent.MODULE_PROCESS_LOG,
                "android_log",
                "older"
        ));
        store.append(new RootDiagnosticEvent(
                1704067201000L,
                "diag-test",
                "com.company.internal",
                RootDiagnosticEvent.MODULE_PROCESS_LOG,
                "android_log",
                "newer"
        ));

        List<String> dates = store.listDates();
        List<RootDiagnosticEvent> events = store.loadEventsForDate("2024-01-01");

        assertEquals("2024-01-01", dates.get(0));
        assertEquals("newer", events.get(0).getDetail());
        assertEquals("older", events.get(1).getDetail());
        assertTrue(store.buildTextForDate("2024-01-01").contains("com.company.internal"));
    }
}
