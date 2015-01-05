package com.microsoft.applicationinsights.shared;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import com.microsoft.applicationinsights.logging.common.ApplicationInsightsEvent;
import com.microsoft.applicationinsights.logging.common.LogTelemetryClientProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class LogTelemetryClientProxyTests {

    // region Members

    private static final String INSTRUMENTATION_KEY = "Instrumentation_Key";
    private LogTelemetryClientProxy telemetryClientProxy;
    private TelemetryClient telemetryClientMock;
    private List<Telemetry> telemetriesSent;

    // endregion Members

    // region Initialization

    @Before
    public void Before() {
        this.telemetryClientMock = mock(TelemetryClient.class);
        this.telemetriesSent = new LinkedList<Telemetry>();
        setupTelemetryClientMock(this.telemetriesSent);
        this.telemetryClientProxy = new LogTelemetryClientProxy(telemetryClientMock, INSTRUMENTATION_KEY);
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testExceptionIsClassifiedAndSentCorrectly() {
        Telemetry telemetry = sendAIEventAndGetOutputTelemetry(true);

        Assert.assertTrue("Exception telemetry should be sent.", telemetry instanceof ExceptionTelemetry);
    }

    @Test
    public void testTraceIsClassifiedAndSentCorrectly() {
        Telemetry telemetry = sendAIEventAndGetOutputTelemetry(false);

        Assert.assertTrue("Exactly one Trace telemetry should be sent.", telemetry instanceof TraceTelemetry);
    }

    @Test
    public void testWhenTelemetryClientProxyFailedToInitializeNoExceptionIsThrown() {

        // Setting telemetry client mock to fail proxy initialization when getting client context.
        when(this.telemetryClientMock.getContext()).thenThrow(new NullPointerException("Exception!"));

        this.telemetryClientProxy = new LogTelemetryClientProxy(this.telemetryClientMock, INSTRUMENTATION_KEY);

        Assert.assertFalse("The proxy shouldn't have been initialized.", this.telemetryClientProxy.isInitialized());
    }

    @Test
    public void testTelemetryClientProxyInitializedCorrectlyWhenNullInstrumentationKeyProvided() {
        initializeTelemetryClientProxyWithInstrumentationKeyAndVerifyInitialization(null);
    }

    @Test
    public void testTelemetryClientProxyInitializedCorrectlyWhenEmptyInstrumentationKeyProvided() {
        initializeTelemetryClientProxyWithInstrumentationKeyAndVerifyInitialization("");
    }


    @Test
    public void testCustomParametersAddedByTelemetryClientProxy() {
        Telemetry telemetry = sendAIEventAndGetOutputTelemetry(false);

        // TODO: should custom parameters validated one-by-one for values?
        Map<String, String> customParameters = telemetry.getContext().getProperties();

        Assert.assertTrue("Custom parameters list shouldn't be empty.", customParameters.size() > 0);
    }

    // endregion Tests

    // region Private methods

    private Telemetry sendAIEventAndGetOutputTelemetry(boolean isExceptionEvent) {
        this.telemetryClientProxy.sendEvent(createApplicationInsightEvent(isExceptionEvent));

        return this.telemetriesSent.get(0);
    }

    private void initializeTelemetryClientProxyWithInstrumentationKeyAndVerifyInitialization(String key) {
        this.telemetryClientProxy = new LogTelemetryClientProxy(this.telemetryClientMock, key);

        Assert.assertTrue("Proxy should should have been initialized correctly.", this.telemetryClientProxy.isInitialized());
    }

    private ApplicationInsightsEvent createApplicationInsightEvent(boolean isExceptionEvent) {
        ApplicationInsightsEvent event = mock(ApplicationInsightsEvent.class);
        when(event.isException()).thenReturn(isExceptionEvent);
        when(event.getException()).thenReturn(isExceptionEvent ? new Exception("Exception!") : null);
        when(event.getCustomParameters()).thenReturn(new HashMap<String, String>() {{ put("Key", "Value"); }});

        return event;
    }

    private void setupTelemetryClientMock(final List<Telemetry> telemetries) {
        when(this.telemetryClientMock.getContext()).thenReturn(new TelemetryContext());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Telemetry telemetry = ((Telemetry) invocation.getArguments()[0]);
                telemetries.add(telemetry);

                return null;
            }
        }).when(this.telemetryClientMock).track(any(Telemetry.class));
    }

     // endregion Private methods
}