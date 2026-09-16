// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.monitor.opentelemetry.exporter.implementation.localstorage.LocalStorageTelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineResponse;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyTransactionTelemetryPipelineListener implements TelemetryPipelineListener {

  private static final Logger logger =
      LoggerFactory.getLogger(KeyTransactionTelemetryPipelineListener.class);

  private static final OperationLogger operationLogger =
      new OperationLogger(TelemetryItemExporter.class, "Parsing response from ingestion service");

  @Override
  public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {

    if (logger.isDebugEnabled()) {
      logger.debug("request: {}", requestToString(request));
      logger.debug("response: {}", response.getBody());
    }

    NewResponse parsedResponse;
    try (JsonReader reader = JsonProviders.createReader(response.getBody())) {
      parsedResponse = NewResponse.fromJson(reader);
      operationLogger.recordSuccess();
    } catch (IOException e) {
      operationLogger.recordFailure(e.getMessage(), e);
      return;
    }

    if (parsedResponse.getSdkConfigurations() != null
        && !KeyTransactionConfigSupplier.USE_HARDCODED_CONFIG) {
      KeyTransactionConfigSupplier.getInstance()
          .set(
              parsedResponse.getSdkConfigurations().stream()
                  .map(SdkConfiguration::getValue)
                  .collect(toList()));
    }
  }

  @Override
  public void onException(
      TelemetryPipelineRequest telemetryPipelineRequest, String s, Throwable throwable) {
    // ignore
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private static String requestToString(TelemetryPipelineRequest request) {
    List<ByteBuffer> originalByteBuffers = request.getByteBuffers();
    byte[] gzippedBytes = convertByteBufferListToByteArray(originalByteBuffers);
    byte[] ungzippedBytes = LocalStorageTelemetryPipelineListener.ungzip(gzippedBytes);
    return new String(ungzippedBytes, UTF_8);
  }

  // convert a list of byte buffers to a big byte array
  private static byte[] convertByteBufferListToByteArray(List<ByteBuffer> byteBuffers) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (ByteBuffer buffer : byteBuffers) {
      byte[] arr = new byte[buffer.remaining()];
      buffer.get(arr);
      try {
        baos.write(arr);
      } catch (IOException e) {
        // this should never happen since ByteArrayOutputStream doesn't throw IOException
        throw new IllegalStateException(e);
      }
    }

    return baos.toByteArray();
  }
}
