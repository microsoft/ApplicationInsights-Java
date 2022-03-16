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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import io.opentelemetry.instrumentation.api.cache.Cache;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class TelemetryChannel {

  // the number 100 was calculated as the max number of concurrent exports that the single worker
  // thread can drive, so anything higher than this should not increase throughput
  private static final int MAX_CONCURRENT_EXPORTS = 100;

  private static final OperationLogger operationLogger =
      new OperationLogger(
          TelemetryChannel.class, "Put export into the background (don't wait for it to return)");

  private final TelemetryChannelImpl impl;

  private final Set<CompletableResultCode> activeExportResults =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  public static TelemetryChannel create(
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      Cache<String, String> ikeyEndpointMap,
      StatsbeatModule statsbeatModule,
      boolean isStatsbeat,
      @Nullable Configuration.AadAuthentication aadAuthentication) {
    HttpPipeline httpPipeline = LazyHttpClient.newHttpPipeLine(aadAuthentication, ikeyEndpointMap);
    return new TelemetryChannel(
        httpPipeline, endpointUrl, localFileWriter, statsbeatModule, isStatsbeat);
  }

  // used by tests only
  public TelemetryChannel(
      HttpPipeline pipeline,
      URL endpointUrl,
      LocalFileWriter localFileWriter,
      StatsbeatModule statsbeatModule,
      boolean isStatsbeat) {
    impl =
        new TelemetryChannelImpl(
            pipeline, endpointUrl, localFileWriter, statsbeatModule, isStatsbeat);
  }

  public CompletableResultCode sendRawBytes(
      ByteBuffer buffer,
      String instrumentationKey,
      Runnable onSuccess,
      Consumer<Boolean> onFailure) {
    return impl.sendRawBytes(buffer, instrumentationKey, onSuccess, onFailure);
  }

  public CompletableResultCode send(List<TelemetryItem> telemetryItems) {
    return maybeAddToActiveExportResults(impl.send(telemetryItems));
  }

  private CompletableResultCode maybeAddToActiveExportResults(CompletableResultCode result) {
    if (activeExportResults.size() >= MAX_CONCURRENT_EXPORTS) {
      // this is just a failsafe to limit concurrent exports, it's not ideal because it blocks
      // waiting for the most recent export instead of waiting for the first export to return
      operationLogger.recordFailure(
          "Hit max " + MAX_CONCURRENT_EXPORTS + " active concurrent requests");
      return result;
    }

    operationLogger.recordSuccess();

    activeExportResults.add(result);
    result.whenComplete(() -> activeExportResults.remove(result));

    return CompletableResultCode.ofSuccess();
  }

  public CompletableResultCode flush() {
    return CompletableResultCode.ofAll(activeExportResults);
  }
}
