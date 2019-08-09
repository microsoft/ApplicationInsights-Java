package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;

/**
 * Created by gupele on 4/29/2015.
 */
public abstract class AbstractWindowsPerformanceCounter implements PerformanceCounter {

    protected static final String SDK_VERSION = "java-metric-windows:" + PropertyHelper.getSdkVersionNumber();

    protected void reportError(double value, String displayName) {
        if (!InternalLogger.INSTANCE.isErrorEnabled()) {
            return;
        }

        if (value == -1) {
            InternalLogger.INSTANCE.error("Native code exception in wrapper while fetching counter value '%s'", displayName);
        } else if (value == -4) {
            InternalLogger.INSTANCE.error("Native code exception in internal wrapper while fetching counter value '%s'", displayName);
        } else if (value == -2) {
            InternalLogger.INSTANCE.error("Native code exception performance counter '%s' not found", displayName);
        } else if (value == -7) {
            InternalLogger.INSTANCE.error("Native code exception while fetching counter value '%s'", displayName);
        } else {
            InternalLogger.INSTANCE.error("Native code unknown exception while fetching counter value '%s'", displayName);
        }
    }

    protected AbstractWindowsPerformanceCounter() {
    }
}
