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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ApplicationInsightsCsvLayout extends PatternLayout {

  private static final String PREFIX = "LanguageWorkerConsoleLogMS_APPLICATION_INSIGHTS_LOGS";

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final ApplicationMetadataFactory applicationMetadataFactory =
      DiagnosticsHelper.getMetadataFactory();
  private final String qualifiedSdkVersion;

  public ApplicationInsightsCsvLayout(String qualifiedSdkVersion) {
    this.qualifiedSdkVersion = qualifiedSdkVersion;
  }

  @Override
  @SuppressFBWarnings(
      value = "ERRMSG", // Information exposure through an error message
      justification = "Error message is not exposed to an end user of the instrumented application")
  public String doLayout(ILoggingEvent event) {
    String message = event.getFormattedMessage();
    IThrowableProxy throwableProxy = event.getThrowableProxy();
    Throwable throwable = null;
    if (throwableProxy instanceof ThrowableProxy) {
      // there is only one other subclass of ch.qos.logback.classic.spi.IThrowableProxy
      // and it is only used for logging exceptions over the wire
      throwable = ((ThrowableProxy) throwableProxy).getThrowable();
    }
    if (throwable != null) {
      message += " ";
      StringWriter sw = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sw, true));
      message += sw;
    }
    return PREFIX
        + " "
        + event.getTimeStamp()
        + ","
        + event.getLevel().toString()
        + ","
        + event.getLoggerName()
        + ","
        + "\""
        + formatForCsv(message)
        + "\""
        + ","
        + applicationMetadataFactory.getSiteName().getValue()
        + ","
        + applicationMetadataFactory.getInstrumentationKey().getValue()
        + ","
        + qualifiedSdkVersion
        + ","
        + "java"
        + System.getProperty("line.separator");
  }

  private static String formatForCsv(String str) {
    return str.replace(LINE_SEPARATOR, " ").replace('\"', '\'');
  }
}
