package com.microsoft.applicationinsights.agentc.internal.diagnostics.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsHelper;

public class DiagnosticsLogEnabledFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (DiagnosticsHelper.shouldOutputDiagnostics()) {
            return FilterReply.NEUTRAL;
        } else {
            return FilterReply.DENY;
        }
    }

}
