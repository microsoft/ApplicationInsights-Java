// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
    // convert both windows and linux newlines just to be safe
    return str.replace("\r\n", " ").replace("\n", " ").replace('\"', '\'');
  }
}
