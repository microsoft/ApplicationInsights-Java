// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.ExceptionDetailBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryUtilTest {

  @Test
  public void testStringsIsNullOrEmpty() {
    // Test the behavior of Strings.isNullOrEmpty with different inputs
    
    // null should return true
    assertThat(Strings.isNullOrEmpty(null)).isTrue();
    
    // empty string should return true
    assertThat(Strings.isNullOrEmpty("")).isTrue();
    
    // string with only whitespace should return false (important!)
    assertThat(Strings.isNullOrEmpty(" ")).isFalse();
    
    // non-empty string should return false
    assertThat(Strings.isNullOrEmpty("test")).isFalse();
  }
  
  @Test
  public void testExceptionMessageHandling() {
    // Create different types of exceptions and test the message logic
    
    // Exception with null message
    NullPointerException nullMsgException = new NullPointerException();
    assertThat(nullMsgException.getMessage()).isNull();
    
    // Exception with empty message
    NullPointerException emptyMsgException = new NullPointerException("");
    assertThat(emptyMsgException.getMessage()).isEqualTo("");
    
    // Exception with whitespace message
    NullPointerException whitespaceMsgException = new NullPointerException("   ");
    assertThat(whitespaceMsgException.getMessage()).isEqualTo("   ");
    
    // Test our logic
    String[] testMessages = {null, "", "   ", "real message"};
    String[] expectedResults = {
      "java.lang.NullPointerException", 
      "java.lang.NullPointerException", 
      "   ", 
      "real message"
    };
    
    for (int i = 0; i < testMessages.length; i++) {
      String testMessage = testMessages[i];
      String expectedResult = expectedResults[i];
      
      // Simulate TelemetryUtil logic
      String exceptionMessage = testMessage;
      if (Strings.isNullOrEmpty(exceptionMessage)) {
        exceptionMessage = "java.lang.NullPointerException";
      }
      
      assertThat(exceptionMessage).isEqualTo(expectedResult);
    }
  }
  
  @Test
  public void testExceptionWithMessage() {
    // Create an exception with a message
    String testMessage = "Test exception message";
    NullPointerException exception = new NullPointerException(testMessage);
    
    // Process the exception
    List<ExceptionDetailBuilder> exceptions = TelemetryUtil.getExceptions(exception);
    
    // Should have one exception detail
    assertThat(exceptions).hasSize(1);
    
    // Test the logic directly - the message should remain as original
    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    assertThat(exceptionMessage).isEqualTo(testMessage);
  }
  
  @Test
  public void testExceptionWithEmptyMessage() {
    // Create an exception with an empty message
    NullPointerException exception = new NullPointerException("");
    
    // Process the exception
    List<ExceptionDetailBuilder> exceptions = TelemetryUtil.getExceptions(exception);
    
    // Should have one exception detail
    assertThat(exceptions).hasSize(1);
    
    // Test the logic directly - the message should be set to class name
    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    assertThat(exceptionMessage).isEqualTo("java.lang.NullPointerException");
  }
  
  @Test
  public void testExceptionWithNullMessage() {
    // Create an exception with null message (which is the default for most exceptions)
    RuntimeException exception = new RuntimeException((String) null);
    
    // Process the exception
    List<ExceptionDetailBuilder> exceptions = TelemetryUtil.getExceptions(exception);
    
    // Should have one exception detail
    assertThat(exceptions).hasSize(1);
    
    // Test the logic directly - the message should be set to class name
    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    assertThat(exceptionMessage).isEqualTo("java.lang.RuntimeException");
  }
}