// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MessageData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryExceptionData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GenAiPropertyUtilTest {

  @Test
  void extractGenAiAttributesReturnsEmptyMapWhenNoGenAiAttributes() {
    Attributes attributes =
        Attributes.builder()
            .put("http.method", "GET")
            .put("http.url", "http://example.com")
            .build();

    Map<String, String> result = GenAiPropertyUtil.extractGenAiAttributes(attributes);

    assertThat(result).isEmpty();
  }

  @Test
  void extractGenAiAttributesExtractsStringAttributes() {
    String longValue = generateLongString(10000);
    Attributes attributes =
        Attributes.builder()
            .put("gen_ai.input.messages", longValue)
            .put("http.method", "GET")
            .build();

    Map<String, String> result = GenAiPropertyUtil.extractGenAiAttributes(attributes);

    assertThat(result).hasSize(1);
    assertThat(result).containsEntry("gen_ai.input.messages", longValue);
    assertThat(result.get("gen_ai.input.messages")).hasSize(10000);
  }

  @Test
  void extractGenAiAttributesExtractsAllGenAiKeys() {
    Attributes attributes =
        Attributes.builder()
            .put("gen_ai.input.messages", "input")
            .put("gen_ai.output.messages", "output")
            .put("gen_ai.system_instructions", "instructions")
            .put("gen_ai.tool.definitions", "definitions")
            .put("gen_ai.tool.call.arguments", "arguments")
            .put("gen_ai.tool.call.result", "result")
            .put("gen_ai.evaluation.explanation", "explanation")
            .build();

    Map<String, String> result = GenAiPropertyUtil.extractGenAiAttributes(attributes);

    assertThat(result).hasSize(7);
    assertThat(result).containsEntry("gen_ai.input.messages", "input");
    assertThat(result).containsEntry("gen_ai.output.messages", "output");
    assertThat(result).containsEntry("gen_ai.system_instructions", "instructions");
    assertThat(result).containsEntry("gen_ai.tool.definitions", "definitions");
    assertThat(result).containsEntry("gen_ai.tool.call.arguments", "arguments");
    assertThat(result).containsEntry("gen_ai.tool.call.result", "result");
    assertThat(result).containsEntry("gen_ai.evaluation.explanation", "explanation");
  }

  @Test
  void extractGenAiAttributesHandlesStringArrays() {
    Attributes attributes =
        Attributes.builder()
            .put(
                AttributeKey.stringArrayKey("gen_ai.input.messages"),
                Arrays.asList("message1", "message2", "message3"))
            .build();

    Map<String, String> result = GenAiPropertyUtil.extractGenAiAttributes(attributes);

    assertThat(result).hasSize(1);
    assertThat(result).containsEntry("gen_ai.input.messages", "message1,message2,message3");
  }

  @Test
  void extractGenAiAttributesIgnoresNonGenAiAttributes() {
    Attributes attributes =
        Attributes.builder()
            .put("gen_ai.input.messages", "input")
            .put("http.method", "GET")
            .put("db.statement", "SELECT 1")
            .build();

    Map<String, String> result = GenAiPropertyUtil.extractGenAiAttributes(attributes);

    assertThat(result).hasSize(1);
    assertThat(result).containsKey("gen_ai.input.messages");
    assertThat(result).doesNotContainKey("http.method");
    assertThat(result).doesNotContainKey("db.statement");
  }

  @Test
  void restoreGenAiPropertiesRestoresValuesInMessageData() {
    TelemetryItem item = createTelemetryItem(new MessageData());
    String longValue = generateLongString(10000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.input.messages", longValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties = ((MessageData) item.getData().getBaseData()).getProperties();
    assertThat(properties).containsEntry("gen_ai.input.messages", longValue);
    assertThat(properties.get("gen_ai.input.messages")).hasSize(10000);
  }

  @Test
  void restoreGenAiPropertiesRestoresValuesInRequestData() {
    TelemetryItem item = createTelemetryItem(new RequestData());
    String longValue = generateLongString(15000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.output.messages", longValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties = ((RequestData) item.getData().getBaseData()).getProperties();
    assertThat(properties).containsEntry("gen_ai.output.messages", longValue);
    assertThat(properties.get("gen_ai.output.messages")).hasSize(15000);
  }

  @Test
  void restoreGenAiPropertiesRestoresValuesInRemoteDependencyData() {
    TelemetryItem item = createTelemetryItem(new RemoteDependencyData());
    String longValue = generateLongString(20000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.tool.call.arguments", longValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties =
        ((RemoteDependencyData) item.getData().getBaseData()).getProperties();
    assertThat(properties).containsEntry("gen_ai.tool.call.arguments", longValue);
    assertThat(properties.get("gen_ai.tool.call.arguments")).hasSize(20000);
  }

  @Test
  void restoreGenAiPropertiesRestoresValuesInTelemetryExceptionData() {
    TelemetryItem item = createTelemetryItem(new TelemetryExceptionData());
    String longValue = generateLongString(10000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.evaluation.explanation", longValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties =
        ((TelemetryExceptionData) item.getData().getBaseData()).getProperties();
    assertThat(properties).containsEntry("gen_ai.evaluation.explanation", longValue);
  }

  @Test
  void restoreGenAiPropertiesRestoresValuesInTelemetryEventData() {
    TelemetryItem item = createTelemetryItem(new TelemetryEventData());
    String longValue = generateLongString(10000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.system_instructions", longValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties =
        ((TelemetryEventData) item.getData().getBaseData()).getProperties();
    assertThat(properties).containsEntry("gen_ai.system_instructions", longValue);
  }

  @Test
  void restoreGenAiPropertiesOverwritesTruncatedValues() {
    MessageData messageData = new MessageData();
    // Simulate what the mapper does: add a truncated value
    String truncated = generateLongString(8192);
    Map<String, String> existingProperties = new HashMap<>();
    existingProperties.put("gen_ai.input.messages", truncated);
    existingProperties.put("other.property", "value");
    messageData.setProperties(existingProperties);

    TelemetryItem item = createTelemetryItem(messageData);

    String fullValue = generateLongString(15000);
    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.input.messages", fullValue);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties = ((MessageData) item.getData().getBaseData()).getProperties();
    assertThat(properties.get("gen_ai.input.messages")).hasSize(15000);
    assertThat(properties.get("gen_ai.input.messages")).isEqualTo(fullValue);
    assertThat(properties).containsEntry("other.property", "value");
  }

  @Test
  void restoreGenAiPropertiesHandlesMultipleGenAiAttributes() {
    TelemetryItem item = createTelemetryItem(new MessageData());
    String longInput = generateLongString(10000);
    String longOutput = generateLongString(12000);

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.input.messages", longInput);
    genAiValues.put("gen_ai.output.messages", longOutput);

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);

    Map<String, String> properties = ((MessageData) item.getData().getBaseData()).getProperties();
    assertThat(properties.get("gen_ai.input.messages")).hasSize(10000);
    assertThat(properties.get("gen_ai.output.messages")).hasSize(12000);
  }

  @Test
  void restoreGenAiPropertiesHandlesNullData() {
    TelemetryItem item = new TelemetryItem();
    // item.getData() is null - should not throw

    Map<String, String> genAiValues = new HashMap<>();
    genAiValues.put("gen_ai.input.messages", "value");

    GenAiPropertyUtil.restoreGenAiProperties(item, genAiValues);
    // no exception thrown
  }

  private static TelemetryItem createTelemetryItem(MonitorDomain baseData) {
    TelemetryItem item = new TelemetryItem();
    MonitorBase data = new MonitorBase();
    data.setBaseData(baseData);
    item.setData(data);
    return item;
  }

  private static String generateLongString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append((char) ('a' + (i % 26)));
    }
    return sb.toString();
  }
}
