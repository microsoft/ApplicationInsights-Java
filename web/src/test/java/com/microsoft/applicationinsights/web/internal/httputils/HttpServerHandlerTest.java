package com.microsoft.applicationinsights.web.internal.httputils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class HttpServerHandlerTest {

    @Rule public final ExpectedException thrown = ExpectedException.none();
    @Mock public HttpExtractor<HttpServletRequest, HttpServletResponse> extractor;
    private TelemetryConfiguration telemetryConfiguration = TelemetryConfiguration.getActive();
    @Spy public WebModulesContainer<HttpServletRequest,HttpServletResponse> webModulesContainer =
        new WebModulesContainer<>(telemetryConfiguration);
    @Spy public TelemetryClient telemetryClient;
    @InjectMocks HttpServerHandler<HttpServletRequest, HttpServletResponse> httpServerHandler;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock HttpServletRequest requestWithQueryString;
    private String url = "http://www.abc.com/xyz/opq";
    private String url1 = "http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a";

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

        when(extractor.getUrl(requestWithQueryString)).thenReturn(url1);
        when(extractor.getMethod(requestWithQueryString)).thenReturn("GET");
        when(extractor.getHost(requestWithQueryString)).thenReturn("30thh.loc:8480");
        when(extractor.getQuery(requestWithQueryString)).thenReturn("p+1=c+d&p+2=e+f");
        when(extractor.getUserAgent(requestWithQueryString)).thenReturn("Test");
        when(extractor.getUri(requestWithQueryString)).thenReturn("/app/test%3F/a%3F+b");
        when(extractor.getScheme(requestWithQueryString)).thenReturn("http");
        when(extractor.getStatusCode(response)).thenReturn(500);
    }

    @After
    public void clean() {
        ThreadContext.remove();
    }

    @Test
    public void httpServerHandlerDoesNotExceptNullExtractor() {
        thrown.expect(NullPointerException.class);
        new HttpServerHandler<>(null, webModulesContainer, telemetryClient);
    }

    @Test
    public void httpServerHandlerDoesNotExceptNullWebModulesContainer() {
        thrown.expect(NullPointerException.class);
        new HttpServerHandler<>(extractor, null, telemetryClient);
    }

    @Test
    public void httpServerHandlerAcceptsNullTelemetryClient() {
        new HttpServerHandler<>(extractor, webModulesContainer, null);
    }

    @Test
    public void handleStartShouldReturnTelemetryContext() throws MalformedURLException {
        RequestTelemetryContext context = httpServerHandler.handleStart(request, response);
        assertThat(context, is(notNullValue()));
    }

    @Test
    public void handleEndThrowsWhenCalledBeforeHandleStart() {
        thrown.expect(NullPointerException.class);
        httpServerHandler.handleEnd(request, response);
    }

    @Test
    public void handleEndSucceedsWhenHandleStartIsCalledFirst() throws MalformedURLException {
        httpServerHandler.handleStart(request, response);
        httpServerHandler.handleEnd(request, response);
    }

    @Test
    public void webModuleContainersOnBeginIsCalledWhenHandleStartInvoked() throws MalformedURLException {
        httpServerHandler.handleStart(request, response);
        verify(webModulesContainer, times(1)).invokeOnBeginRequest(request, response);
    }

    @Test
    public void webModuleContainersOnEndIsCalledWhenHandleEndInvoked() throws MalformedURLException {
        httpServerHandler.handleStart(request, response);
        httpServerHandler.handleEnd(request, response);
        verify(webModulesContainer, times(1)).invokeOnEndRequest(request, response);
    }

    @Test
    public void onBeginIsCalledBeforeOnEnd() throws MalformedURLException {
        httpServerHandler.handleStart(request, response);
        httpServerHandler.handleEnd(request, response);
        InOrder inOrder = inOrder(webModulesContainer);
        inOrder.verify(webModulesContainer).invokeOnBeginRequest(request, response);
        inOrder.verify(webModulesContainer).invokeOnEndRequest(request, response);
    }

    @Test
    public void trackExceptionIsCalledWhenHandleExceptionInvoked() {
        Exception ne = new NullPointerException();
        httpServerHandler.handleException(ne);
        verify(telemetryClient, times(1)).trackException(ne);
    }

    @Test
    public void requestTelemetryFieldsAreSetWhenHandleStartIsInvoked() throws MalformedURLException {
        RequestTelemetryContext rtc = httpServerHandler.handleStart(request, response);
        RequestTelemetry rt = rtc.getHttpRequestTelemetry();
        assertThat(rt.getId(), is(CoreMatchers.<String>notNullValue()));
        assertThat(rt.getName(), containsString("GET"));
        assertThat(rt.getName(), containsString("/xyz/opq"));
        assertThat(rt.getUrl().toString(), equalTo(url));
        assertThat(rt.getContext().getUser().getUserAgent(), equalTo("User-Agent"));
        assertThat(rt.getTimestamp(), is(CoreMatchers.<Date>notNullValue()));
    }

    @Test
    public void timeStatusCodeAreSetWhenHandleEndIsInvoked() throws MalformedURLException {
        RequestTelemetryContext rtc = httpServerHandler.handleStart(request, response);
        RequestTelemetry rt = rtc.getHttpRequestTelemetry();
        assertThat(rt.getId(), is(CoreMatchers.<String>notNullValue()));
        assertThat(rt.getName(), containsString("GET"));
        assertThat(rt.getName(), containsString("/xyz/opq"));
        assertThat(rt.getUrl().toString(), equalTo(url));
        assertThat(rt.getContext().getUser().getUserAgent(), equalTo("User-Agent"));
        assertThat(rt.getTimestamp(), is(CoreMatchers.<Date>notNullValue()));

        httpServerHandler.handleEnd(request, response);
        // ensure same request telemetry is modified (picked from TLS)
        assertThat(rt.getDuration().getTotalMilliseconds(), is(not(0L)));
        assertThat(rt.getResponseCode(), equalTo("500"));
        assertThat(rt.isSuccess(), equalTo(false));
    }
}
