package com.microsoft.applicationinsights.logback;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.LinkedList;
import java.util.List;

import com.microsoft.applicationinsights.shared.LogChannelMockVerifier;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;

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
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        logger.trace("Hello");

        Assert.assertEquals(1, LogChannelMockVerifier.INSTANCE.getTelemetryCollection().size());
    }

    // endregion Tests

    // region Private methods

    private ApplicationInsightsAppender getApplicationInsightsAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        ApplicationInsightsAppender appender = (ApplicationInsightsAppender) logger.getAppender("test");

        return appender;
    }

    private void setMockTelemetryChannelToAIAppender() {
        LogChannelMockVerifier.INSTANCE.getTelemetryCollection().clear();
    }

    // endregion Private methods
}