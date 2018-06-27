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

package com.microsoft.applicationinsights.log4j.v2.internal;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;

public final class ApplicationInsightsLogEventTest {
  private static void testSeverityLevel(final Level level, SeverityLevel expected) {
    org.apache.logging.log4j.core.LogEvent logEvent =
        new LogEvent() {
          @Override
          public Map<String, String> getContextMap() {
            return null;
          }

          @Override
          public ThreadContext.ContextStack getContextStack() {
            return null;
          }

          @Override
          public String getLoggerFqcn() {
            return null;
          }

          @Override
          public Level getLevel() {
            return level;
          }

          @Override
          public String getLoggerName() {
            return null;
          }

          @Override
          public Marker getMarker() {
            return null;
          }

          @Override
          public Message getMessage() {
            return null;
          }

          @Override
          public long getTimeMillis() {
            return 0;
          }

          @Override
          public StackTraceElement getSource() {
            return null;
          }

          @Override
          public String getThreadName() {
            return null;
          }

          @Override
          public Throwable getThrown() {
            return null;
          }

          @Override
          public ThrowableProxy getThrownProxy() {
            return null;
          }

          @Override
          public boolean isEndOfBatch() {
            return false;
          }

          @Override
          public void setEndOfBatch(boolean endOfBatch) {}

          @Override
          public boolean isIncludeLocation() {
            return false;
          }

          @Override
          public void setIncludeLocation(boolean locationRequired) {}
        };
    ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(logEvent);

    assertEquals(expected, event.getNormalizedSeverityLevel());
  }

  @Test
  public void testFatalSeverityLevel() {
    testSeverityLevel(Level.FATAL, SeverityLevel.Critical);
  }

  @Test
  public void testErrorSeverityLevel() {
    testSeverityLevel(Level.ERROR, SeverityLevel.Error);
  }

  @Test
  public void testInfoSeverityLevel() {
    testSeverityLevel(Level.INFO, SeverityLevel.Information);
  }

  @Test
  public void testWarningSeverityLevel() {
    testSeverityLevel(Level.WARN, SeverityLevel.Warning);
  }

  @Test
  public void testDebugSeverityLevel() {
    testSeverityLevel(Level.DEBUG, SeverityLevel.Verbose);
  }

  @Test
  public void testTraceSeverityLevel() {
    testSeverityLevel(Level.TRACE, SeverityLevel.Verbose);
  }

  @Test
  public void testAllSeverityLevel() {
    testSeverityLevel(Level.ALL, SeverityLevel.Verbose);
  }
}
