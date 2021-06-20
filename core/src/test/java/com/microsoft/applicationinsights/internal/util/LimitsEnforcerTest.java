package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LimitsEnforcerTest {
    private final static String MOCK_PROPERTY_NAME = "MockProperty";

    @Test
    public void testCreateDefaultOnErrorMinBiggerThanMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testCreateDefaultOnErrorDefaultNotBetweenMinAndMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateDefaultOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
        assertThat(enforcer.getMinimum()).isEqualTo(10);
        assertThat(enforcer.getMaximum()).isEqualTo(900);
        assertThat(enforcer.getDefaultValue()).isEqualTo(100);
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

    @Test
    public void testCreateClosestOnErrorMinBiggerThanMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testCreateClosestOnErrorDefaultNotBetweenMinAndMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
                .isInstanceOf(IllegalStateException.class);
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