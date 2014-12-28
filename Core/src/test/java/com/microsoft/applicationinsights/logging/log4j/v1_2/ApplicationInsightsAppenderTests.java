package com.microsoft.applicationinsights.logging.log4j.v1_2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.logging.Common.LoggingTestHelper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import java.util.LinkedList;
import java.util.List;


public class ApplicationInsightsAppenderTests {

    // region Consts

    private final String TestInstrumentationKey = "Test_Instrumentation_Key";

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

        Assert.assertEquals(1, telemetriesSent.size());
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = LogManager.getRootLogger();
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender)logger.getAppender("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        this.telemetriesSent = new LinkedList<>();
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();
        appender.activateOptions();

        TelemetryChannel telemetryChannelMock = LoggingTestHelper.createMockTelemetryChannelWithGivenTelemetryCollection(this.telemetriesSent);

        appender.getTelemetryClientProxy().getTelemetryClient().setChannel(telemetryChannelMock);
    }

    // endregion Private methods
}