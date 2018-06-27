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

package com.microsoft.applicationinsights.log4j.v1_2.internal;

import com.microsoft.applicationinsights.internal.common.ApplicationInsightsEvent;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

public final class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

  private LoggingEvent loggingEvent;

  public ApplicationInsightsLogEvent(LoggingEvent event) {
    this.loggingEvent = event;
  }

  @Override
  public String getMessage() {
    String message = this.loggingEvent.getRenderedMessage();

    return message != null ? message : "Log4j Trace";
  }

  @Override
  public boolean isException() {
    return this.loggingEvent.getThrowableInformation() != null;
  }

  @Override
  public Exception getException() {
    Exception exception = null;

    if (isException()) {
      Throwable throwable = this.loggingEvent.getThrowableInformation().getThrowable();
      exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
    }

    return exception;
  }

  @Override
  public Map<String, String> getCustomParameters() {
    Map<String, String> metaData = new HashMap<String, String>();

    metaData.put("SourceType", "Log4j");

    addLogEventProperty("LoggerName", loggingEvent.getLoggerName(), metaData);
    addLogEventProperty(
        "LoggingLevel",
        loggingEvent.getLevel() != null ? loggingEvent.getLevel().toString() : null,
        metaData);
    addLogEventProperty("ThreadName", loggingEvent.getThreadName(), metaData);
    addLogEventProperty("TimeStamp", getFormattedDate(loggingEvent.getTimeStamp()), metaData);

    if (isException()) {
      addLogEventProperty("Logger Message", getMessage(), metaData);
    }

    if (loggingEvent.locationInformationExists()) {
      LocationInfo locationInfo = loggingEvent.getLocationInformation();

      addLogEventProperty("ClassName", locationInfo.getClassName(), metaData);
      addLogEventProperty("FileName", locationInfo.getFileName(), metaData);
      addLogEventProperty("MethodName", locationInfo.getMethodName(), metaData);
      addLogEventProperty("LineNumber", String.valueOf(locationInfo.getLineNumber()), metaData);
    }

    for (Object o : loggingEvent.getProperties().entrySet()) {
      Map.Entry<String, Object> entry = (Map.Entry<String, Object>) o;
      addLogEventProperty(entry.getKey(), entry.getValue().toString(), metaData);
    }

    // TODO: Username, domain and identity should be included as in .NET version.
    // TODO: Should check, seems that it is not included in Log4j2.

    return metaData;
  }

  @Override
  public SeverityLevel getNormalizedSeverityLevel() {
    int log4jLevelAsInt = loggingEvent.getLevel().toInt();
    switch (log4jLevelAsInt) {
      case Priority.FATAL_INT: // FATAL
        return SeverityLevel.Critical;

      case Priority.ERROR_INT: // ERROR
        return SeverityLevel.Error;

      case Priority.WARN_INT: // WARN
        return SeverityLevel.Warning;

      case Priority.INFO_INT: // INFO
        return SeverityLevel.Information;

      case Level.TRACE_INT: // TRACE
      case Priority.DEBUG_INT: // DEBUG
      case Priority.ALL_INT: // ALL
        return SeverityLevel.Verbose;

      default:
        InternalLogger.INSTANCE.error(
            "Unknown Log4j v1.2 option, %d, using TRACE level as default", log4jLevelAsInt);
        return SeverityLevel.Verbose;
    }
  }
}
