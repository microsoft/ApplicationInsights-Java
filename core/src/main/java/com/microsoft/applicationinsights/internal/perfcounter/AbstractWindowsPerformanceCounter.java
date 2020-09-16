package com.microsoft.applicationinsights.internal.perfcounter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gupele on 4/29/2015.
 */
public abstract class AbstractWindowsPerformanceCounter implements PerformanceCounter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWindowsPerformanceCounter.class);

    protected void reportError(double value, String displayName) {
        if (!logger.isErrorEnabled()) {
            return;
        }

        if (value == -1) {
            logger.error("Native code exception in wrapper while fetching counter value '{}'", displayName);
        } else if (value == -4) {
            logger.error("Native code exception in internal wrapper while fetching counter value '{}'", displayName);
        } else if (value == -2) {
            logger.error("Native code exception performance counter '{}' not found", displayName);
        } else if (value == -7) {
            logger.error("Native code exception while fetching counter value '{}'", displayName);
        } else {
            logger.error("Native code unknown exception while fetching counter value '{}'", displayName);
        }
    }

    protected AbstractWindowsPerformanceCounter() {
    }
}
