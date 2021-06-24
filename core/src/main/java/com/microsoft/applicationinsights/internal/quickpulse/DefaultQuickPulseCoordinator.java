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

import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultQuickPulseCoordinator implements QuickPulseCoordinator, Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DefaultQuickPulseCoordinator.class);
  private String qpsServiceRedirectedEndpoint;
  private long qpsServicePollingIntervalHintMillis;

  private volatile boolean stopped = false;
  private volatile boolean pingMode = true;

  private final QuickPulsePingSender pingSender;
  private final QuickPulseDataFetcher dataFetcher;
  private final QuickPulseDataSender dataSender;

  private final long waitBetweenPingsInMS;
  private final long waitBetweenPostsInMS;
  private final long waitOnErrorInMS;

  public DefaultQuickPulseCoordinator(QuickPulseCoordinatorInitData initData) {
    dataSender = initData.dataSender;
    pingSender = initData.pingSender;
    dataFetcher = initData.dataFetcher;

    waitBetweenPingsInMS = initData.waitBetweenPingsInMS;
    waitBetweenPostsInMS = initData.waitBetweenPostsInMS;
    waitOnErrorInMS = initData.waitBetweenPingsInMS;
    qpsServiceRedirectedEndpoint = null;
    qpsServicePollingIntervalHintMillis = -1;
  }

  @Override
  public void run() {
    try {
      while (!stopped) {
        long sleepInMS;
        if (pingMode) {
          sleepInMS = ping();
        } else {
          sleepInMS = sendData();
        }
        Thread.sleep(sleepInMS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      // chomp
    }
  }

  private long sendData() {
    dataFetcher.prepareQuickPulseDataForSend(qpsServiceRedirectedEndpoint);
    QuickPulseHeaderInfo currentQuickPulseHeaderInfo = dataSender.getQuickPulseHeaderInfo();

    this.handleReceivedHeaders(currentQuickPulseHeaderInfo);

    switch (currentQuickPulseHeaderInfo.getQuickPulseStatus()) {
      case ERROR:
        pingMode = true;
        return waitOnErrorInMS;

      case QP_IS_OFF:
        pingMode = true;
        return qpsServicePollingIntervalHintMillis > 0
            ? qpsServicePollingIntervalHintMillis
            : waitBetweenPingsInMS;

      case QP_IS_ON:
        return waitBetweenPostsInMS;
    }

    logger.error("Critical error while sending QP data: unknown status, aborting");
    QuickPulseDataCollector.INSTANCE.disable();
    stopped = true;
    return 0;
  }

  private long ping() {
    QuickPulseHeaderInfo pingResult = pingSender.ping(qpsServiceRedirectedEndpoint);
    this.handleReceivedHeaders(pingResult);
    switch (pingResult.getQuickPulseStatus()) {
      case ERROR:
        return waitOnErrorInMS;

      case QP_IS_ON:
        pingMode = false;
        dataSender.startSending();
        return waitBetweenPostsInMS;
      case QP_IS_OFF:
        return qpsServicePollingIntervalHintMillis > 0
            ? qpsServicePollingIntervalHintMillis
            : waitBetweenPingsInMS;
    }

    logger.error("Critical error while ping QP: unknown status, aborting");
    QuickPulseDataCollector.INSTANCE.disable();
    stopped = true;
    return 0;
  }

  private void handleReceivedHeaders(QuickPulseHeaderInfo currentQuickPulseHeaderInfo) {
    String redirectLink = currentQuickPulseHeaderInfo.getQpsServiceEndpointRedirect();
    if (!LocalStringsUtils.isNullOrEmpty(redirectLink)) {
      qpsServiceRedirectedEndpoint = redirectLink;
    }

    long newPollingInterval = currentQuickPulseHeaderInfo.getQpsServicePollingInterval();
    if (newPollingInterval > 0) {
      qpsServicePollingIntervalHintMillis = newPollingInterval;
    }
  }

  public void stop() {
    stopped = true;
  }
}
