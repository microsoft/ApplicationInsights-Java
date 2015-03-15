package com.microsoft.applicationinsights.internal.jmx;

import java.util.List;
import java.util.Map;

/**
 * Created by gupele on 3/15/2015.
 */
public final class JmxWebData {
    private final Map<String, List<JmxAttributeData>> dataToFetch;

    public JmxWebData(Map<String, List<JmxAttributeData>> dataToFetch) {
        this.dataToFetch = dataToFetch;
    }

    public Map<String, List<JmxAttributeData>> getDataToFetch() {
        return dataToFetch;
    }
}
