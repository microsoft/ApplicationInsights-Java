package com.microsoft.applicationinsights.log4j.v1_2;

import java.util.LinkedList;
import java.util.List;
import org.junit.*;

import com.microsoft.applicationinsights.shared.LogChannelMockVerifier;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

        Assert.assertEquals(1, LogChannelMockVerifier.INSTANCE.getTelemetryCollection().size());
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = LogManager.getRootLogger();
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender)logger.getAppender("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        ApplicationInsightsAppender appender = getApplicationInsightsAppender();
        appender.activateOptions();
        LogChannelMockVerifier.INSTANCE.getTelemetryCollection().clear();
    }

    // endregion Private methods
}