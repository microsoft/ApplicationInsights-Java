package com.microsoft.applicationinsights.logging.log4j.v2;

import com.microsoft.applicationinsights.logging.common.TelemetryManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name="ApplicationInsightsAppender", category = "Core", elementType = "appender")
public class ApplicationInsightsAppender extends AbstractAppender {

    //region Members

    private TelemetryManager telemetryManager;

    //endregion Members

    //region Ctor

    /**
     * Constructs new Application Insights appender.
     * @param name The appender name.
     * @param instrumentationKey The instrumentation key.
     */
    protected ApplicationInsightsAppender(String name, String instrumentationKey) {
        super(name, null, null);

        try {
            telemetryManager = new TelemetryManager(instrumentationKey);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            // TODO: Assert.Debug/warning on exception?
        }
    }

    //endregion Ctor

    //region Public methods

    public TelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    /**
     * Creates new appender with the given name and instrumentation key.
     * This method is being called on the application startup upon Log4j system initialization.
     * @param name The appender name.
     * @param instrumentationKey The instrumentation key.
     * @return New Application Insights appender.
     */
    @PluginFactory
    public static ApplicationInsightsAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("instrumentationKey") String instrumentationKey) {

        return new ApplicationInsightsAppender(name, instrumentationKey);
    }

    /**
     * Logs a LogEvent using whatever logic this Appender wishes to use. It is typically recommended to use a
     * bridge pattern not only for the benefits from decoupling an Appender from its implementation, but it is also
     * handy for sharing resources which may require some form of locking.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event) {
        if (!this.isStarted()) {

            // TODO: trace not started.
            return;
        }

        try {
            ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(event);
            this.telemetryManager.sendTelemetry(aiEvent);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            // TODO: Assert.Debug/warning on exception?
        }
    }

    /**
     * This method is being called on object initialization.
     */
    @Override
    public void start() {
        super.start();
    }

    //endregion Public methods
}
