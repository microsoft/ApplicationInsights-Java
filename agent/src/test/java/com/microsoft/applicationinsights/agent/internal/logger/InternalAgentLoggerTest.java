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

package com.microsoft.applicationinsights.agent.internal.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class InternalAgentLoggerTest {

  private static PrintStream REAL_SYSOUT;
  private static ByteArrayOutputStream baos = new ByteArrayOutputStream();

  @BeforeClass
  public static void setupSysout() {
    REAL_SYSOUT = System.out;
    System.setOut(new PrintStream(baos, true));
  }

  @AfterClass
  public static void restoreSysout() {
    System.setOut(REAL_SYSOUT);
  }

  @Before
  public void preTest() throws NoSuchFieldException, IllegalAccessException {
    Field field = InternalAgentLogger.class.getDeclaredField("initialized");
    field.setAccessible(true);
    field.set(InternalAgentLogger.INSTANCE, false);

    field = InternalAgentLogger.class.getDeclaredField("loggingLevel");
    field.setAccessible(true);
    field.set(InternalAgentLogger.INSTANCE, InternalAgentLogger.LoggingLevel.OFF);
  }

  @After
  public void postTest() {
    baos.reset();
  }

  @Test
  public void testInitializeWithBadValue() {
    InternalAgentLogger.INSTANCE.initialize("asdf");

    assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testInitializeWithEmptyValue() {
    InternalAgentLogger.INSTANCE.initialize("");

    assertTrue(InternalAgentLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testInitializeWithNullyValue() {
    InternalAgentLogger.INSTANCE.initialize(null);

    assertTrue(InternalAgentLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testNotInitialized() {
    assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
  }

  @Test
  public void testInitializedTwice() {
    InternalAgentLogger.INSTANCE.initialize("WARN");
    InternalAgentLogger.INSTANCE.initialize("TRACE");

    assertTrue(InternalAgentLogger.INSTANCE.isWarnEnabled());
    assertTrue(InternalAgentLogger.INSTANCE.isErrorEnabled());
    assertFalse(InternalAgentLogger.INSTANCE.isTraceEnabled());
  }

  @Test // this is very dependent on the format string.
  public void loggerDateFormatIncludesMilliseconds()
      throws NoSuchFieldException, IllegalAccessException {
    InternalAgentLogger.INSTANCE.initialize("TRACE");
    InternalAgentLogger.INSTANCE.info("T3$t");
    String message = baos.toString();
    REAL_SYSOUT.println(message);
    assertTrue(message.contains("T3$t"));

    String[] parts = message.split("\\s+");
    String time = parts[3];
    String[] timeParts = time.split(":");
    assertEquals(3, timeParts.length);

    int dotIndex = timeParts[2].indexOf('.');
    assertEquals(2, dotIndex);
  }
}
