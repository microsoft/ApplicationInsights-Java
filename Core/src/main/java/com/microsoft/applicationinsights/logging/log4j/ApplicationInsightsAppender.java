package com.microsoft.applicationinsights.logging.log4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.BaseTelemetry;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import com.microsoft.applicationinsights.util.LocalStringsUtils;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name="ApplicationInsightsAppender", category = "Core", elementType = "appender")
public class ApplicationInsightsAppender extends AbstractAppender {

    //region Members

    private TelemetryClient telemetryClient;
    private String instrumentationKey;

    //endregion Members

    //region Ctor

    /**
     * Constructs new Application Insights appender.
     * @param name The appender name.
     * @param instrumentationKey The instrumentation key.
     */
    protected ApplicationInsightsAppender(String name, String instrumentationKey) {
        super(name, null, null);

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
     * Logs the event.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event) {
        String formattedMessage = event.getMessage().getFormattedMessage();

        if (formattedMessage == null) {
            formattedMessage = "Log4j Trace";
        }

        BaseTelemetry telemetry;
        Throwable throwable = event.getThrown();
        if (throwable != null) {
            Exception exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);

            telemetry = new ExceptionTelemetry(exception);
        } else {
            telemetry = new TraceTelemetry(formattedMessage);
        }

        buildCustomParameters(event, telemetry);

        this.telemetryClient.track(telemetry);
    }

    /**
     * This method is being called on object initialization.
     */
    @Override
    public void start() {
        super.start();

        this.telemetryClient = new TelemetryClient();
        if (!LocalStringsUtils.isNullOrEmpty(this.instrumentationKey))
        {
            this.telemetryClient.getContext().setInstrumentationKey(this.instrumentationKey);
        }
    }

    //endregion Public methods

    //region Private methods

    protected TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    /**
     * Builds the custom parameters.
     * @param logEvent The event.
     * @param telemetry The trace telemetry to update.
     */
    private static void buildCustomParameters(LogEvent logEvent, BaseTelemetry telemetry) {

        Map<String, String> metaData = telemetry.getContext().getProperties();

        metaData.put("SourceType", "Log4j");

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
