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

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FormattedDurationTest {
  @Test
  public void testGetFormattedDurationMilliSeconds() {
    String formattedDuration = FormattedDuration.fromNanos(42657024);
    assertEquals("00:00:00.042657", formattedDuration);
  }

  @Test
  public void testGetFormattedDurationSeconds() {
    String formattedDuration = FormattedDuration.fromNanos(42657024000L);
    assertEquals("00:00:42.657024", formattedDuration);
  }

  @Test
  public void testGetFormattedDurationMinutes() {
    String formattedDuration = FormattedDuration.fromNanos(426570240000L);
    assertEquals("00:07:06.570240", formattedDuration);
  }

  @Test
  public void testGetFormattedDurationHours() {
    String formattedDuration = FormattedDuration.fromNanos(4265702400000L);
    assertEquals("01:11:05.702400", formattedDuration);
  }

  @Test
  public void testGetFormattedDurationDays() {
    String formattedDuration = FormattedDuration.fromNanos(426570240000000L);
    assertEquals("4.22:29:30.240000", formattedDuration);
  }
}
