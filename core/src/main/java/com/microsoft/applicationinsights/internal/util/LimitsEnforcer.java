package com.microsoft.applicationinsights.internal.util;

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
                    logger.warn("'{}': bad value is replaced by the default: '{}'", propertyName, defaultValue);
                    return defaultValue;
                } else {
                    return value;
                }

            case CLOSEST_LIMIT_ON_ERROR:
                if (value == null) {
                    logger.debug("'{}': null value is replaced with '{}'", propertyName, defaultValue);
                    return defaultValue;
                } else if (value < minimum) {
                    logger.warn("'{}': value is under the minimum, therefore is replaced with '{}'", propertyName, minimum);
                    return minimum;
                } else if (value > maximum) {
                    logger.warn("'{}': value is above the maximum, therefore is replaced with '{}'", propertyName, maximum);
                    return maximum;
                } else {
                    return value;
                }
        }
        throw new IllegalStateException("Unknown type "+type);
    }

    public int normalizeStringValue(String value) {
        return normalizeValue(translate(propertyName, value));
    }

    private LimitsEnforcer(Type type, int minimum, int maximum, int defaultValue, Integer currentValue, String propertyName) {
        if (maximum < minimum) {
            throw new IllegalStateException("maximum must be >= than minimum");
        }
        if (defaultValue < minimum || defaultValue > maximum) {
            throw new IllegalStateException("defaultValue must be: 'defaultValue >= minimum && defaultValue <= maximum");
        }

        this.propertyName = propertyName;

        this.type = type;
        this.maximum = maximum;
        this.minimum = minimum;
        this.defaultValue = defaultValue;
        this.currentValue = normalizeValue(currentValue);
    }

    public static LimitsEnforcer createWithDefaultOnError(String propertyName, int minimum, int maximum, int defaultValue, Integer currentValue) {
        return new LimitsEnforcer(Type.DEFAULT_ON_ERROR, minimum, maximum, defaultValue, currentValue, propertyName);
    }

    public static LimitsEnforcer createWithClosestLimitOnError(String propertyName, int minimum, int maximum, int defaultValue, Integer currentValue) {
        return new LimitsEnforcer(Type.CLOSEST_LIMIT_ON_ERROR, minimum, maximum, defaultValue, currentValue, propertyName);
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

