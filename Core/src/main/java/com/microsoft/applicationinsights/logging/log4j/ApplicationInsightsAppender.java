package com.microsoft.applicationinsights.logging.log4j;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Plugin(name="ApplicationInsightsAppender", category = "Core", elementType = "appender")
public class ApplicationInsightsAppender extends AbstractAppender {

    //region Members

    private TelemetryClient telemetryClient;
    private String instrumentationKey;

    //endregion Members

    //region Ctor

    /**
     * Constructor that defaults to suppressing exceptions.
     *
     * @param name   The Appender name.
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     */
    protected ApplicationInsightsAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    /**
     * Constructs new Application Insights appender.
     * @param name The appender name.
     * @param instrumentationKey The instrumentation key.
     */
    public ApplicationInsightsAppender(String name, String instrumentationKey) {
        this(name, null, null);

        this.instrumentationKey = instrumentationKey;
    }

    //endregion Ctor

    //region Public methods

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
     * Gets the instrumentation key.
     *
     * @return instrumentation key.
     */
    public String getInstrumentationKey() {
        return instrumentationKey;
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
        String formattedMessage = event.getMessage().getFormattedMessage();

        if (formattedMessage == null) {
            formattedMessage = "Log4J Trace";
        }

        TraceTelemetry trace = new TraceTelemetry(formattedMessage);
        buildCustomParameters(event, trace);

        this.telemetryClient.trackTrace(trace);
    }

    /**
     * This method is being called on object initialization.
     */
    @Override
    public void start() {
        super.start();

        this.telemetryClient = new TelemetryClient();
        if (this.instrumentationKey != null && !this.instrumentationKey.isEmpty())
        {
            this.telemetryClient.getContext().setInstrumentationKey(this.instrumentationKey);
        }
    }

    //endregion Public methods

    //region Private methods

    /**
     * Builds the custom parameters.
     * @param logEvent The event.
     * @param traceTelemetry The trace telemetry to update.
     */
    private static void buildCustomParameters(LogEvent logEvent, TraceTelemetry traceTelemetry) {

        Map<String, String> metaData = traceTelemetry.getContext().getProperties();

        metaData.put("SourceType", "Log4Net");

        addLogEventProperty("LoggerName", logEvent.getLoggerName(), metaData);
        addLogEventProperty("LoggingLevel", logEvent.getLevel() != null ? logEvent.getLevel().name() : null, metaData);
        addLogEventProperty("ThreadName", logEvent.getThreadName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(logEvent.getTimeMillis()), metaData);

        if (logEvent.isIncludeLocation())
        {
            StackTraceElement stackTraceElement = logEvent.getSource();

            addLogEventProperty("ClassName", stackTraceElement.getClassName(), metaData);
            addLogEventProperty("FileName", stackTraceElement.getFileName(), metaData);
            addLogEventProperty("MethodName", stackTraceElement.getMethodName(), metaData);
            addLogEventProperty("LineNumber", String.valueOf(stackTraceElement.getLineNumber()), metaData);
        }

        if (logEvent.getThrown() != null)
        {
            addLogEventProperty("ExceptionMessage", logEvent.getThrown().getMessage(), metaData);
        }

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.
    }

    private static void addLogEventProperty(String key, String value, Map<String, String> metaData) {
        if (value != null)
        {
            metaData.put(key, value);
        }
    }

    private static String getFormattedDate(long dateInMilliseconds) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).format(new Date(dateInMilliseconds));
    }

    //endregion Private methods
}
