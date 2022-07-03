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

package com.microsoft.applicationinsights.smoketest.fakeingestion;

import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private final List<Predicate<Envelope>> filters;

  private final Object multimapLock = new Object();

  private final ExecutorService itemExecutor = Executors.newSingleThreadExecutor();

  MockedAppInsightsIngestionServlet() {
    type2envelope = MultimapBuilder.treeKeys().arrayListValues().build();
    filters = new ArrayList<>();
  }

  void addIngestionFilter(Predicate<Envelope> filter) {
    this.filters.add(filter);
  }

  void resetData() {
    synchronized (multimapLock) {
      type2envelope.clear();
    }
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
      throws InterruptedException, ExecutionException, TimeoutException {
    Future<List<Envelope>> future =
        itemExecutor.submit(
            () -> {
              List<Envelope> targetCollection = new ArrayList<>(numItems);
              while (targetCollection.size() < numItems) {
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
                TimeUnit.MILLISECONDS.sleep(75);
              }
              return targetCollection;
            });
    return future.get(timeout, timeUnit);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (!"/v2.1/track".equals(req.getPathInfo())) {
      resp.sendError(404, "Unknown URI");
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

    String[] lines = body.split("\n");
    for (String line : lines) {
      Envelope envelope = JsonHelper.GSON.fromJson(line.trim(), Envelope.class);
      String baseType = envelope.getData().getBaseType();
      if (filtersAllowItem(envelope)) {
        synchronized (multimapLock) {
          type2envelope.put(baseType, envelope);
        }
      }
    }
  }

  private boolean filtersAllowItem(Envelope item) {
    if (this.filters.isEmpty()) {
      return true;
    }
    for (Predicate<Envelope> filter : this.filters) {
      if (!filter.test(item)) {
        return false;
      }
    }
    return true;
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
}
