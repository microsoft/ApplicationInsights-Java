package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitsEnforcerTest {
    private final static String MOCK_PROPERTY_NAME = "MockProperty";

    @Test
    void testCreateDefaultOnErrorMinBiggerThanMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCreateDefaultOnErrorDefaultNotBetweenMinAndMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCreateDefaultOnErrorCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateDefaultOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateDefaultOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
        assertThat(enforcer.getMinimum()).isEqualTo(10);
        assertThat(enforcer.getMaximum()).isEqualTo(900);
        assertThat(enforcer.getDefaultValue()).isEqualTo(100);
    }

    @Test
    void testCreateDefaultOnErrorCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        assertThat(enforcer.getCurrentValue()).isEqualTo(200);
    }

    @Test
    void testCreateDefaultOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateDefaultOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateDefaultOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }

    @Test
    void testCreateDefaultOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateClosestOnErrorMinBiggerThanMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCreateClosestOnErrorDefaultNotBetweenMinAndMax() {
        assertThatThrownBy(() ->
                LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCreateClosestOnErrorCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
        assertThat(enforcer.getCurrentValue()).isEqualTo(10);
    }

    @Test
    void testCreateClosestOnErrorCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(900);
    }

    @Test
    void testCreateClosestOnErrorCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateClosestOnErrorCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        assertThat(enforcer.getCurrentValue()).isEqualTo(200);
    }

    @Test
    void testCreateClosestOnErrorNewCurrentValueHigherThanMax() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(900);
    }

    @Test
    void testCreateClosestOnErrorNewCurrentValueLowerThanMin() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(-1000);
        assertThat(enforcer.getCurrentValue()).isEqualTo(10);
    }

    @Test
    void testCreateClosestOnErrorNewCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(700);
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }

    @Test
    void testCreateClosestOnErrorNewCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateClosestOnErrorNewStringCurrentValueIsNull() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue(null);
        assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    }

    @Test
    void testCreateClosestOnErrorNewStringCurrentValueWithinLimits() {
        LimitsEnforcer enforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
        enforcer.normalizeStringValue("700");
        assertThat(enforcer.getCurrentValue()).isEqualTo(700);
    }
}