package com.microsoft.applicationinsights.internal.util;

import org.junit.Test;

import static org.junit.Assert.*;

public final class LimitsEnforcerTest {
    private final static String MOCK_PROPERTY_NAME = "MockProperty";

    @Test(expected = IllegalStateException.class)
    public void testCreateDefaultOnErrorMinBiggerThanMax() {
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateDefaultOnErrorDefaultNotBetweenMinAndMax() {
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10);
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertEquals(100, enforcer.getCurrentValue());
        assertEquals(10, enforcer.getMinimum());
        assertEquals(900, enforcer.getMaximum());
        assertEquals(100, enforcer.getDefaultValue());
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        assertEquals(200, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertEquals(700, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertEquals(100, enforcer.getCurrentValue());
    }

    //
    @Test(expected = IllegalStateException.class)
    public void testCreateClosestOnErrorMinBiggerThanMax() {
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateClosestOnErrorDefaultNotBetweenMinAndMax() {
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10);
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
        assertEquals(10, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertEquals(900, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        assertEquals(200, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertEquals(900, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertEquals(10, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertEquals(700, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewStringCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue(null);
        assertEquals(100, enforcer.getCurrentValue());
    }

    @Test
    public void testCreateClosestOnErrorNewStringCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue("700");
        assertEquals(700, enforcer.getCurrentValue());
    }
}