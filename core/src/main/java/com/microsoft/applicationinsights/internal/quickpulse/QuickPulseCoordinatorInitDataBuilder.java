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

package com.microsoft.applicationinsights.internal.quickpulse;

/** Created by gupele on 12/15/2016. */
final class QuickPulseCoordinatorInitDataBuilder {
  private static final long DEFAULT_WAIT_BETWEEN_PING_IN_MS = 5000;
  private static final long DEFAULT_WAIT_BETWEEN_POSTS_IN_MS = 1000;
  private static final long DEFAULT_WAIT_BETWEEN_PINGS_AFTER_ERROR_IN_MS = 60000;

  private QuickPulsePingSender pingSender;
  private QuickPulseDataSender dataSender;
  private QuickPulseDataFetcher dataFetcher;
  private long waitBetweenPingsInMS = DEFAULT_WAIT_BETWEEN_PING_IN_MS;
  private long waitBetweenPostsInMS = DEFAULT_WAIT_BETWEEN_POSTS_IN_MS;
  private long waitOnErrorInMS = DEFAULT_WAIT_BETWEEN_PINGS_AFTER_ERROR_IN_MS;

  public QuickPulseCoordinatorInitDataBuilder withWaitBetweenPingsInMS(long waitBetweenPingsInMS) {
    this.waitBetweenPingsInMS = waitBetweenPingsInMS;
    return this;
  }

  public QuickPulseCoordinatorInitDataBuilder withWaitBetweenPostsInMS(long waitBetweenPostsInMS) {
    this.waitBetweenPostsInMS = waitBetweenPostsInMS;
    return this;
  }

  public QuickPulseCoordinatorInitDataBuilder withWaitOnErrorInMS(long waitOnErrorInMS) {
    this.waitOnErrorInMS = waitOnErrorInMS;
    return this;
  }

  public QuickPulseCoordinatorInitDataBuilder withPingSender(QuickPulsePingSender pingSender) {
    this.pingSender = pingSender;
    return this;
  }

  public QuickPulseCoordinatorInitDataBuilder withDataSender(QuickPulseDataSender dataSender) {
    this.dataSender = dataSender;
    return this;
  }

  public QuickPulseCoordinatorInitDataBuilder withDataFetcher(QuickPulseDataFetcher dataFetcher) {
    this.dataFetcher = dataFetcher;
    return this;
  }

  public QuickPulseCoordinatorInitData build() {
    if (pingSender == null) {
      throw new NullPointerException("ping sender should not be null");
    }
    if (dataFetcher == null) {
      throw new NullPointerException("data fetcher should not be null");
    }
    if (dataSender == null) {
      throw new NullPointerException("data sender should not be null");
    }

    return new QuickPulseCoordinatorInitData(
        pingSender,
        dataFetcher,
        dataSender,
        waitBetweenPingsInMS,
        waitBetweenPostsInMS,
        waitOnErrorInMS);
  }
}
