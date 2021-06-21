package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(enforcer.getCurrentValue()).isEqualTo(200);
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }

    @Test
    public void testCreateDefaultOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
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
        assertThat(enforcer.getCurrentValue()).isEqualTo(10);
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(900);
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateClosestOnErrorCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        assertThat(enforcer.getCurrentValue()).isEqualTo(200);
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(900);
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(10);
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }

    @Test
    public void testCreateClosestOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateClosestOnErrorNewStringCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    public void testCreateClosestOnErrorNewStringCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue("700");
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }
}