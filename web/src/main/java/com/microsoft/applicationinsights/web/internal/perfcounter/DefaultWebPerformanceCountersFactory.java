package com.microsoft.applicationinsights.web.internal.perfcounter;

import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.perfcounter.JmxMetricPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by gupele on 3/12/2015.
 */
final class DefaultWebPerformanceCountersFactory implements WebPerformanceCountersFactory {
    private static final String TOMCAT_REQUEST_COUNTER_ID = "Tomcat RCID";
    private static final String TOMCAT_GRP_SERVER_NAME = "Catalina:type=GlobalRequestProcessor,name=*";
    private static final String TOMCAT_RC_ATTRIBUTE_NAME = "requestCount";
    private static final String TOMCAT_RC_ATTRIBUTE_DISPLAY_NAME = "Request Count";

    @Override
    public Collection<PerformanceCounter> getPerformanceCounters() {
        ArrayList<JmxAttributeData> defaultWeb = new ArrayList<JmxAttributeData>();
        defaultWeb.add(new JmxAttributeData(TOMCAT_RC_ATTRIBUTE_DISPLAY_NAME, TOMCAT_RC_ATTRIBUTE_NAME));

        ArrayList<PerformanceCounter> pcs = new ArrayList<PerformanceCounter>();
        pcs.add(new JmxMetricPerformanceCounter(TOMCAT_REQUEST_COUNTER_ID, TOMCAT_GRP_SERVER_NAME, defaultWeb));

        return pcs;
    }
}
