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

package com.microsoft.applicationinsights.internal.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public final class InternalLoggerTest {

  private static final String WRONG_LOGGER_OUTPUT_NAME = "wrong";
  private static final String WRONG_LOGGER_LEVEL_NAME = "wrong";

  private final TestLoggerOutput testLoggerOutput = new TestLoggerOutput();

  @Before
  public void prepare() throws NoSuchFieldException, IllegalAccessException {
    Field field = InternalLogger.class.getDeclaredField("initialized");
    field.setAccessible(true);
    field.set(InternalLogger.INSTANCE, false);

    forceLoggingLevel(InternalLogger.LoggingLevel.OFF);
    forceLoggerOutput(null);
  }

  private void forceLoggingLevel(InternalLogger.LoggingLevel level)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = InternalLogger.class.getDeclaredField("loggingLevel");
    field.setAccessible(true);
    field.set(InternalLogger.INSTANCE, level);
  }

  private void forceLoggerOutput(LoggerOutput output)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = InternalLogger.class.getDeclaredField("loggerOutput");
    field.setAccessible(true);
    field.set(InternalLogger.INSTANCE, output);
  }

  @Test
  public void testNoLoggerLevelData() {
    final Map<String, String> loggerData = new HashMap<String, String>();
    InternalLogger.INSTANCE.initialize(
        InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

    assertTrue(InternalLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testWrongLoggerLevelName() {
    final Map<String, String> loggerData = new HashMap<String, String>();
    loggerData.put("Level", WRONG_LOGGER_LEVEL_NAME);
    InternalLogger.INSTANCE.initialize(
        InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

    assertFalse(InternalLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testWrongLoggerOutputName() {
    final Map<String, String> loggerData = new HashMap<String, String>();
    loggerData.put("Level", InternalLogger.LoggingLevel.TRACE.toString());
    InternalLogger.INSTANCE.initialize(WRONG_LOGGER_OUTPUT_NAME, loggerData);

    assertFalse(InternalLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testInitializeTwice() {
    final Map<String, String> loggerData = new HashMap<String, String>();
    loggerData.put("Level", InternalLogger.LoggingLevel.ERROR.toString());
    InternalLogger.INSTANCE.initialize(
        InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

    loggerData.put("Level", InternalLogger.LoggingLevel.WARN.toString());
    InternalLogger.INSTANCE.initialize(
        InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);

    assertTrue(InternalLogger.INSTANCE.isErrorEnabled());
    assertFalse(InternalLogger.INSTANCE.isWarnEnabled());
  }

  @Test
  public void testLogAlwaysWithLoggerLevelOff() {
    final Map<String, String> loggerData = new HashMap<String, String>();
    loggerData.put("Level", InternalLogger.LoggingLevel.OFF.toString());
    InternalLogger.INSTANCE.initialize(
        InternalLogger.LoggerOutputType.CONSOLE.toString(), loggerData);
    InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "hey");
  }

  @Test // this is very dependent on the format string.
  public void loggerDateFormatIncludesMilliseconds()
      throws NoSuchFieldException, IllegalAccessException {
    InternalLogger.INSTANCE.initialize(
        LoggerOutputType.CONSOLE.toString(), new HashMap<String, String>());
    forceLoggerOutput(testLoggerOutput);
    InternalLogger.INSTANCE.info("T3$t");
    List<String> messages = testLoggerOutput.getMessages();
    assertEquals(1, messages.size());
    String message = messages.get(0);
    System.out.println(message);
    assertTrue(message.contains("T3$t"));

    String[] parts = message.split("\\s+");
    String time = parts[2];
    String[] timeParts = time.split(":");
    assertEquals(3, timeParts.length);

    int dotIndex = timeParts[2].indexOf('.');
    assertEquals(2, dotIndex);
  }

  private static class TestLoggerOutput implements LoggerOutput {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void log(String message) {
      messages.add(message);
    }

    @Override
    public void close() {
      clear();
    }

    public void clear() {
      messages.clear();
    }

    public List<String> getMessages() {
      return new ArrayList<>(messages);
    }
  }
}
