package com.microsoft.applicationinsights.internal.perfcounter;

import org.junit.Test;

import static org.junit.Assert.*;

public final class UnixTotalMemInfoReaderTest {
    @Test
    public void testProcess() {
        UnixTotalMemInfoParser reader = new UnixTotalMemInfoParser();
        reader.process("MemTotal:        3973736 kB");
        assertTrue(!reader.done());

        reader.process("MemFree:          431064 kB");
        assertTrue(!reader.done());

        reader.process("Buffers:           46604 kB");
        assertTrue(!reader.done());

        reader.process("Cached:           494648 kB");
        assertTrue(reader.done());

        assertEquals(972316, reader.getValue(), 0.0);

        reader.process("Cached:           494648 kB");
        assertTrue(reader.done());

        assertEquals(972316, reader.getValue(), 0.0);
    }
}