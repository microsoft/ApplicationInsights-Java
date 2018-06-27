package com.microsoft.applicationinsights.web.internal.perfcounter;

import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/** Created by gupele on 3/12/2015. */
final class DefaultWebPerformanceCountersFactory implements WebPerformanceCountersFactory {
  private static final String REQUEST_COUNT_PC_CATEGORY_NAME = "ASP.NET Applications";
  private static final String REQUEST_COUNT_PC_COUNTER_NAME = "Requests/Sec";
  private static final String REQUEST_COUNT_ATTRIBUTE_DISPLAY_NAME = "Request Count";

  private static final String TOMCAT_GRP_SERVER_NAME =
      "Catalina:type=GlobalRequestProcessor,name=*";
  private static final String TOMCAT_RC_ATTRIBUTE_NAME = "requestCount";

  @Override
  public Collection<PerformanceCounter> getPerformanceCounters() {
    ArrayList<PerformanceCounter> pcs = new ArrayList<PerformanceCounter>();

    return pcs;
  }

  private void addTomcatData(Map<String, Collection<JmxAttributeData>> requestCountData) {
    ArrayList<JmxAttributeData> defaultWeb = new ArrayList<JmxAttributeData>();
    defaultWeb.add(
        new JmxAttributeData(REQUEST_COUNT_ATTRIBUTE_DISPLAY_NAME, TOMCAT_RC_ATTRIBUTE_NAME));
    requestCountData.put(TOMCAT_GRP_SERVER_NAME, defaultWeb);
  }
}
