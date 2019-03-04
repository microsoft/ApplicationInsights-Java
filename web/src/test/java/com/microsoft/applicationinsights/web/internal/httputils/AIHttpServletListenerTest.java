package com.microsoft.applicationinsights.web.internal.httputils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.util.ThreadLocalCleaner;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;
import java.io.IOException;
import java.util.List;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * Tests for {@link AIHttpServletListener}
 */
@RunWith(JUnit4.class)
public class AIHttpServletListenerTest {

    @Rule public final ExpectedException thrown = ExpectedException.none();
    @Mock public RequestTelemetryContext requestTelemetryContext;
    @Mock public HttpExtractor<HttpServletRequest, HttpServletResponse> extractor;
    private TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
    @Spy public WebModulesContainer<HttpServletRequest,HttpServletResponse> webModulesContainer =
            new WebModulesContainer<>(telemetryConfiguration);
    @Spy public TelemetryClient telemetryClient;
    @Mock public List<ThreadLocalCleaner> threadLocalCleanerList;
    @InjectMocks HttpServerHandler<HttpServletRequest, HttpServletResponse> httpServerHandler;
    private AIHttpServletListener listener;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock AsyncEvent asyncEvent;
    @Mock AsyncContext asyncContext;
    @Mock Exception exception;
    private String url = "http://www.abc.com/xyz/opq";
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(extractor.getUrl(request)).thenReturn(url);
        when(extractor.getUri(request)).thenReturn("/xyz/opq");
        when(extractor.getScheme(request)).thenReturn("http");
        when(extractor.getHost(request)).thenReturn("www.abc.com");
        when(extractor.getQuery(request)).thenReturn("");
        when(extractor.getMethod(request)).thenReturn("GET");
        when(extractor.getUserAgent(request)).thenReturn("User-Agent");
        when(extractor.getStatusCode(response)).thenReturn(500);
        when(asyncEvent.getSuppliedResponse()).thenReturn(response);
        when(asyncEvent.getSuppliedRequest()).thenReturn(request);
        when(asyncEvent.getAsyncContext()).thenReturn(asyncContext);
        when(asyncEvent.getThrowable()).thenReturn(exception);
    }

    @Test
    public void cannotCreateListenerWithNullHttpServerHandler() {
        thrown.expect(NullPointerException.class);
        new AIHttpServletListener(null, requestTelemetryContext);
    }

    @Test
    public void cannotCreateListenerWithNullRequestTelemetryContext() {
        thrown.expect(NullPointerException.class);
        new AIHttpServletListener(httpServerHandler, null);
    }

    @Test
    public void listenerIsCreatedWhenCorrectArgsArePassed() {
        new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
    }

    @Test
    public void testOnComplete() throws IOException {
        requestTelemetryContext = httpServerHandler.handleStart(request, response);
        listener = new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
        listener.onComplete(asyncEvent);
        // since HttpServerHandler is a final class cannot call verify on httpServerHandler.handleEnd()
        // Hence verifying invocation on webModuleContainers.invokeOnEndRequest() which is the last instruction
        // in handleEnd() method.
        verify(webModulesContainer, times(1)).invokeOnEndRequest(request, response);
    }

    @Test
    public void testOnException() throws IOException {
        requestTelemetryContext = httpServerHandler.handleStart(request, response);
        listener = new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
        listener.onError(asyncEvent);
        // since HttpServerHandler is a final class cannot call verify on httpServerHandler.handleException()
        // Hence verifying invocation on telemetryClient.trackException() which is the last instruction
        // in handleException() method.
        verify(telemetryClient, times(1)).trackException(exception);
        verify(webModulesContainer, times(1)).invokeOnEndRequest(request, response);
    }

    @Test
    public void throwableAreNotTracked() throws IOException {
        when(asyncEvent.getThrowable()).thenReturn(mock(Throwable.class));
        requestTelemetryContext = httpServerHandler.handleStart(request, response);
        listener = new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
        listener.onError(asyncEvent);
        verify(telemetryClient, never()).trackException(exception);
        verify(webModulesContainer, times(1)).invokeOnEndRequest(request, response);
    }

    @Test
    public void testOnTimeout() throws IOException {
        requestTelemetryContext = httpServerHandler.handleStart(request, response);
        listener = new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
        listener.onComplete(asyncEvent);
        // since HttpServerHandler is a final class cannot call verify on httpServerHandler.handleEnd()
        // Hence verifying invocation on webModuleContainers.invokeOnEndRequest() which is the last instruction
        // in handleEnd() method.
        verify(webModulesContainer, times(1)).invokeOnEndRequest(request, response);
    }

    @Test
    public void testOnAsyncStart() throws IOException {
        requestTelemetryContext = httpServerHandler.handleStart(request, response);
        listener = new AIHttpServletListener(httpServerHandler, requestTelemetryContext);
        listener.onStartAsync(asyncEvent);
        verify(webModulesContainer, never()).invokeOnEndRequest(request, response);
        verify(asyncContext)
                .addListener(
                any(AIHttpServletListener.class),
                any(ServletRequest.class),
                any(ServletResponse.class));
    }
}

