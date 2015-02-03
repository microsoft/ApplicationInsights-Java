package com.microsoft.applicationinsights.web.extensibility;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.utils.JettyServer;
import com.microsoft.applicationinsights.web.utils.MockTelemetryChannel;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletRequest;

import static org.junit.Assert.assertEquals;
import static com.microsoft.applicationinsights.web.utils.HttpHelper.sendGetRequestAndWait;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.List;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingTelemetryModuleTests {
    private static JettyServer server = new JettyServer();
    private static WebRequestTrackingTelemetryModule defaultModule;

    @BeforeClass
    public static void classInitialize() throws Exception {
        server.start();
    }

    @Before
    public void testInitialize() {
        defaultModule = new WebRequestTrackingTelemetryModule();
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    @Test
    public void testHttpRequestTrackedSuccessfully() throws Exception {
        //Set channel
        MockTelemetryChannel channel = new MockTelemetryChannel();
        TelemetryConfiguration.getActive().setChannel(channel);
        TelemetryConfiguration.getActive().setInstrumentationKey("SOME_INT_KEY");

        sendGetRequestAndWait("http://localhost:1234");

        List<Telemetry> items = channel.getTelemetryItems();
        assertEquals(1, items.size());
        HttpRequestTelemetry requestTelemetry = (HttpRequestTelemetry)items.get(0);

        assertEquals("200", requestTelemetry.getResponseCode());
        assertEquals("GET /", requestTelemetry.getName());
        assertEquals("GET", requestTelemetry.getHttpMethod());
        assertEquals("http://localhost:1234/", requestTelemetry.getUrl().toString());
    }

    @Test
    public void testOnBeginRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onBeginRequest(request, null);
    }

    @Test
    public void testOnEndRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onEndRequest(request, null);
    }

    // region Private methods

    private ServletRequest createFaultyServletRequestMock() {
        ServletRequest request = mock(ServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(request).getAttribute(any(String.class));

        return request;
    }

    // endregion Private methods
}