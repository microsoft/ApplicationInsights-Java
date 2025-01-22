// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.applicationinsights.smoketest.fakeingestion;

import com.google.common.io.CharStreams;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MockedQuickPulseServlet extends HttpServlet {

  private final AtomicInteger liveMetricsPingReceived = new AtomicInteger();

  private final AtomicInteger liveMetricsPostReceived = new AtomicInteger();

  private final AtomicReference<String> pingBody = new AtomicReference<>();
  private final AtomicReference<String> lastPostBody = new AtomicReference<>();

  private static final String MOCK_RESPONSE_JSON_DEFAULT_CONFIG =
      "{\"ETag\":\"fake::etag\",\"Metrics\":[],\"QuotaInfo\":null,\"DocumentStreams\":[{\"Id\":\"all-types-default\",\"DocumentFilterGroups\":[{\"TelemetryType\":\"Request\",\"Filters\":{\"Filters\":[{\"FieldName\":\"Success\",\"Predicate\":\"Equal\",\"Comparand\":\"false\"}]}},{\"TelemetryType\":\"Dependency\",\"Filters\":{\"Filters\":[{\"FieldName\":\"Success\",\"Predicate\":\"Equal\",\"Comparand\":\"false\"}]}},{\"TelemetryType\":\"Exception\",\"Filters\":{\"Filters\":[]}},{\"TelemetryType\":\"Event\",\"Filters\":{\"Filters\":[]}},{\"TelemetryType\":\"Trace\",\"Filters\":{\"Filters\":[]}}]}]}";

  private volatile boolean loggingEnabled;
  public MockedQuickPulseServlet() {

  }

  @SuppressWarnings("SystemOut")
  private void logit(String message) {
    if (loggingEnabled) {
      System.out.println("FAKE INGESTION: INFO - " + message);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
      Readable reader = req.getReader();
      StringWriter sw = new StringWriter();
      CharStreams.copy(reader, sw);
      String body = sw.toString();

      String path = req.getPathInfo();
      logit("QuickPulse received: " + path);
      if (path.equals("/ping")) {
          liveMetricsPingReceived.incrementAndGet();
          pingBody.set(body);
          logit("ping body: " + body);
          // want the following request to be a post
          resp.setHeader("x-ms-qps-subscribed", "true");
          resp.setContentType("application/json");
          resp.getWriter().write(MOCK_RESPONSE_JSON_DEFAULT_CONFIG);

      } else if (path.equals("/post")) {
          liveMetricsPostReceived.incrementAndGet();
          lastPostBody.set(body);
          logit("post body: " + body);
          // continue to post
          resp.setHeader("x-ms-qps-subscribed", "true");
      } else {
          resp.setStatus(404);
      }
  }

  public int getNumPingsReceived() {
    return liveMetricsPingReceived.get();
  }

  public int getNumPostsReceived() {
    return liveMetricsPostReceived.get();
  }

  public String getPingBody() {
    return pingBody.get();
  }

  public String getLastPostBody() {
    return lastPostBody.get();
  }

  public void setRequestLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }

}
