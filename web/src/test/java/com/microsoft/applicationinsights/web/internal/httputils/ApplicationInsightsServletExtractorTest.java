package com.microsoft.applicationinsights.web.internal.httputils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ApplicationInsightsServletExtractorTest {

    private final StringBuffer url = new StringBuffer("http://www.abcd.com/user/xyz");

    @Test
    public void testDataExtraction() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpExtractor<HttpServletRequest, HttpServletResponse> extractor = spy(new ApplicationInsightsServletExtractor());
        when(httpServletRequest.getRequestURL()).thenReturn(url);
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getServerName()).thenReturn("1.1.1.1");
        when(httpServletRequest.getQueryString()).thenReturn("/user/xyz");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Test");
        when(httpServletResponse.getStatus()).thenReturn(0);

        assertThat(extractor.getMethod(httpServletRequest), equalTo("GET"));
        assertThat(extractor.getQuery(httpServletRequest), equalTo("/user/xyz"));
        assertThat(extractor.getHost(httpServletRequest), equalTo("1.1.1.1"));
        assertThat(extractor.getUserAgent(httpServletRequest), equalTo("Test"));
        assertThat(extractor.getStatusCode(httpServletResponse), equalTo(0));
        assertThat(extractor.getUrl(httpServletRequest), equalTo(url.toString()));
    }
}
