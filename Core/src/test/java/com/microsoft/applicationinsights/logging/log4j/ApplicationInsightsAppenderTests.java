package com.microsoft.applicationinsights.logging.log4j;

import java.util.List;
import java.util.Map;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.common.TelemetryChannelMock;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.junit.*;

public class ApplicationInsightsAppenderTests {

    // region Consts

    private final String TestInstrumentationKey = "Test_Instrumentation_Key";

    // endregion Consts

    // region Members

    private TelemetryChannelMock telemetryChannelMock;

    // endregion Members

    // region Initialization & cleanup

    @Before
    public void setup() {
        telemetryChannelMock = new TelemetryChannelMock();

        setMockTelemetryChannelToAIAppender();
    }

    // endregion Initialization & cleanup

    // region Tests

    @Test
    public void testInstrumentationKeyIsLoadedFromConfiguration() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();

        Assert.assertEquals(TestInstrumentationKey, appender.getInstrumentationKey());
    }

    @Test
    public void testAppenderSendsGivenEvent() {
        Logger logger = LogManager.getRootLogger();
        logger.trace("New event!");

        Assert.assertEquals(1, telemetryChannelMock.getSentItems().size());
    }

    @Test
    public void testCustomParametersAddedByAppender() {
        Logger logger = LogManager.getRootLogger();
        logger.trace("New event!");

        List<Telemetry> sentItems = telemetryChannelMock.getSentItems();
        Telemetry telemetry = sentItems.get(0);

        // TODO: should custom parameters validated one-by-one for values?
        Map<String, String> customParameters = telemetry.getContext().getProperties();

        Assert.assertTrue("Custom parameters list shouldn't be empty.", customParameters.size() > 0);
    }

    /**
     * The instrumentation key can be provided using Log4j configuration xml OR App Insights properties file
     * (applicationinsights.properties) as a fallback. Therefore we must allow empty/null instrumentation key.
     */
    @Test
    public void testAppenderInitializedCorrectlyWhenNoInstrumentationKeyProvided() {

        boolean isExceptionWasThrown = false;
        try {
            ApplicationInsightsAppender appender = new ApplicationInsightsAppender("appender", null);
            appender = new ApplicationInsightsAppender("appender", "");
        } catch (Exception e) {
            isExceptionWasThrown = true;
        }

        Assert.assertFalse("No exception should be thrown.", isExceptionWasThrown);
    }

    @Test
    public void testWhenExceptionTracedThenExceptionTelemetrySent() {
        sendThrowableAndVerifyExceptionTelemetrySent(new Exception("oh no!"));
    }

    @Test
    public void testWhenErrorTracedThenExceptionTelemetrySent() {
        sendThrowableAndVerifyExceptionTelemetrySent(new Error("oh no!"));
    }

    // endregion Tests

    // region Private methods

    private void sendThrowableAndVerifyExceptionTelemetrySent(Throwable throwable) {
        Logger logger = LogManager.getRootLogger();
        logger.catching(throwable);

        Telemetry eventSent = getLastSentItems().get(0);

        Assert.assertTrue("Event should be of type " + ExceptionTelemetry.class.getSimpleName(), eventSent instanceof ExceptionTelemetry);
    }

    private List<Telemetry> getLastSentItems() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();
        TelemetryChannelMock channelMock = (TelemetryChannelMock) appender.getTelemetryClient().getChannel();

        return channelMock.getSentItems();
    }

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = LogManager.getRootLogger();
        org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)logger;

        Map<String, Appender> appenderMap = coreLogger.getAppenders();
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender) appenderMap.get("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();
        TelemetryClient telemetryClient = appender.getTelemetryClient();
        telemetryClient.setChannel(telemetryChannelMock);
    }

    // endregion Private methods
}
