package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.TelemetryConfiguration;

/**
 * Created by yonisha on 2/2/2015.
 */
public interface TelemetryModule {
    void initialize(TelemetryConfiguration configuration);
}
