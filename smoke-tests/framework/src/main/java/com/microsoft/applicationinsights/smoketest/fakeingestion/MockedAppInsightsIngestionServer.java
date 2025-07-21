// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import com.microsoft.applicationinsights.smoketest.SmokeTestExtension;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MockedAppInsightsIngestionServer {
  public static final int DEFAULT_PORT = 6060;

  private final MockedAppInsightsIngestionServlet servlet;
  private final MockedProfilerSettingsServlet profilerSettingsServlet;
  private final MockedQuickPulseServlet quickPulseServlet;
  private final Server server;

  public MockedAppInsightsIngestionServer(boolean usingOld3xAgent) {
    server = new Server(DEFAULT_PORT);
    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);

    servlet = new MockedAppInsightsIngestionServlet();
    profilerSettingsServlet = new MockedProfilerSettingsServlet();
    quickPulseServlet = new MockedQuickPulseServlet(usingOld3xAgent);

    handler.addServletWithMapping(new ServletHolder(profilerSettingsServlet), "/profiler/*");
    handler.addServletWithMapping(new ServletHolder(quickPulseServlet), "/QuickPulseService.svc/*");
    handler.addServletWithMapping(new ServletHolder(servlet), "/*");
  }

  @SuppressWarnings("SystemOut")
  public void startServer() throws Exception {
    System.out.println("Starting fake Breeze ingestion...");
    server.start();
  }

  @SuppressWarnings("SystemOut")
  public void stopServer() throws Exception {
    System.out.println("Stopping fake Breeze ingestion...");
    server.stop();
    server.join();
    quickPulseServlet.resetData();
  }

  public void resetData() {
    servlet.resetData();
    quickPulseServlet.resetData();
  }

  public boolean hasData() {
    return this.servlet.hasData();
  }

  public int getItemCount() {
    return this.servlet.getItemCount();
  }

  public int getCountForType(String type) {
    Objects.requireNonNull(type, "type");
    return getItemsEnvelopeDataType(type).size();
  }

  public int getCountForType(String type, String operationId) {
    Objects.requireNonNull(type, "type");
    return (int)
        getItemsEnvelopeDataType(type).stream()
            .filter(input -> operationId.equals(input.getTags().get("ai.operation.id")))
            .count();
  }

  public List<Envelope> getItemsEnvelopeDataType(String type) {
    return this.servlet.getItemsByType(type);
  }

  public List<Envelope> getAllItems() {
    return this.servlet.getAllItems();
  }

  public <T extends Domain> List<T> getTelemetryDataByType(String type) {
    return getTelemetryDataByType(type, false);
  }

  private <T extends Domain> List<T> getTelemetryDataByType(String type, boolean inRequestOnly) {
    Objects.requireNonNull(type, "type");
    List<Envelope> items = getItemsEnvelopeDataType(type);
    List<T> dataItems = new ArrayList<>();
    for (Envelope e : items) {
      if (!inRequestOnly || e.getTags().containsKey("ai.operation.id")) {
        @SuppressWarnings("unchecked")
        Data<T> dt = (Data<T>) e.getData();
        dataItems.add(dt.getBaseData());
      }
    }
    return dataItems;
  }

  public <T extends Domain> List<T> getTelemetryDataByTypeInRequest(String type) {
    return getTelemetryDataByType(type, true);
  }

  public <T extends Domain> List<T> getMessageDataInRequest(int numItems)
      throws ExecutionException, InterruptedException, TimeoutException {
    List<Envelope> items =
        waitForItems("MessageData", e -> e.getTags().containsKey("ai.operation.id"), numItems);
    List<T> dataItems = new ArrayList<>();
    for (Envelope e : items) {
      @SuppressWarnings("unchecked")
      Data<T> dt = (Data<T>) e.getData();
      dataItems.add(dt.getBaseData());
    }
    return dataItems;
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  public <T extends Domain> T getBaseDataForType(int index, String type) {
    @SuppressWarnings("unchecked")
    Data<T> data = (Data<T>) getItemsEnvelopeDataType(type).get(index).getData();
    return data.getBaseData();
  }

  public void awaitAnyItems(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    servlet.awaitAnyItems(timeout, unit);
  }

  /**
   * Waits the given amount of time for this mocked server to recieve one telemetry item matching
   * the given predicate.
   *
   * @see #waitForItems(Predicate, int, int, TimeUnit)
   */
  public Envelope waitForItem(Predicate<Envelope> condition, int timeout, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return waitForItems(condition, 1, timeout, timeUnit).get(0);
  }

  public List<Envelope> waitForItems(String type, int numItems)
      throws ExecutionException, InterruptedException, TimeoutException {
    return waitForItems(type, numItems, null);
  }

  public List<Envelope> waitForItems(String type, Predicate<Envelope> condition, int numItems)
      throws ExecutionException, InterruptedException, TimeoutException {
    return waitForItems(type, numItems, null, condition);
  }

  // if operationId is null, then matches all items, otherwise only matches items with that
  // operationId
  public List<Envelope> waitForItems(String type, int numItems, @Nullable String operationId)
      throws InterruptedException, ExecutionException, TimeoutException {
    return waitForItems(type, numItems, operationId, envelope -> true);
  }

  public List<Envelope> waitForItems(
      String type, int numItems, @Nullable String operationId, Predicate<Envelope> condition)
      throws InterruptedException, ExecutionException, TimeoutException {
    List<Envelope> items =
        waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                if (!input.getData().getBaseType().equals(type)) {
                  return false;
                }
                if (operationId != null
                    && !operationId.equals(input.getTags().get("ai.operation.id"))) {
                  return false;
                }
                return condition.test(input);
              }
            },
            numItems,
            10,
            TimeUnit.SECONDS);
    if (items.size() > numItems) {
      throw new AssertionError(
          "Expecting " + numItems + " of type " + type + ", but received " + items.size());
    }
    return items;
  }

  /**
   * Waits the given amount of time for this mocked server to receive a certain number of items
   * which match the given predicate.
   *
   * @param condition condition describing what items to wait for.
   * @param numItems number of matching items to wait for.
   * @param timeout amount of time to wait
   * @param timeUnit the unit of time to wait
   * @return The items the given condition. This will be at least {@code numItems}, but could be
   *     more.
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws ExecutionException if an exception is thrown while waiting
   * @throws TimeoutException if the timeout is reached
   */
  public List<Envelope> waitForItems(
      Predicate<Envelope> condition, int numItems, int timeout, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return servlet.waitForItems(condition, numItems, timeout, timeUnit);
  }

  public List<Envelope> waitForMetricItems(String metricName, int numItems)
      throws InterruptedException, TimeoutException {
    return waitForMetricItems(metricName, numItems, 10, TimeUnit.SECONDS);
  }

  public List<Envelope> waitForMetricItems(
      String metricName, String secondPredicate, int numItems, boolean isRolename)
      throws InterruptedException, TimeoutException {
    return waitForMetricItems(
        metricName, secondPredicate, numItems, 10, TimeUnit.SECONDS, isRolename);
  }

  public List<Envelope> waitForMetricItems(
      String metricName, int numItems, int timeout, TimeUnit timeUnit)
      throws InterruptedException, TimeoutException {
    return servlet.waitForItems(
        SmokeTestExtension.getMetricPredicate(metricName), numItems, timeout, timeUnit);
  }

  public List<Envelope> waitForMetricItems(
      String metricName,
      String secondPredicate,
      int numItems,
      int timeout,
      TimeUnit timeUnit,
      boolean isRolename)
      throws InterruptedException, TimeoutException {
    return servlet.waitForItems(
        SmokeTestExtension.getMetricPredicate(metricName, secondPredicate, isRolename),
        numItems,
        timeout,
        timeUnit);
  }

  public List<Envelope> waitForStandardMetricItems(String metricId, int numItems)
      throws InterruptedException, TimeoutException {
    return servlet.waitForItems(
        SmokeTestExtension.getStandardMetricPredicate(metricId), numItems, 70, TimeUnit.SECONDS);
  }

  // this is important for Message and Exception types which can also be captured outside of
  // requests
  public List<Envelope> waitForItemsInOperation(String type, int numItems, String operationId)
      throws ExecutionException, InterruptedException, TimeoutException {
    return waitForItems(type, numItems, operationId);
  }

  public List<Envelope> waitForItemsInOperation(
      String type, int numItems, String operationId, Predicate<Envelope> condition)
      throws ExecutionException, InterruptedException, TimeoutException {
    return waitForItems(type, numItems, operationId, condition);
  }

  // this is used to filter out some sporadic messages that are captured via java.util.logging
  // instrumentation
  public List<Envelope> waitForMessageItemsInRequest(int numItems, String operationId)
      throws ExecutionException, InterruptedException, TimeoutException {
    List<Envelope> items =
        waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                return input.getData().getBaseType().equals("MessageData")
                    && operationId.equals(input.getTags().get("ai.operation.id"));
              }
            },
            numItems,
            10,
            TimeUnit.SECONDS);
    if (items.size() > numItems) {
      throw new AssertionError(
          "Expecting " + numItems + " of type MessageData, but received " + items.size());
    }
    return items;
  }

  public boolean isReceivingLiveMetrics() {
    return quickPulseServlet.isReceivingLiveMetrics();
  }

  public LiveMetricsVerifier getLiveMetrics() {
    return quickPulseServlet.getVerifier();
  }

  @SuppressWarnings("SystemOut")
  public static void main(String[] args) throws Exception {
    MockedAppInsightsIngestionServer i = new MockedAppInsightsIngestionServer(false);
    System.out.println("Starting mocked ingestion on port " + DEFAULT_PORT);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                new Runnable() {
                  @Override
                  public void run() {
                    try {
                      i.stopServer();
                    } catch (Exception e) {
                      e.printStackTrace();
                      throw new IllegalStateException(e);
                    }
                  }
                }));
    i.startServer();
  }

  public void setRequestLoggingEnabled(boolean enabled) {
    servlet.setRequestLoggingEnabled(enabled);
  }

  public void setQuickPulseRequestLoggingEnabled(boolean enabled) {
    quickPulseServlet.setRequestLoggingEnabled(enabled);
  }
}
