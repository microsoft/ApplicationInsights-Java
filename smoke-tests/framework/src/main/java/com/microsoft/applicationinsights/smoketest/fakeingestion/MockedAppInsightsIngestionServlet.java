// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.CharStreams;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class MockedAppInsightsIngestionServlet extends HttpServlet {

  // guarded by multimapLock
  private final ListMultimap<String, Envelope> type2envelope;

  private final Object multimapLock = new Object();

  private volatile boolean liveMetricsPingReceived;

  private volatile boolean loggingEnabled;

  MockedAppInsightsIngestionServlet() {
    type2envelope = MultimapBuilder.treeKeys().arrayListValues().build();
  }

  @SuppressWarnings("SystemOut")
  private void logit(String message) {
    if (loggingEnabled) {
      System.out.println("FAKE INGESTION: INFO - " + message);
    }
  }

  void resetData() {
    synchronized (multimapLock) {
      type2envelope.clear();
    }
    liveMetricsPingReceived = false;
  }

  boolean hasData() {
    return !type2envelope.isEmpty();
  }

  int getItemCount() {
    return type2envelope.size();
  }

  List<Envelope> getItemsByType(String type) {
    Objects.requireNonNull(type, "type");
    synchronized (multimapLock) {
      return type2envelope.get(type);
    }
  }

  void awaitAnyItems(long timeout, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    waitForItems(x -> true, 1, timeout, timeUnit);
  }

  List<Envelope> waitForItems(
      Predicate<Envelope> condition, int numItems, long timeout, TimeUnit timeUnit)
      throws InterruptedException, TimeoutException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    List<Envelope> targetCollection = new ArrayList<>(numItems);
    while (stopwatch.elapsed(timeUnit) < timeout) {
      targetCollection.clear();
      List<Envelope> currentValues;
      synchronized (multimapLock) {
        currentValues = new ArrayList<>(type2envelope.values());
      }
      for (Envelope val : currentValues) {
        if (condition.test(val)) {
          targetCollection.add(val);
        }
      }
      if (targetCollection.size() >= numItems) {
        return targetCollection;
      }
      TimeUnit.MILLISECONDS.sleep(75);
    }
    throw new TimeoutException("timed out waiting for items");
  }

  boolean isLiveMetricsPingReceived() {
    return liveMetricsPingReceived;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (!"/v2.1/track".equals(req.getPathInfo())
        && !"/QuickPulseService.svc/ping".equals(req.getPathInfo())) {
      resp.sendError(404, "Unknown URI");
      return;
    }

    String contentEncoding = req.getHeader("content-encoding");
    Readable reader;
    if ("gzip".equals(contentEncoding)) {
      reader = new InputStreamReader(new GZIPInputStream(req.getInputStream()), UTF_8);
    } else {
      reader = req.getReader();
    }

    StringWriter sw = new StringWriter();
    CharStreams.copy(reader, sw);
    String body = sw.toString();
    resp.setContentType("application/json");
    logit("raw payload:\n\n" + body + "\n");

    if ("/QuickPulseService.svc/ping".equals(req.getPathInfo())) {
      liveMetricsPingReceived = true;
      resp.setHeader("x-ms-qps-subscribed", "false");
      return;
    }

    String[] lines = body.split("\n");
    for (String line : lines) {
      Envelope envelope = JsonHelper.GSON.fromJson(line.trim(), Envelope.class);
      String baseType = envelope.getData().getBaseType();
      synchronized (multimapLock) {
        type2envelope.put(baseType, envelope);
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (req.getPathInfo().startsWith("/api/profiles/") && req.getPathInfo().endsWith("/appId")) {
      // any fake appId should do
      resp.getWriter().append("12341234-1234-1234-1234-123412341234");
      return;
    }

    if ("/".equals(req.getPathInfo())) {
      // just to help with debugging when hitting the endpoint manually
      resp.getWriter().append("Fake AI Endpoint Online");
    } else {
      resp.sendError(404, "Unknown URI");
    }
  }

  public void setRequestLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }
}
