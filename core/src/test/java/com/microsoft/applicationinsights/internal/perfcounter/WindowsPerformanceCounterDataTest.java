package com.microsoft.applicationinsights.internal.perfcounter;

import org.junit.Test;

import static org.junit.Assert.*;

public final class WindowsPerformanceCounterDataTest {
    private static final String MOCK_CATEGORY = "category";
    private static final String MOCK_COUNTER = "counter";
    private static final String MOCK_INSTANCE = "instance";
    private static final String MOCK_DISPLAY = "display";

    @Test
    public void testAllAreOk() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);

        assertEquals(data.categoryName, MOCK_CATEGORY);
        assertEquals(data.counterName, MOCK_COUNTER);
        assertEquals(data.instanceName, MOCK_INSTANCE);
        assertEquals(data.displayName, MOCK_DISPLAY);
    }

    @Test(expected = Exception.class)
    public void testInstanceNameIsSelfProcess() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(JniPCConnector.PROCESS_SELF_INSTANCE_NAME).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCategoryName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(null).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCategoryName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName("").
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCounterName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(null).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCounterName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName("").
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(MOCK_DISPLAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCDisplayName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyDisplayName() throws Throwable {
        WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                setCategoryName(MOCK_CATEGORY).
                setCounterName(MOCK_COUNTER).
                setInstanceName(MOCK_INSTANCE).
                setDisplayName("");
    }
}