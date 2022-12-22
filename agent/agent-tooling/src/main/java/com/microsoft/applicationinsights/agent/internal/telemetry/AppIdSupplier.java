// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.APP_ID_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.NetworkFriendlyExceptions;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.MDC;

// note: app id is used by jfr profiling
class AppIdSupplier {

  private static final ClientLogger logger = new ClientLogger(AppIdSupplier.class);

  private static final String NEWLINE = System.getProperty("line.separator");

  private final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(AppIdSupplier.class));

  private static final OperationLogger operationLogger =
      new OperationLogger(GetAppIdTask.class, "Retrieving appId");

  // guarded by taskLock
  private GetAppIdTask task;
  private final Object taskLock = new Object();

  @Nullable private volatile String appId;

  // TODO (kryalama) do we still need this AtomicBoolean, or can we use throttling built in to the
  //  warningLogger?
  private static final AtomicBoolean friendlyExceptionThrown = new AtomicBoolean();

  AppIdSupplier() {}

  void updateAppId(ConnectionString connectionString) {
    if (connectionString == null) {
      appId = null;
      if (task != null) {
        // in case prior task is still running (can be called multiple times from JsonConfigPolling)
        task.cancelled = true;
      }
      return;
    }

    GetAppIdTask newTask;
    try {
      newTask = new GetAppIdTask(getAppIdUrl(connectionString));
    } catch (MalformedURLException e) {
      try (MDC.MDCCloseable ignored = APP_ID_ERROR.makeActive()) {
        logger.warning(e.getMessage(), e);
      }
      return;
    }
    synchronized (taskLock) {
      appId = null;
      if (task != null) {
        // in case prior task is still running (can be called multiple times from JsonConfigPolling)
        task.cancelled = true;
      }
      task = newTask;
    }
    scheduledExecutor.submit(newTask);
  }

  // visible for testing
  static URL getAppIdUrl(ConnectionString connectionString) throws MalformedURLException {
    return new URL(
        new URL(connectionString.getIngestionEndpoint()),
        "api/profiles/" + connectionString.getInstrumentationKey() + "/appId");
  }

  String get() {
    // it's possible the appId returned is null (e.g. async task is still pending or has failed). In
    // this case, just return and let the next request resolve the ikey.
    if (appId == null) {
      logger.verbose("appId has not been retrieved yet (e.g. task may be pending or failed)");
      return "";
    }
    return appId;
  }

  private class GetAppIdTask implements Runnable {

    private final URL url;

    // 1, 2, 4, 8, 16, 32, 60 (max)
    private volatile long backoffSeconds = 1;

    private volatile boolean cancelled;

    private GetAppIdTask(URL url) {
      this.url = url;
    }

    @Override
    public void run() {
      if (cancelled) {
        return;
      }

      HttpRequest request = new HttpRequest(HttpMethod.GET, url);
      HttpResponse response;
      try {
        response = LazyHttpClient.getInstance().send(request).block();
      } catch (RuntimeException ex) {
        if (!NetworkFriendlyExceptions.logSpecialOneTimeFriendlyException(
            ex, url.toString(), friendlyExceptionThrown, logger)) {
          operationLogger.recordFailure("exception sending request to " + url, ex, APP_ID_ERROR);
        }
        backOff();
        return;
      }

      if (response == null) {
        // this shouldn't happen, the mono should complete with a response or a failure
        throw new AssertionError("http response mono returned empty");
      }

      String body = response.getBodyAsString().block();
      int statusCode = response.getStatusCode();
      if (statusCode != 200) {
        operationLogger.recordFailure(
            "received " + statusCode + " from " + url + NEWLINE + "full response:" + NEWLINE + body,
            null,
            APP_ID_ERROR);
        backOff();
        return;
      }

      // check for case when breeze returns invalid value
      if (body == null || body.isEmpty()) {
        operationLogger.recordFailure("received empty body from " + url, null, APP_ID_ERROR);
        backOff();
        return;
      }

      operationLogger.recordSuccess();
      appId = body;
    }

    private void backOff() {
      scheduledExecutor.schedule(this, backoffSeconds, SECONDS);
      backoffSeconds = Math.min(backoffSeconds * 2, 60);
    }
  }
}
