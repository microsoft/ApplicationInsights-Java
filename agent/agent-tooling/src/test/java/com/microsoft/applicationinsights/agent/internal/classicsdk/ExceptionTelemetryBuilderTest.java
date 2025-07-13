// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionTelemetryBuilderTest {

  @Test
  public void testExceptionTelemetryBuilderWithNullMessage() {
    // Create an exception without a message
    NullPointerException exception = new NullPointerException();
    
    // Create a test telemetry client
    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    
    // Create an exception telemetry builder
    ExceptionTelemetryBuilder telemetryBuilder = telemetryClient.newExceptionTelemetryBuilder();
    
    // Set the exceptions (this calls TelemetryUtil.getExceptions)
    telemetryBuilder.setExceptions(TelemetryUtil.getExceptions(exception));
    
    // Build the telemetry item
    TelemetryItem telemetryItem = telemetryBuilder.build();
    
    // The item should not be null
    assertThat(telemetryItem).isNotNull();
    
    // The item should have data
    assertThat(telemetryItem.getData()).isNotNull();
    assertThat(telemetryItem.getData().getBaseData()).isNotNull();
    
    // This test ensures the telemetry item can be built without throwing exceptions
    // The actual message validation would need to be done in the serialization layer
  }
  
  @Test 
  public void testExceptionTelemetryBuilderWithMessage() {
    // Create an exception with a message
    String testMessage = "Test exception message";
    NullPointerException exception = new NullPointerException(testMessage);
    
    // Create a test telemetry client
    TelemetryClient telemetryClient = TelemetryClient.createForTest();
    
    // Create an exception telemetry builder
    ExceptionTelemetryBuilder telemetryBuilder = telemetryClient.newExceptionTelemetryBuilder();
    
    // Set the exceptions (this calls TelemetryUtil.getExceptions)
    telemetryBuilder.setExceptions(TelemetryUtil.getExceptions(exception));
    
    // Build the telemetry item
    TelemetryItem telemetryItem = telemetryBuilder.build();
    
    // The item should not be null
    assertThat(telemetryItem).isNotNull();
    
    // The item should have data
    assertThat(telemetryItem.getData()).isNotNull();
    assertThat(telemetryItem.getData().getBaseData()).isNotNull();
  }
}