// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;

public class ApplicationInsightsDiagnosticsLogFilter extends Filter<ILoggingEvent> {
  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (event.getLevel().toInt() >= Level.WARN_INT
        || DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME.equals(event.getLoggerName())) {
      return FilterReply
          .NEUTRAL; // accept logs if error level or if they are from the diagnostic logger
    } else {
      return FilterReply.DENY; // unless disabled or outside of diagnostics logging environment
    }
  }
}
