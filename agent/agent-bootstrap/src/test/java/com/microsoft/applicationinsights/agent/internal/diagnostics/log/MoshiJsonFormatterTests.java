package com.microsoft.applicationinsights.agent.internal.diagnostics.log;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.*;

public class MoshiJsonFormatterTests {

    private MoshiJsonFormatter formatter;

    @Before
    public void setup() {
        formatter = new MoshiJsonFormatter();
    }

    @After
    public void tearDown() {
        formatter = null;
    }

    @Test
    public void formatterSerializesSimpleMap() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("s1", "v1");
        m.put("int1", 123);
        m.put("b", true);
        assertEquals("{\"b\":true,\"int1\":123,\"s1\":\"v1\"}", formatter.toJsonString(m));
    }

    @Test
    public void formatterWithPrettyPrintPrintsPretty() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("s1", "v1");
        m.put("int1", 123);
        formatter.setPrettyPrint(true);
        assertEquals("{\n" +
                "  \"int1\": 123,\n" +
                "  \"s1\": \"v1\"\n" +
                "}", formatter.toJsonString(m));
    }

}
