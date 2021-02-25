package com.microsoft.jfr;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class RecordingOptionsTest {

    @DataProvider(name="nameValues")
    public static Object[][] nameValues() {
        return new Object[][] {
                {"test", "test"},
                {" test", "test"},
                {" test ", "test"},
                {"", ""},
                {null, ""}
        };
    }
    @Test(dataProvider = "nameValues")
    public void testGetName(String[] args) {
        String expected = args[1];
        RecordingOptions opts = new RecordingOptions.Builder().name(args[0]).build();
        assertEquals(opts.getName(), expected);
    }

    @Test
    public void testGetNameDefault() {
        String expected = "";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getName(), expected);
    }

    @DataProvider(name="maxAgeGoodValues")
    public static Object[][] maxAgeGoodValues() {
        return new Object[][] {
            {"3 ns", "3 ns"},
            {"3 us", "3 us"},
            {"3 ms", "3 ms"},
            {"3 s", "3 s"},
            {"3 m", "3 m"},
            {"3 h", "3 h"},
            {"3 h", "3 h"},
            {"+3 d", "3 d"},
            {"3ms", "3 ms"},
            {"0", "0"},
            {"", "0"},
            {null, "0"}
        };
    }
    @Test(dataProvider = "maxAgeGoodValues")
    public void testGetMaxAge(String[] args) {
        RecordingOptions opts = new RecordingOptions.Builder().maxAge(args[0]).build();
        assertEquals(opts.getMaxAge(), args[1]);
    }

    @Test
    public void testGetMaxAgeDefault() {
        String expected = "0";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getMaxAge(), expected);
    }

    @DataProvider(name="maxAgeBadValues")
    public static Object[][] maxAgeBadValues() {
        return new Object[][] {
                {"-3 ms"},
                {"3 ps"},
                {"3.0 ms"},
                {"3-ms"},
                {"us"},
                {"3_ms"},
                {"3 _ms"},
                {"3_ ms"},
                {"3 _ ms"}
        };
    }
    @Test(dataProvider = "maxAgeBadValues", expectedExceptions = {IllegalArgumentException.class})
    public void testGetMaxAgeNegative(String[] args) {
        RecordingOptions opts = new RecordingOptions.Builder().maxAge(args[0]).build();
    }

    @DataProvider(name="maxSizeGoodValues")
    public static Object[][] maxSizeGoodValues() {
        return new Object[][] {
                {"12345", "12345"},
                {"+54321", "54321"},
                {" 6789", "6789"},
                {" 6789 ", "6789"},
                {" 06789 ", "6789"},
                {"0", "0"},
                {"", "0"},
                {null, "0"}
        };
    }
    @Test(dataProvider = "maxSizeGoodValues")
    public void testGetMaxSize(String[] args) {
        RecordingOptions opts = new RecordingOptions.Builder().maxSize(args[0]).build();
        assertEquals(opts.getMaxSize(), args[1]);
    }

    @Test
    public void testGetMaxSizeDefault() {
        String expected = "0";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getMaxSize(), expected);
    }

    @DataProvider(name="maxSizeBadValues")
    public static Object[][] maxSizeBadValues() {
        return new Object[][] {
                {"-12345"},
                {"5.4321"},
                {"BAD"},
                {"0xBEEF"}
        };
    }
    @Test(dataProvider = "maxSizeBadValues", expectedExceptions = {IllegalArgumentException.class})
    public void testGetMaxSizeNegative(String[] args) {
        RecordingOptions opts = new RecordingOptions.Builder().maxSize(args[0]).build();
    }

    @Test
    public void testGetDumpOnExit() {
        String expected = "true";
        RecordingOptions opts = new RecordingOptions.Builder().dumpOnExit(expected).build();
        assertEquals(opts.getDumpOnExit(), expected);
    }

    @Test
    public void testGetDumpOnExitDefault() {
        String expected = "false";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getDumpOnExit(), expected);
    }

    @Test
    public void testGetDumpOnExitBadValue() {
        String expected = "false";
        RecordingOptions opts = new RecordingOptions.Builder().dumpOnExit("BAD_VALUE").build();
        assertEquals(opts.getDumpOnExit(), expected);
    }

    @DataProvider(name="destinationValues")
    public static Object[][] destinationValues() {
        return new Object[][] {
                {"./destination", "./destination"},
                {" ./destination", "./destination"},
                {" ./destination ", "./destination"},
                {"", ""},
                {null, ""}
        };
    }
    @Test(dataProvider = "destinationValues")
    public void testGetDestination(String[] args) {
        String expected = args[1];
        RecordingOptions opts = new RecordingOptions.Builder().destination(args[0]).build();
        assertEquals(opts.getDestination(), expected);
    }

    @Test
    public void testGetDestinationDefault() {
        String expected = "";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getDestination(), expected);
    }

    @Test
    public void testGetDisk() {
        String expected = "true";
        RecordingOptions opts = new RecordingOptions.Builder().disk(expected).build();
        assertEquals(opts.getDisk(), expected);
    }

    @Test
    public void testGetDiskDefault() {
        String expected = "false";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getDisk(), expected);
    }

    @Test
    public void testGetDiskBadValue() {
        String expected = "false";
        RecordingOptions opts = new RecordingOptions.Builder().disk("BAD_VALUE").build();
        assertEquals(opts.getDisk(), expected);
    }

    @DataProvider(name="durationGoodValues")
    public static Object[][] durationGoodValues() {
        return new Object[][] {
                {"3 ns", "3 ns"},
                {"3 us", "3 us"},
                {"3 ms", "3 ms"},
                {"3 s", "3 s"},
                {"3 m", "3 m"},
                {"3 h", "3 h"},
                {"3 h", "3 h"},
                {"+3 d", "3 d"},
                {"3ms", "3 ms"},
                {"0", "0"},
                {"", "0"},
                {null, "0"}
        };
    }
    @Test(dataProvider = "durationGoodValues")
    public void testGetDuration(String[] args) {
        String expected = args[1];
        RecordingOptions opts = new RecordingOptions.Builder().duration(args[0]).build();
        assertEquals(opts.getDuration(), expected);
    }

    @Test
    public void testGetDurationDefault() {
        String expected = "0";
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getDuration(), expected);
    }

    @DataProvider(name="durationBadValues")
    public static Object[][] durationBadValues() {
        return new Object[][] {
                {"-3 ms"},
                {"3 ps"},
                {"3.0 ms"},
                {"3-ms"},
                {"us"},
                {"3_ms"},
                {"3 _ms"},
                {"3_ ms"},
                {"3 _ ms"}
        };
    }
    @Test(dataProvider = "durationBadValues", expectedExceptions = {IllegalArgumentException.class})
    public void testGetDurationNegative(String[] args) {
        RecordingOptions opts = new RecordingOptions.Builder().duration(args[0]).build();
    }

    @Test
    public void testGetRecordingOptions() {
        Map<String,String> expected = new HashMap<>();
        expected.put("name", "test");
        expected.put("maxAge", "3 m");
        expected.put("maxSize", "1048576");
        expected.put("dumpOnExit", "true");
        expected.put("destination", "test.jfr");
        expected.put("disk", "true");
        expected.put("duration", "120 s");
        RecordingOptions opts = new RecordingOptions.Builder()
                .name("test")
                .maxAge("3 m")
                .maxSize("1048576")
                .dumpOnExit("true")
                .destination("test.jfr")
                .disk("true")
                .duration("120 s")
                .build();
        assertEquals(opts.getRecordingOptions(), expected);
    }

    @Test
    public void testGetRecordingOptionsDefaults() {
        Map<String,String> expected = new HashMap<>();
        // Due to a bug, some JVMs default "disk=true". So include "disk=false" (the documented default)
        // to insure consistent behaviour.
        expected.put("disk", "false");
        RecordingOptions opts = new RecordingOptions.Builder().build();
        assertEquals(opts.getRecordingOptions(), expected);
    }
}