package com.microsoft.applicationinsights.web.internal.httputils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ApplicationInsightsServletExtractorTest {

    private final StringBuffer url =
        new StringBuffer("http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a");

    @Mock public HttpServletRequest httpServletRequest;
    @Mock public HttpServletResponse httpServletResponse;
    @Spy public HttpExtractor<HttpServletRequest, HttpServletResponse> extractor = new ApplicationInsightsServletExtractor();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(httpServletRequest.getRequestURL()).thenReturn(url);
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getServerName()).thenReturn("30thh.loc");
        when(httpServletRequest.getQueryString()).thenReturn("p+1=c+d&p+2=e+f");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Test");
        when(httpServletRequest.getRequestURI()).thenReturn("/app/test%3F/a%3F+b");
        when(httpServletRequest.getScheme()).thenReturn("http");
        when(httpServletRequest.getServerPort()).thenReturn(8480);
        when(httpServletResponse.getStatus()).thenReturn(0);
    }

    @Test
    public void testDataExtraction() {
        assertThat(extractor.getMethod(httpServletRequest), equalTo("GET"));
        assertThat(extractor.getQuery(httpServletRequest), equalTo("p+1=c+d&p+2=e+f"));
        assertThat(extractor.getHost(httpServletRequest), equalTo("30thh.loc:8480"));
        assertThat(extractor.getUserAgent(httpServletRequest), equalTo("Test"));
        assertThat(extractor.getStatusCode(httpServletResponse), equalTo(0));
        assertThat(extractor.getUrl(httpServletRequest), equalTo(url.toString()));
    }

    @Test
    public void requestUriDoesntHaveSessionIdWhenExtracted() {
        assertThat(extractor.getUri(httpServletRequest), not(containsString("jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a")));
    }
}