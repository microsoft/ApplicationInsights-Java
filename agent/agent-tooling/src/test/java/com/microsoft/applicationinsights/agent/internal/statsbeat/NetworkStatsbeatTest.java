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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NetworkStatsbeatTest {

  private NetworkStatsbeat networkStatsbeat;
  private static final String IKEY = "00000000-0000-0000-0000-0FEEDDADBEEF";

  @BeforeEach
  public void init() {
    networkStatsbeat = new NetworkStatsbeat();
  }

  @Test
  public void testIncrementRequestSuccessCount() {
    assertThat(networkStatsbeat.getRequestSuccessCount(IKEY)).isEqualTo(0);
    assertThat(networkStatsbeat.getRequestDurationAvg(IKEY)).isEqualTo(0);
    networkStatsbeat.incrementRequestSuccessCount(1000, IKEY, "host");
    networkStatsbeat.incrementRequestSuccessCount(3000, IKEY, "host");
    assertThat(networkStatsbeat.getRequestSuccessCount(IKEY)).isEqualTo(2);
    assertThat(networkStatsbeat.getRequestDurationAvg(IKEY)).isEqualTo(2000.0);
  }

  @Test
  public void testIncrementRequestFailureCount() {
    int statusCode = 400;
    assertThat(networkStatsbeat.getRequestFailureCount(IKEY, statusCode)).isEqualTo(0);
    networkStatsbeat.incrementRequestFailureCount(IKEY, "host", statusCode);
    networkStatsbeat.incrementRequestFailureCount(IKEY, "host", statusCode);
    assertThat(networkStatsbeat.getRequestFailureCount(IKEY, statusCode)).isEqualTo(2);
  }

  @Test
  public void testIncrementRetryCount() {
    int statusCode = 500;
    assertThat(networkStatsbeat.getRetryCount(IKEY, statusCode)).isEqualTo(0);
    networkStatsbeat.incrementRetryCount(IKEY, "host", statusCode);
    networkStatsbeat.incrementRetryCount(IKEY, "host", statusCode);
    assertThat(networkStatsbeat.getRetryCount(IKEY, statusCode)).isEqualTo(2);
  }

  @Test
  public void testIncrementThrottlingCount() {
    int statusCode = 402;
    assertThat(networkStatsbeat.getThrottlingCount(IKEY, statusCode)).isEqualTo(0);
    networkStatsbeat.incrementThrottlingCount(IKEY, "host", statusCode);
    networkStatsbeat.incrementThrottlingCount(IKEY, "host", statusCode);
    assertThat(networkStatsbeat.getThrottlingCount(IKEY, statusCode)).isEqualTo(2);
  }

  @Test
  public void testIncrementExceptionCount() {
    String exceptionType = NullPointerException.class.getName();
    assertThat(networkStatsbeat.getExceptionCount(IKEY, exceptionType)).isEqualTo(0);
    networkStatsbeat.incrementExceptionCount(IKEY, "host", exceptionType);
    networkStatsbeat.incrementExceptionCount(IKEY, "host", exceptionType);
    assertThat(networkStatsbeat.getExceptionCount(IKEY, exceptionType)).isEqualTo(2);
  }

  @Test
  public void testRaceCondition() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(100);
    for (int i = 0; i < 100; i++) {
      executorService.execute(
          new Runnable() {
            @Override
            public void run() {
              for (int j = 0; j < 1000; j++) {
                networkStatsbeat.incrementRequestSuccessCount(j % 2 == 0 ? 5 : 10, IKEY, "host");
                networkStatsbeat.incrementRequestFailureCount(IKEY, "host", 400);
                networkStatsbeat.incrementRetryCount(IKEY, "host", 500);
                networkStatsbeat.incrementThrottlingCount(IKEY, "host", 402);
                networkStatsbeat.incrementExceptionCount(
                    IKEY, "host", NullPointerException.class.getName());
              }
            }
          });
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.MINUTES);
    assertThat(networkStatsbeat.getRequestSuccessCount(IKEY)).isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestFailureCount(IKEY, 400)).isEqualTo(100000);
    assertThat(networkStatsbeat.getRetryCount(IKEY, 500)).isEqualTo(100000);
    assertThat(networkStatsbeat.getThrottlingCount(IKEY, 402)).isEqualTo(100000);
    assertThat(networkStatsbeat.getExceptionCount(IKEY, NullPointerException.class.getName()))
        .isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestDurationAvg(IKEY)).isEqualTo(7.5);
  }

  @Test
  public void testGetHost() {
    String url = "https://fakehost-1.example.com/";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost-1");

    url = "https://fakehost-2.example.com/";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost-2");

    url = "http://www.fakehost-3.example.com/";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost-3");

    url = "http://www.fakehost.com/v2/track";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost");

    url = "https://www.fakehost0-4.com/";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost0-4");

    url = "https://www.fakehost-5.com";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost-5");

    url = "https://fakehost.com";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost");

    url = "http://fakehost-5/";
    assertThat(NetworkStatsbeat.getHost(url)).isEqualTo("fakehost-5");
  }
}
