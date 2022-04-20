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
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;

public class ApplicationInsightsCsvLayout extends PatternLayout {

  private static final String PREFIX = "LanguageWorkerConsoleLog MS_APPLICATION_INSIGHTS_LOGS";

  private static final ApplicationMetadataFactory applicationMetadataFactory =
      DiagnosticsHelper.getMetadataFactory();

  @Override
  public String doLayout(ILoggingEvent event) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(PREFIX);
    stringBuilder.append(" ");
    stringBuilder.append(event.getTimeStamp());
    stringBuilder.append(",");
    stringBuilder.append(event.getLevel().toString());
    stringBuilder.append(",");
    stringBuilder.append(event.getLoggerName());
    stringBuilder.append(",");
    stringBuilder.append("\"");
    stringBuilder.append(event.getFormattedMessage());
    stringBuilder.append("\"");
    stringBuilder.append(",");
    stringBuilder.append(applicationMetadataFactory.getSiteName().getValue());
    stringBuilder.append(",");
    stringBuilder.append(applicationMetadataFactory.getInstrumentationKey().getValue());
    stringBuilder.append(",");
    stringBuilder.append(applicationMetadataFactory.getExtensionVersion().getValue());
    stringBuilder.append(",");
    stringBuilder.append(applicationMetadataFactory.getSdkVersion().getValue());
    stringBuilder.append(",");
    stringBuilder.append(applicationMetadataFactory.getSubscriptionId().getValue());
    stringBuilder.append(",");
    stringBuilder.append("java");

    return stringBuilder.toString();
  }
}
