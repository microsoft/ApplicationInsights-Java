package com.microsoft.applicationinsights.internal.util;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;

/** Created by gupele on 5/10/2015. */
public final class LimitsEnforcer {

  private final Type type;
  private final int maximum;
  private final int minimum;
  private final int defaultValue;
  private final String propertyName;
  private int currentValue;

  private LimitsEnforcer(
      Type type,
      int minimum,
      int maximum,
      int defaultValue,
      Integer currentValue,
      String propertyName) {
    Preconditions.checkState(maximum >= minimum, "maximum must be >= than minimum");
    Preconditions.checkState(
        defaultValue >= minimum && defaultValue <= maximum,
        "defaultValue must be: 'defaultValue >= minimum && defaultValue <= maximum");

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

  public static LimitsEnforcer createWithDefaultOnError(
      int minimum, int maximum, int defaultValue, String propertyName, String currentValue) {
    return new LimitsEnforcer(
        Type.DEFAULT_ON_ERROR,
        minimum,
        maximum,
        defaultValue,
        translate(propertyName, currentValue),
        propertyName);
  }

  public static LimitsEnforcer createWithClosestLimitOnError(
      String propertyName, int minimum, int maximum, int defaultValue, Integer currentValue) {
    return new LimitsEnforcer(
        Type.CLOSEST_LIMIT_ON_ERROR, minimum, maximum, defaultValue, currentValue, propertyName);
  }

  public static LimitsEnforcer createWithClosestLimitOnError(
      int minimum, int maximum, int defaultValue, String propertyName, String currentValue) {
    return new LimitsEnforcer(
        Type.CLOSEST_LIMIT_ON_ERROR,
        minimum,
        maximum,
        defaultValue,
        translate(propertyName, currentValue),
        propertyName);
  }

  private static Integer translate(String propertyName, String valueAsString) {
    Integer value = null;
    if (valueAsString != null) {
      try {
        value = Integer.parseInt(valueAsString);
      } catch (NumberFormatException e) {
        InternalLogger.INSTANCE.warn(
            "'%s': bad format for value '%s'", propertyName, valueAsString);
        InternalLogger.INSTANCE.trace(
            "Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
      }
    }

    return value;
  }

  public Type getType() {
    return type;
  }

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
    switch (type) {
      case DEFAULT_ON_ERROR:
        if (value == null || value < minimum || value > maximum) {
          InternalLogger.INSTANCE.warn(
              "'%s': bad value is replaced by the default: '%d'", propertyName, defaultValue);
          currentValue = defaultValue;
        } else {
          currentValue = value;
        }
        break;

      case CLOSEST_LIMIT_ON_ERROR:
        if (value == null) {
          currentValue = defaultValue;
          InternalLogger.INSTANCE.info(
              "'%s': null value is replaced with '%d'", propertyName, defaultValue);
        } else if (value < minimum) {
          currentValue = minimum;
          InternalLogger.INSTANCE.warn(
              "'%s': value is under the minimum, therefore is replaced with '%d'",
              propertyName, minimum);
        } else if (value > maximum) {
          currentValue = maximum;
          InternalLogger.INSTANCE.warn(
              "'%s': value is above the maximum, therefore is replaced with '%d'",
              propertyName, maximum);
        } else {
          currentValue = value;
        }
        break;

      default:
        throw new IllegalStateException(String.format("Unknown type %s", type));
    }

    return currentValue;
  }

  public int normalizeStringValue(String value) {
    return normalizeValue(translate(propertyName, value));
  }

  enum Type {
    DEFAULT_ON_ERROR,
    CLOSEST_LIMIT_ON_ERROR
  }
}
