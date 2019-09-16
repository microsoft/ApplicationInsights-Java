/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

/**
 * Tests the interface of the telemetry client.
 */
public final class TelemetryClientHelperTests {

    @Mock
    private TelemetryClient mockTelemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> argCaptor;
    @Captor
    private ArgumentCaptor<RemoteDependencyTelemetry> telemetryCaptor;
    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getTelemetryClient_returns_telemetryclient() {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);
        assertSame(mockTelemetryClient, sut.getTelemetryClient());
    }

    @Test
    public void monitorHttpDependencyTracksSuccessfulCall() throws Exception {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        HttpUriRequest mockRequest = mock(HttpUriRequest.class);
        CloseableHttpResponse mockResponse = buildMockResponse(200);

        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockClient.execute(eq(mockRequest), any(HttpContext.class))).thenReturn(mockResponse);

        CloseableHttpResponse response = sut.monitorHttpDependency("wibble", "wobble", mockClient, mockRequest);

        assertSame(response, mockResponse);

        verify(mockTelemetryClient).trackDependency(telemetryCaptor.capture());

        assertEquals(true, telemetryCaptor.getValue().getSuccess());
        assertEquals("wibble", telemetryCaptor.getValue().getName());
        assertEquals("wobble", telemetryCaptor.getValue().getCommandName());
        assertEquals("HTTP STATUS - 200", telemetryCaptor.getValue().getResultCode());
        assertEquals("HTTP - GET", telemetryCaptor.getValue().getType());
        assertTrue(telemetryCaptor.getValue().getDuration().getMilliseconds() > 0);
    }

    @Test
    public void monitorHttpDependencyTracksFailedCall() throws Exception {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        HttpUriRequest mockRequest = mock(HttpUriRequest.class);
        CloseableHttpResponse mockResponse = buildMockResponse(404);

        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockClient.execute(eq(mockRequest), any(HttpContext.class))).thenReturn(mockResponse);

        CloseableHttpResponse response = sut.monitorHttpDependency("wibble", "wobble", mockClient, mockRequest);

        assertSame(response, mockResponse);

        verify(mockTelemetryClient).trackDependency(telemetryCaptor.capture());

        assertEquals(false, telemetryCaptor.getValue().getSuccess());
        assertEquals("wibble", telemetryCaptor.getValue().getName());
        assertEquals("wobble", telemetryCaptor.getValue().getCommandName());
        assertEquals("HTTP STATUS - 404", telemetryCaptor.getValue().getResultCode());
        assertEquals("HTTP - GET", telemetryCaptor.getValue().getType());
    }

    @Test
    public void monitorHttpDependencyTracksOnThrownException() throws ClientProtocolException, IOException {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);
        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        HttpUriRequest mockRequest = mock(HttpUriRequest.class);

        when(mockClient.execute(eq(mockRequest), any(HttpContext.class))).thenThrow(new ClientProtocolException());

        try {
            sut.monitorHttpDependency("wibble", "wobble", mockClient, mockRequest);
        } catch (Exception ex) {
            // ignore
        }

        verify(mockTelemetryClient).trackDependency(telemetryCaptor.capture());
        verify(mockTelemetryClient).trackException(any(Exception.class), argCaptor.capture(),
                Matchers.<HashMap<String, Double>>any());

        assertEquals("wibble", argCaptor.getValue().get("dependencyName"));
        assertEquals("wobble", argCaptor.getValue().get("commandName"));
        assertEquals(false, telemetryCaptor.getValue().getSuccess());
        assertEquals("N/A", telemetryCaptor.getValue().getResultCode());
    }

    @Test
    public void monitorDependencyTracksOnSuccess() throws Exception {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);

        Callable<Integer> callableObj = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(2);
                return 2 * 2;
            }
        };

        int result = sut.monitorDependency("wibble", "wobble", callableObj);

        assertEquals(4, result);
        verify(mockTelemetryClient).trackDependency(eq("wibble"), eq("wobble"), durationCaptor.capture(), eq(true));
        assertTrue("Duration should be set", durationCaptor.getValue().getMilliseconds() > 0);
    }

    @Test
    public void monitorDependencyTracksOnExceptionThrown() {
        TelemetryClientHelper sut = new TelemetryClientHelper(mockTelemetryClient);

        Callable<Integer> callableObj = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(2);
                throw new NullPointerException();
            }
        };

        try {
            sut.monitorDependency("wibble", "wobble", callableObj);
        } catch (Exception ex) {
            // ignore
        }

        verify(mockTelemetryClient).trackDependency(eq("wibble"), eq("wobble"), durationCaptor.capture(), eq(false));
        verify(mockTelemetryClient).trackException(any(NullPointerException.class), argCaptor.capture(),
                Matchers.<HashMap<String, Double>>any());

        assertTrue("Duration should be set", durationCaptor.getValue().getMilliseconds() > 0);

        assertEquals("wibble", argCaptor.getValue().get("dependencyName"));
        assertEquals("wobble", argCaptor.getValue().get("commandName"));
    }

    private CloseableHttpResponse buildMockResponse(final int statusCode) {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);

        when(mockResponse.getStatusLine()).thenReturn(new StatusLine() {

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return null;
            }

            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public String toString() {
                return String.format("HTTP STATUS - %s", Integer.toString(statusCode));
            }
        });

        return mockResponse;
    }
}