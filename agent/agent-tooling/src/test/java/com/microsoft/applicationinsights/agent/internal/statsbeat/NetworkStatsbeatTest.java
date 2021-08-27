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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NetworkStatsbeatTest {

  private NetworkStatsbeat networkStatsbeat;
  private List<String> ikeys;

  @BeforeEach
  public void init() {
    ikeys = new ArrayList<>();
    ikeys.add("00000000-0000-0000-0000-0FEEDDADBEEF");
    networkStatsbeat = new NetworkStatsbeat(new CustomDimensions(), ikeys);
  }

  @Test
  public void testIncrementRequestSuccessCount() {
    assertThat(networkStatsbeat.getRequestSuccessCount(ikeys.get(0))).isEqualTo(0);
    assertThat(networkStatsbeat.getRequestDurationAvg(ikeys.get(0))).isEqualTo(0);
    networkStatsbeat.incrementRequestSuccessCount(1000, ikeys.get(0));
    networkStatsbeat.incrementRequestSuccessCount(3000, ikeys.get(0));
    assertThat(networkStatsbeat.getRequestSuccessCount(ikeys.get(0))).isEqualTo(2);
    assertThat(networkStatsbeat.getRequestDurationAvg(ikeys.get(0))).isEqualTo(2000.0);
  }

  @Test
  public void testIncrementRequestFailureCount() {
    assertThat(networkStatsbeat.getRequestFailureCount(ikeys.get(0))).isEqualTo(0);
    networkStatsbeat.incrementRequestFailureCount(ikeys.get(0));
    networkStatsbeat.incrementRequestFailureCount(ikeys.get(0));
    assertThat(networkStatsbeat.getRequestFailureCount(ikeys.get(0))).isEqualTo(2);
  }

  @Test
  public void testIncrementRetryCount() {
    assertThat(networkStatsbeat.getRetryCount(ikeys.get(0))).isEqualTo(0);
    networkStatsbeat.incrementRetryCount(ikeys.get(0));
    networkStatsbeat.incrementRetryCount(ikeys.get(0));
    assertThat(networkStatsbeat.getRetryCount(ikeys.get(0))).isEqualTo(2);
  }

  @Test
  public void testIncrementThrottlingCount() {
    assertThat(networkStatsbeat.getThrottlingCount(ikeys.get(0))).isEqualTo(0);
    networkStatsbeat.incrementThrottlingCount(ikeys.get(0));
    networkStatsbeat.incrementThrottlingCount(ikeys.get(0));
    assertThat(networkStatsbeat.getThrottlingCount(ikeys.get(0))).isEqualTo(2);
  }

  @Test
  public void testIncrementExceptionCount() {
    assertThat(networkStatsbeat.getExceptionCount(ikeys.get(0))).isEqualTo(0);
    networkStatsbeat.incrementExceptionCount(ikeys.get(0));
    networkStatsbeat.incrementExceptionCount(ikeys.get(0));
    assertThat(networkStatsbeat.getExceptionCount(ikeys.get(0))).isEqualTo(2);
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
                networkStatsbeat.incrementRequestSuccessCount(j % 2 == 0 ? 5 : 10, ikeys.get(0));
                networkStatsbeat.incrementRequestFailureCount(ikeys.get(0));
                networkStatsbeat.incrementRetryCount(ikeys.get(0));
                networkStatsbeat.incrementThrottlingCount(ikeys.get(0));
                networkStatsbeat.incrementExceptionCount(ikeys.get(0));
              }
            }
          });
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.MINUTES);
    assertThat(networkStatsbeat.getRequestSuccessCount(ikeys.get(0))).isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestFailureCount(ikeys.get(0))).isEqualTo(100000);
    assertThat(networkStatsbeat.getRetryCount(ikeys.get(0))).isEqualTo(100000);
    assertThat(networkStatsbeat.getThrottlingCount(ikeys.get(0))).isEqualTo(100000);
    assertThat(networkStatsbeat.getExceptionCount(ikeys.get(0))).isEqualTo(100000);
    assertThat(networkStatsbeat.getRequestDurationAvg(ikeys.get(0))).isEqualTo(7.5);
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
}
