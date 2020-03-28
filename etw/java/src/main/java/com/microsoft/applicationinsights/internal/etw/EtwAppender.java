package com.microsoft.applicationinsights.internal.etw;

import java.util.Map;

import com.microsoft.applicationinsights.agentc.internal.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.internal.etw.events.IpaCritical;
import com.microsoft.applicationinsights.internal.etw.events.IpaError;
import com.microsoft.applicationinsights.internal.etw.events.IpaInfo;
import com.microsoft.applicationinsights.internal.etw.events.IpaWarn;
import com.microsoft.applicationinsights.internal.etw.events.model.IpaEtwEventBase;

import org.apache.commons.lang3.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class EtwAppender extends AppenderBase<ILoggingEvent> {

    private final EtwProvider etwProvider = new EtwProvider();
    private final IpaEtwEventBase proto = new IpaInfo();

    public EtwAppender() {
        ApplicationMetadataFactory metadata = DiagnosticsHelper.getMetadataFactory();
        // TODO siteName == appName ???
        proto.setAppName(metadata.getSiteName().getValue());
        proto.setExtensionVersion(metadata.getExtensionVersion().getValue());
        proto.setSubscriptionId(metadata.getSubscriptionId().getValue());
        proto.setInstrumentationKey(metadata.getInstrumentationKey().getValue());
        proto.setResourceType(StringUtils.defaultString(DiagnosticsHelper.getCodelessResourceType(), "unknown"));
    }

    @Override
    protected void append(ILoggingEvent logEvent) {
        Level level = logEvent.getLevel();
        IpaEtwEventBase event;
        // empty if no throwable
        switch (level.levelInt) {
            case Level.ERROR_INT:
                IpaError error = new IpaError(proto);
                error.setStacktrace(logEvent.getThrowableProxy());
                event = error;
                break;
            case Level.WARN_INT:
                IpaWarn warn = new IpaWarn(proto);
                warn.setStacktrace(logEvent.getThrowableProxy());
                event = warn;
                break;
            case Level.INFO_INT:
                event = new IpaInfo(proto);
                break;
            default:
                addWarn("Unsupported log level: "+level.levelStr);
                return;
        }

        Map<String, String> mdcPropertyMap = logEvent.getMDCPropertyMap();

        // TODO should this timestamp be included?
        // long timeStamp = logEvent.getTimeStamp();
        if (!mdcPropertyMap.isEmpty()) {
            String operation = mdcPropertyMap.get(DiagnosticsHelper.MDC_PROP_OPERATION);
            if (StringUtils.isNotEmpty(operation)) {
                event.setOperation(operation);
            }
            String etwCritical = mdcPropertyMap.get(DiagnosticsHelper.MDC_ETW_CRITICAL);
            if (StringUtils.isNotEmpty(etwCritical) && Boolean.parseBoolean(etwCritical)) {
                event = new IpaCritical(event);
            }
        }
        event.setLogger(logEvent.getLoggerName());
        event.setMessageFormat(logEvent.getMessage());
        event.setMessageArgs(logEvent.getArgumentArray());
        try {
            etwProvider.writeEvent(event);
        } catch (ApplicationInsightsEtwException e) {
            addError("Exception from EtwProvider: "+e.getLocalizedMessage(), e);
        }
    }

}