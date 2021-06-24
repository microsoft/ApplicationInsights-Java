/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.test.fakeingestion;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MockedAppInsightsIngestionServer {
  public static final int DEFAULT_PORT = 6060;

  private final MockedAppInsightsIngestionServlet servlet;
  private final Server server;

  public MockedAppInsightsIngestionServer() {
    server = new Server(DEFAULT_PORT);
    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);

    servlet = new MockedAppInsightsIngestionServlet();

    handler.addServletWithMapping(new ServletHolder(servlet), "/*");
  }

  public int getPort() {
    return DEFAULT_PORT; // TODO this could be configurable
  }

  @SuppressWarnings("SystemOut")
  public void startServer() throws Exception {
    System.out.println("Starting fake ingestion...");
    server.start();
  }

  @SuppressWarnings("SystemOut")
  public void stopServer() throws Exception {
    System.out.println("Stopping fake ingestion...");
    server.stop();
    server.join();
  }

  public void addIngestionFilter(Predicate<Envelope> filter) {
    this.servlet.addIngestionFilter(filter);
  }

  public void resetData() {
    this.servlet.resetData();
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

  public List<Envelope> getItemsEnvelopeDataType(String type) {
    return this.servlet.getItemsByType(type);
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
        Data<T> dt = (Data<T>) e.getData();
        dataItems.add(dt.getBaseData());
      }
    }
    return dataItems;
  }

  public <T extends Domain> List<T> getTelemetryDataByTypeInRequest(String type) {
    return getTelemetryDataByType(type, true);
  }

  public <T extends Domain> List<T> getMessageDataInRequest() {
    List<Envelope> items = getItemsEnvelopeDataType("MessageData");
    List<T> dataItems = new ArrayList<>();
    for (Envelope e : items) {
      String message = ((MessageData) ((Data<?>) e.getData()).getBaseData()).getMessage();
      if (e.getTags().containsKey("ai.operation.id") && !ignoreMessageData(message)) {
        Data<T> dt = (Data<T>) e.getData();
        dataItems.add(dt.getBaseData());
      }
    }
    return dataItems;
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  public <T extends Domain> T getBaseDataForType(int index, String type) {
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

  // if operationId is null, then matches all items, otherwise only matches items with that
  // operationId
  public List<Envelope> waitForItems(String type, int numItems, String operationId)
      throws InterruptedException, ExecutionException, TimeoutException {
    List<Envelope> items =
        waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                return input.getData().getBaseType().equals(type)
                    && (operationId == null
                        || operationId.equals(input.getTags().get("ai.operation.id")));
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

  // this is important for Message and Exception types which can also be captured outside of
  // requests
  public List<Envelope> waitForItemsInOperation(String type, int numItems, String operationId)
      throws ExecutionException, InterruptedException, TimeoutException {
    return waitForItems(type, numItems, operationId);
  }

  // this is used to filter out some sporadic messages that are captured via java.util.logging
  // instrumentation
  public List<Envelope> waitForMessageItemsInRequest(int numItems)
      throws ExecutionException, InterruptedException, TimeoutException {
    List<Envelope> items =
        waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                if (!input.getData().getBaseType().equals("MessageData")
                    || !input.getTags().containsKey("ai.operation.id")) {
                  return false;
                }
                String message =
                    ((MessageData) ((Data<?>) input.getData()).getBaseData()).getMessage();
                return !ignoreMessageData(message);
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

  private static boolean ignoreMessageData(String message) {
    return message.contains("The profile fetch task will not execute for next")
        || message.contains("pending resolution of instrumentation key");
  }

  @SuppressWarnings("SystemOut")
  public static void main(String[] args) throws Exception {
    MockedAppInsightsIngestionServer i = new MockedAppInsightsIngestionServer();
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
}
