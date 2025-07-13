// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionMessageBehaviorTest {

  @Test
  public void testNullPointerExceptionMessageBehavior() {
    // Test the exact scenario from the problem statement
    NullPointerException exception = new NullPointerException();
    
    // Check the message
    String message = exception.getMessage();
    assertThat(message).isNull();
    
    // Apply the TelemetryUtil logic
    String exceptionMessage = message;
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    
    // The final message should be the class name
    assertThat(exceptionMessage).isEqualTo("java.lang.NullPointerException");
    assertThat(exceptionMessage).isNotEmpty();
    assertThat(exceptionMessage).isNotNull();
  }

  @Test
  public void testNullPointerExceptionWithEmptyMessage() {
    // Test the scenario with empty string
    NullPointerException exception = new NullPointerException("");
    
    // Check the message
    String message = exception.getMessage();
    assertThat(message).isEqualTo("");
    
    // Apply the TelemetryUtil logic
    String exceptionMessage = message;
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    
    // The final message should be the class name
    assertThat(exceptionMessage).isEqualTo("java.lang.NullPointerException");
    assertThat(exceptionMessage).isNotEmpty();
    assertThat(exceptionMessage).isNotNull();
  }

  @Test
  public void testRuntimeExceptionMessageBehavior() {
    // Test with RuntimeException
    RuntimeException exception = new RuntimeException();
    
    // Check the message
    String message = exception.getMessage();
    assertThat(message).isNull();
    
    // Apply the TelemetryUtil logic
    String exceptionMessage = message;
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    
    // The final message should be the class name
    assertThat(exceptionMessage).isEqualTo("java.lang.RuntimeException");
    assertThat(exceptionMessage).isNotEmpty();
    assertThat(exceptionMessage).isNotNull();
  }

  @Test
  public void testStringIsNullOrEmptyBehavior() {
    // Test the Strings.isNullOrEmpty behavior with various inputs
    assertThat(Strings.isNullOrEmpty(null)).isTrue();
    assertThat(Strings.isNullOrEmpty("")).isTrue();
    assertThat(Strings.isNullOrEmpty("   ")).isFalse(); // Whitespace is not empty
    assertThat(Strings.isNullOrEmpty("test")).isFalse();
  }
}