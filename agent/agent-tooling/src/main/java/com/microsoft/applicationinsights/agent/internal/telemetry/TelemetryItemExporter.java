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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryItemExporter {

  private static final Logger logger = LoggerFactory.getLogger(TelemetryItemExporter.class);

  private static final ObjectMapper mapper = createObjectMapper();

  private static final AppInsightsByteBufferPool byteBufferPool = new AppInsightsByteBufferPool();

  private static final OperationLogger encodeBatchOperationLogger =
      new OperationLogger(TelemetryItemExporter.class, "Encoding telemetry batch into json");

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // it's important to pass in the "agent class loader" since TelemetryItemPipeline is initialized
    // lazily and can be initialized via an application thread, in which case the thread context
    // class loader is used to look up jsr305 module and its not found
    mapper.registerModules(ObjectMapper.findModules(TelemetryItemExporter.class.getClassLoader()));
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return mapper;
  }

  private final TelemetryPipeline byteBufferPipeline;
  private final TelemetryPipelineListener listener;

  // e.g. construct with diagnostic listener and local storage listener
  public TelemetryItemExporter(
      TelemetryPipeline byteBufferPipeline, TelemetryPipelineListener listener) {
    this.byteBufferPipeline = byteBufferPipeline;
    this.listener = listener;
  }

  public CompletableResultCode send(List<TelemetryItem> telemetryItems) {
    Map<String, List<TelemetryItem>> instrumentationKeyMap = new HashMap<>();
    List<CompletableResultCode> resultCodeList = new ArrayList<>();
    for (TelemetryItem telemetryItem : telemetryItems) {
      String instrumentationKey = telemetryItem.getInstrumentationKey();
      if (!instrumentationKeyMap.containsKey(instrumentationKey)) {
        instrumentationKeyMap.put(instrumentationKey, new ArrayList<>());
      }
      instrumentationKeyMap.get(instrumentationKey).add(telemetryItem);
    }
    for (String instrumentationKey : instrumentationKeyMap.keySet()) {
      resultCodeList.add(
          internalSendByInstrumentationKey(
              instrumentationKeyMap.get(instrumentationKey), instrumentationKey));
    }
    return CompletableResultCode.ofAll(resultCodeList);
  }

  CompletableResultCode internalSendByInstrumentationKey(
      List<TelemetryItem> telemetryItems, String instrumentationKey) {
    List<ByteBuffer> byteBuffers;
    try {
      byteBuffers = encode(telemetryItems);
      encodeBatchOperationLogger.recordSuccess();
    } catch (Throwable t) {
      encodeBatchOperationLogger.recordFailure(
          "Error encoding telemetry items: " + t.getMessage(), t);
      return CompletableResultCode.ofFailure();
    }
    return byteBufferPipeline.send(byteBuffers, instrumentationKey, listener);
  }

  List<ByteBuffer> encode(List<TelemetryItem> telemetryItems) throws IOException {

    if (logger.isDebugEnabled()) {
      StringWriter debug = new StringWriter();
      try (JsonGenerator jg = mapper.createGenerator(debug)) {
        writeTelemetryItems(jg, telemetryItems);
      }
      logger.debug("sending telemetry to ingestion service:\n{}", debug);
    }

    ByteBufferOutputStream out = new ByteBufferOutputStream(byteBufferPool);

    try (JsonGenerator jg = mapper.createGenerator(new GZIPOutputStream(out))) {
      writeTelemetryItems(jg, telemetryItems);
    } catch (IOException e) {
      byteBufferPool.offer(out.getByteBuffers());
      throw e;
    }

    out.close(); // closing ByteBufferOutputStream is a no-op, but this line makes LGTM happy

    List<ByteBuffer> byteBuffers = out.getByteBuffers();
    for (ByteBuffer byteBuffer : byteBuffers) {
      byteBuffer.flip();
    }
    return byteBuffers;
  }

  private static void writeTelemetryItems(JsonGenerator jg, List<TelemetryItem> telemetryItems)
      throws IOException {
    jg.setRootValueSeparator(new SerializedString("\n"));
    for (TelemetryItem telemetryItem : telemetryItems) {
      mapper.writeValue(jg, telemetryItem);
    }
  }
}
