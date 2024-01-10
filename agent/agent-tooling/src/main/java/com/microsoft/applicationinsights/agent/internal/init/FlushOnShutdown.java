// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.concurrent.TimeUnit;

public class FlushOnShutdown implements AutoConfigureListener {
  @Override
  public void afterAutoConfigure(OpenTelemetrySdk sdk) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> flushAll(sdk, TelemetryClient.getActive()).join(10, TimeUnit.SECONDS)));
  }

  private static CompletableResultCode flushAll(
      OpenTelemetrySdk sdk, TelemetryClient telemetryClient) {
    CompletableResultCode sdkShutdownResult = sdk.shutdown();
    CompletableResultCode overallResult = new CompletableResultCode();
    sdkShutdownResult.whenComplete(
        () -> {
          // IMPORTANT: the metric reader flush will fail if the periodic metric reader is already
          // mid-exporter
          CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
          telemetryClientResult.whenComplete(
              () -> {
                if (sdkShutdownResult.isSuccess() && telemetryClientResult.isSuccess()) {
                  overallResult.succeed();
                } else {
                  overallResult.fail();
                }
              });
        });
    return overallResult;
  }
}
