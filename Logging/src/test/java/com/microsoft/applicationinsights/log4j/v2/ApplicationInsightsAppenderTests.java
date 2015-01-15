package com.microsoft.applicationinsights.log4j.v2;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.common.LoggingTestHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.junit.*;

public class ApplicationInsightsAppenderTests {

    // region Consts

    private final String TestInstrumentationKey = "c9341531-05ac-4d8c-972e-36e97601d5ff";

    // endregion Consts

    // region Members

    private List<Telemetry> telemetriesSent;

    // endregion Members

    // region Initialization & cleanup

    @Before
    public void setup() {
        setMockTelemetryChannelToAIAppender();
    }

    // endregion Initialization & cleanup

    // region Tests

    @Test
    public void testInstrumentationKeyIsLoadedFromConfiguration() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();

        String configurationKey = appender.getTelemetryClientProxy().getTelemetryClient().getContext().getInstrumentationKey();
        Assert.assertEquals(TestInstrumentationKey, configurationKey);
    }

    @Test
    public void testAppenderSendsGivenEvent() {
        Logger logger = LogManager.getRootLogger();
        logger.trace("New event!");

        Assert.assertEquals(1, this.telemetriesSent.size());
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = LogManager.getRootLogger();
        org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)logger;

        Map<String, Appender> appenderMap = coreLogger.getAppenders();
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender) appenderMap.get("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        this.telemetriesSent = new LinkedList<Telemetry>();
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();

        TelemetryChannel telemetryChannelMock = LoggingTestHelper.createMockTelemetryChannelWithGivenTelemetryCollection(this.telemetriesSent);

        appender.getTelemetryClientProxy().getTelemetryClient().setChannel(telemetryChannelMock);
    }

    // endregion Private methods
}