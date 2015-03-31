package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.config.PerformanceCountersXmlElement;

/**
 * This class should by performance modules who wish to work with the configuration data.
 *
 * Created by gupele on 3/30/2015.
 */
public interface PerformanceCounterConfigurationAware {
    void addConfigurationData(PerformanceCountersXmlElement configuration);
}
