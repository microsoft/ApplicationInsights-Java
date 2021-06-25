/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.wascore.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LimitsEnforcerTest {
  private static final String MOCK_PROPERTY_NAME = "MockProperty";

  @Test
  void testCreateDefaultOnErrorMinBiggerThanMax() {
    assertThatThrownBy(
            () -> LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testCreateDefaultOnErrorDefaultNotBetweenMinAndMax() {
    assertThatThrownBy(
            () -> LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testCreateDefaultOnErrorCurrentValueLowerThanMin() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateDefaultOnErrorCurrentValueHigherThanMax() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateDefaultOnErrorCurrentValueIsNull() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
    assertThat(enforcer.getMinimum()).isEqualTo(10);
    assertThat(enforcer.getMaximum()).isEqualTo(900);
    assertThat(enforcer.getDefaultValue()).isEqualTo(100);
  }

  @Test
  void testCreateDefaultOnErrorCurrentValueWithinLimits() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    assertThat(enforcer.getCurrentValue()).isEqualTo(200);
  }

  @Test
  void testCreateDefaultOnErrorNewCurrentValueHigherThanMax() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateDefaultOnErrorNewCurrentValueLowerThanMin() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(-1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateDefaultOnErrorNewCurrentValueWithinLimits() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(700);
    assertThat(enforcer.getCurrentValue()).isEqualTo(700);
  }

  @Test
  void testCreateDefaultOnErrorNewCurrentValueIsNull() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithDefaultOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(null);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateClosestOnErrorMinBiggerThanMax() {
    assertThatThrownBy(
            () -> LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 9, 10, 10))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testCreateClosestOnErrorDefaultNotBetweenMinAndMax() {
    assertThatThrownBy(
            () ->
                LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, -10, 10))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testCreateClosestOnErrorCurrentValueLowerThanMin() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, -10);
    assertThat(enforcer.getCurrentValue()).isEqualTo(10);
  }

  @Test
  void testCreateClosestOnErrorCurrentValueHigherThanMax() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(900);
  }

  @Test
  void testCreateClosestOnErrorCurrentValueIsNull() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, null);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateClosestOnErrorCurrentValueWithinLimits() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    assertThat(enforcer.getCurrentValue()).isEqualTo(200);
  }

  @Test
  void testCreateClosestOnErrorNewCurrentValueHigherThanMax() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(900);
  }

  @Test
  void testCreateClosestOnErrorNewCurrentValueLowerThanMin() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(-1000);
    assertThat(enforcer.getCurrentValue()).isEqualTo(10);
  }

  @Test
  void testCreateClosestOnErrorNewCurrentValueWithinLimits() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(700);
    assertThat(enforcer.getCurrentValue()).isEqualTo(700);
  }

  @Test
  void testCreateClosestOnErrorNewCurrentValueIsNull() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeValue(null);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateClosestOnErrorNewStringCurrentValueIsNull() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeStringValue(null);
    assertThat(enforcer.getCurrentValue()).isEqualTo(100);
  }

  @Test
  void testCreateClosestOnErrorNewStringCurrentValueWithinLimits() {
    LimitsEnforcer enforcer =
        LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 10, 900, 100, 200);
    enforcer.normalizeStringValue("700");
    assertThat(enforcer.getCurrentValue()).isEqualTo(700);
  }
}
