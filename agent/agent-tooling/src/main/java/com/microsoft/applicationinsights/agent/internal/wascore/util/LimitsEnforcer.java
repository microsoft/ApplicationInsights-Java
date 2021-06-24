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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LimitsEnforcer {

  private static final Logger logger = LoggerFactory.getLogger(LimitsEnforcer.class);

  enum Type {
    DEFAULT_ON_ERROR,
    CLOSEST_LIMIT_ON_ERROR
  }

  private final Type type;

  private final int maximum;

  private final int minimum;

  private final int defaultValue;

  private final String propertyName;

  private int currentValue;

  public int getMaximum() {
    return maximum;
  }

  public int getMinimum() {
    return minimum;
  }

  public int getDefaultValue() {
    return defaultValue;
  }

  public int getCurrentValue() {
    return currentValue;
  }

  public int normalizeValue(Integer value) {
    currentValue = getValue(value);
    return currentValue;
  }

  private int getValue(Integer value) {
    switch (type) {
      case DEFAULT_ON_ERROR:
        if (value == null || value < minimum || value > maximum) {
          logger.warn(
              "'{}': bad value is replaced by the default: '{}'", propertyName, defaultValue);
          return defaultValue;
        } else {
          return value;
        }

      case CLOSEST_LIMIT_ON_ERROR:
        if (value == null) {
          logger.debug("'{}': null value is replaced with '{}'", propertyName, defaultValue);
          return defaultValue;
        } else if (value < minimum) {
          logger.warn(
              "'{}': value is under the minimum, therefore is replaced with '{}'",
              propertyName,
              minimum);
          return minimum;
        } else if (value > maximum) {
          logger.warn(
              "'{}': value is above the maximum, therefore is replaced with '{}'",
              propertyName,
              maximum);
          return maximum;
        } else {
          return value;
        }
    }
    throw new IllegalStateException("Unknown type " + type);
  }

  public int normalizeStringValue(String value) {
    return normalizeValue(translate(propertyName, value));
  }

  private LimitsEnforcer(
      Type type,
      int minimum,
      int maximum,
      int defaultValue,
      Integer currentValue,
      String propertyName) {
    if (maximum < minimum) {
      throw new IllegalStateException("maximum must be >= than minimum");
    }
    if (defaultValue < minimum || defaultValue > maximum) {
      throw new IllegalStateException(
          "defaultValue must be: 'defaultValue >= minimum && defaultValue <= maximum");
    }

    this.propertyName = propertyName;

    this.type = type;
    this.maximum = maximum;
    this.minimum = minimum;
    this.defaultValue = defaultValue;
    this.currentValue = normalizeValue(currentValue);
  }

  public static LimitsEnforcer createWithDefaultOnError(
      String propertyName, int minimum, int maximum, int defaultValue, Integer currentValue) {
    return new LimitsEnforcer(
        Type.DEFAULT_ON_ERROR, minimum, maximum, defaultValue, currentValue, propertyName);
  }

  public static LimitsEnforcer createWithClosestLimitOnError(
      String propertyName, int minimum, int maximum, int defaultValue, Integer currentValue) {
    return new LimitsEnforcer(
        Type.CLOSEST_LIMIT_ON_ERROR, minimum, maximum, defaultValue, currentValue, propertyName);
  }

  private static Integer translate(String propertyName, String valueAsString) {
    Integer value = null;
    if (valueAsString != null) {
      try {
        value = Integer.parseInt(valueAsString);
      } catch (NumberFormatException e) {
        logger.warn("'{}': bad format for value '{}'", propertyName, valueAsString);
        logger.trace("'{}': bad format for value '{}'", propertyName, valueAsString, e);
      }
    }

    return value;
  }
}
