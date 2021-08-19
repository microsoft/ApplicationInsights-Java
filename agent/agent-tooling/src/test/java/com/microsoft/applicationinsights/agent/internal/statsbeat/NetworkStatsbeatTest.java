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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.telemetry.EndpointProvider;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NetworkStatsbeatTest {

  private NetworkStatsbeat networkStatsbeat;

  @BeforeEach
  public void init() {
    networkStatsbeat = new NetworkStatsbeat(new CustomDimensions());
  }

  @Test
  public void testAddInstrumentation() {
    networkStatsbeat.addInstrumentation("io.opentelemetry.jdbc");
    networkStatsbeat.addInstrumentation("io.opentelemetry.tomcat-7.0");
    networkStatsbeat.addInstrumentation("io.opentelemetry.http-url-connection");
    assertThat(networkStatsbeat.getInstrumentation())
        .isEqualTo(
            (long)
                (Math.pow(2, 13)
                    + Math.pow(2, 21)
                    + Math.pow(
                        2, 57))); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP
  }

  @Test
  public void testIncrementRequestSuccessCount() {
    assertThat(networkStatsbeat.getRequestSuccessCount()).isEqualTo(0);
    assertThat(networkStatsbeat.getRequestDurationAvg()).isEqualTo(0);
    networkStatsbeat.incrementRequestSuccessCount(1000);
    networkStatsbeat.incrementRequestSuccessCount(3000);
    assertThat(networkStatsbeat.getRequestSuccessCount()).isEqualTo(2);
    assertThat(networkStatsbeat.getRequestDurationAvg()).isEqualTo(2000.0);
  }

  @Test
  public void testIncrementRequestFailureCount() {
    assertThat(networkStatsbeat.getRequestFailureCount()).isEqualTo(0);
    networkStatsbeat.incrementRequestFailureCount();
    networkStatsbeat.incrementRequestFailureCount();
    assertThat(networkStatsbeat.getRequestFailureCount()).isEqualTo(2);
  }

  @Test
  public void testIncrementRetryCount() {
    assertThat(networkStatsbeat.getRetryCount()).isEqualTo(0);
    networkStatsbeat.incrementRetryCount();
    networkStatsbeat.incrementRetryCount();
    assertThat(networkStatsbeat.getRetryCount()).isEqualTo(2);
  }

  @Test
  public void testIncrementThrottlingCount() {
    assertThat(networkStatsbeat.getThrottlingCount()).isEqualTo(0);
    networkStatsbeat.incrementThrottlingCount();
    networkStatsbeat.incrementThrottlingCount();
    assertThat(networkStatsbeat.getThrottlingCount()).isEqualTo(2);
  }

  @Test
  public void testIncrementExceptionCount() {
    assertThat(networkStatsbeat.getExceptionCount()).isEqualTo(0);
    networkStatsbeat.incrementExceptionCount();
    networkStatsbeat.incrementExceptionCount();
    assertThat(networkStatsbeat.getExceptionCount()).isEqualTo(2);
  }

  @Test
  public void testRaceCondition() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    AtomicInteger instrumentationCounter = new AtomicInteger();
    for (int i = 0; i < 100; i++) {
      executorService.execute(
          new Runnable() {
            @Override
            public void run() {
              for (int j = 0; j < 1000; j++) {
                networkStatsbeat.incrementRequestSuccessCount(j % 2 == 0 ? 5 : 10);
                networkStatsbeat.incrementRequestFailureCount();
                networkStatsbeat.incrementRetryCount();
                networkStatsbeat.incrementThrottlingCount();
                networkStatsbeat.incrementExceptionCount();
                networkStatsbeat.addInstrumentation(
                    "instrumentation" + instrumentationCounter.getAndDecrement());
              }
            }
          });
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.MINUTES);
    assertThat(networkStatsbeat.getRequestSuccessCount()).isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestFailureCount()).isEqualTo(100000);
    assertThat(networkStatsbeat.getRetryCount()).isEqualTo(100000);
    assertThat(networkStatsbeat.getThrottlingCount()).isEqualTo(100000);
    assertThat(networkStatsbeat.getExceptionCount()).isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestDurationAvg()).isEqualTo(7.5);
    assertThat(networkStatsbeat.getInstrumentationList().size()).isEqualTo(100000);
  }

  @Test
  public void testGetHost() {
    String url = "https://fake-host.applicationinsights.azure.com/v2.1/track";
    assertThat(networkStatsbeat.getHost(url)).isEqualTo("fake-host.applicationinsights.azure.com");

    url = "http://fake-host.example.com/v2/track";
    assertThat(networkStatsbeat.getHost(url)).isEqualTo("fake-host.example.com");

    url = "http://www.fake-host.com/v2/track";
    assertThat(networkStatsbeat.getHost(url)).isEqualTo("www.fake-host.com");
  }

  @Test
  public void testTrackHostOnRedirect() throws MalformedURLException {
    String originalUrl = "https://fake-original-url.com/";

    TelemetryClient mockTelemetryClient = Mockito.mock(TelemetryClient.class);
    mockTelemetryClient.setConnectionString("InstrumentationKey=fake-ikey;EndpointSuffix=" + originalUrl);
    EndpointProvider mockEndpointProvider = Mockito.mock(EndpointProvider.class);
    Mockito.when(mockTelemetryClient.getEndpointProvider()).thenReturn(mockEndpointProvider);
    URL mockUrl = new URL(originalUrl);
    Mockito.when(mockEndpointProvider.getIngestionEndpointUrl()).thenReturn(mockUrl);

    assertThat(networkStatsbeat.getPreviousHost()).isEqualTo(null);
    assertThat(networkStatsbeat.getCurrentHost()).isEqualTo(null);
    assertThat(networkStatsbeat.getRedirected().get()).isEqualTo(false);

    // 1st redirect
    String redirectUrl1 = "https://fake-redirect-url.test.com/";
    networkStatsbeat.trackHostOnRedirect(mockTelemetryClient, redirectUrl1);
    assertThat(networkStatsbeat.getPreviousHost()).isEqualTo(networkStatsbeat.getHost(originalUrl));
    assertThat(networkStatsbeat.getCurrentHost()).isEqualTo(networkStatsbeat.getHost(redirectUrl1));
    assertThat(networkStatsbeat.getRedirected().get()).isEqualTo(true);

    // 2nd redirect
    String redirectUrl2 = "https://fake-redirect-url-2.test.com/";
    networkStatsbeat.trackHostOnRedirect(mockTelemetryClient, redirectUrl2);
    assertThat(networkStatsbeat.getPreviousHost()).isEqualTo(networkStatsbeat.getHost(redirectUrl1));
    assertThat(networkStatsbeat.getCurrentHost()).isEqualTo(networkStatsbeat.getHost(redirectUrl2));
    assertThat(networkStatsbeat.getRedirected().get()).isEqualTo(true);
  }
}
