// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MockedQuickPulseServlet extends HttpServlet {

  private final AtomicBoolean pingReceived = new AtomicBoolean(false);
  private final List<String> postBodies = new ArrayList<>();
  private final Object lock = new Object();

  private static final String BODY =
      "{\"ETag\":\"fake::etag\",\"Metrics\":[],\"QuotaInfo\":null,\"DocumentStreams\":[{\"Id\":\"all-types-default\",\"DocumentFilterGroups\":[{\"TelemetryType\":\"Request\",\"Filters\":{\"Filters\":[{\"FieldName\":\"Success\",\"Predicate\":\"Equal\",\"Comparand\":\"false\"}]}},{\"TelemetryType\":\"Dependency\",\"Filters\":{\"Filters\":[{\"FieldName\":\"Success\",\"Predicate\":\"Equal\",\"Comparand\":\"false\"}]}},{\"TelemetryType\":\"Exception\",\"Filters\":{\"Filters\":[]}},{\"TelemetryType\":\"Event\",\"Filters\":{\"Filters\":[]}},{\"TelemetryType\":\"Trace\",\"Filters\":{\"Filters\":[]}}]}]}";

  private volatile boolean loggingEnabled;

  public MockedQuickPulseServlet() {}

  @SuppressWarnings("SystemOut")
  private void logit(String message) {
    if (loggingEnabled) {
      System.out.println("FAKE INGESTION: INFO - " + message);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Readable reader = req.getReader();
    StringWriter sw = new StringWriter();
    CharStreams.copy(reader, sw);
    String body = sw.toString();

    String path = req.getPathInfo();
    logit("QuickPulse received: " + path);
    if (path.equals("/ping")) {
      pingReceived.set(true);
      logit("ping body: " + body);
      // want the following request to be a post
      resp.setHeader("x-ms-qps-configuration-etag", "fake::etag");
      resp.setHeader("x-ms-qps-subscribed", "true");
      resp.setContentType("application/json");
      resp.getWriter().write(BODY);

    } else if (path.equals("/post")) {
      synchronized (lock) {
        postBodies.add(body);
      }
      logit("post body: " + body);
      // continue to post
      resp.setHeader("x-ms-qps-subscribed", "true");
      resp.setHeader("x-ms-qps-configuration-etag", "fake::etag");
    } else {
      throw new IllegalStateException(
          "Unexpected path: " + path + " please fix the test/mock server setup");
    }
  }

  public boolean isPingReceived() {
    return pingReceived.get();
  }

  public List<String> getPostBodies() {
    synchronized (lock) {
      return new ArrayList<>(postBodies);
    }
  }

  public void setRequestLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }
}
