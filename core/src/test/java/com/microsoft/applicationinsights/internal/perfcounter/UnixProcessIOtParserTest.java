package com.microsoft.applicationinsights.internal.perfcounter;

import org.junit.Test;

import static org.junit.Assert.*;

public final class UnixProcessIOtParserTest {
    @Test
    public void testProcess() {
        UnixProcessIOtParser parser = new UnixProcessIOtParser();

        parser.process("rchar: 1661777");
        assertFalse(parser.done());

        parser.process("wchar: 7431");
        assertFalse(parser.done());

        parser.process("syscr: 1240");
        assertFalse(parser.done());

        parser.process("syscw: 123");
        assertFalse(parser.done());

        parser.process("read_bytes: 7335936");
        assertFalse(parser.done());

        parser.process("write_bytes: 12288");
        assertTrue(parser.done());
        assertEquals(7335936 + 12288, parser.getValue(), 0.0);

        parser.process("cancelled_write_bytes: 0");
        assertTrue(parser.done());
        assertEquals(7335936 + 12288, parser.getValue(), 0.0);

        parser.process("write_bytes: 12288");
        assertTrue(parser.done());
        assertEquals(7335936 + 12288, parser.getValue(), 0.0);
    }
}
