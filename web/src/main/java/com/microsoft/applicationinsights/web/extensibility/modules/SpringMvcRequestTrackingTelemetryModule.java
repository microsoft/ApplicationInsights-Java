package com.microsoft.applicationinsights.web.extensibility.modules;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class SpringMvcRequestTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {

    private TelemetryClient telemetryClient;
    private List<String> exceptionAttributesList = asList(DispatcherServlet.class.getName() + ".EXCEPTION");
    private String exceptionAttributes;

    @Override
    public void initialize(TelemetryConfiguration configuration) {
        telemetryClient = new TelemetryClient(configuration);
    }

    @Override
    public void onBeginRequest(ServletRequest req, ServletResponse res) {

    }

    @Override
    public void onEndRequest(ServletRequest req, ServletResponse res) {
        for (String exceptionAttribute : exceptionAttributesList) {
            Exception exception = (Exception) req.getAttribute(exceptionAttribute);
            if (exception != null) {
                telemetryClient.trackException(exception);
            }
        }
    }

    public TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    public void setTelemetryClient(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    public String getExceptionAttributes() {
        return exceptionAttributes;
    }

    public void setExceptionAttributes(String exceptionAttributes) {
        try {
            if (StringUtils.isBlank(exceptionAttributes)) {
                return;
            }
            String[] attributes = exceptionAttributes.split(",");
            exceptionAttributesList = new ArrayList<>(attributes.length);
            for (String attribute : attributes) {
                if (!StringUtils.isBlank(exceptionAttributes)) {
                    exceptionAttributesList.add(attribute);
                }
            }
            InternalLogger.INSTANCE.trace(String.format("SpringMvcRequestTrackingTelemetryModule: set ExceptionAttributes: %s", exceptionAttributes));
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, String.format("SpringMvcRequestTrackingTelemetryModule: failed to parse ExceptionAttributes: %s", exceptionAttributes));
            throw t;
        }

    }
}
