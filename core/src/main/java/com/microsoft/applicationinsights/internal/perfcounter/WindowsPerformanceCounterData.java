package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Created by gupele on 3/30/2015.
 */
public final class WindowsPerformanceCounterData {
    public String displayName;
    public String categoryName;
    public String counterName;
    public String instanceName;

    public WindowsPerformanceCounterData setDisplayName(String displayName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName), "displayName must be non-null and non empty value.");
        this.displayName = displayName;
        return this;
    }

    public WindowsPerformanceCounterData setCategoryName(String categoryName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(categoryName), "categoryName must be non-null and non empty value.");
        this.categoryName = categoryName;
        return this;
    }

    public WindowsPerformanceCounterData setCounterName(String counterName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(counterName), "counterName must be non-null and non empty value.");
        this.counterName = counterName;
        return this;
    }

    /**
     * Sets the instance name, the method will consult the JniPCConnector for the proper instance name.
     * @param instanceName The requested instance name.
     * @return 'this'.
     * @throws Throwable The method might throw an Error if the JniPCConnector is not able to properly connect to the native code.
     */
    public WindowsPerformanceCounterData setInstanceName(String instanceName) throws Throwable {
        String translatedInstanceName;
        try {
            translatedInstanceName = JniPCConnector.translateInstanceName(instanceName);
            this.instanceName = translatedInstanceName;
        } catch (Throwable e) {
            InternalLogger.INSTANCE.error("Failed to translate instance name '%s': '%s'", instanceName, e.getMessage());
            throw e;
        }
        return this;
    }
}
