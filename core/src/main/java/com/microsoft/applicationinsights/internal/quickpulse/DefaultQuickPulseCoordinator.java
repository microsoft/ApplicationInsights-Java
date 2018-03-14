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

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * Created by gupele on 12/14/2016.
 */
final class DefaultQuickPulseCoordinator implements QuickPulseCoordinator, Runnable {
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
        waitBetweenPostsInMS = initData.waitBetweenPingsInMS;
        waitOnErrorInMS = initData.waitBetweenPingsInMS;
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
                try {
                    Thread.sleep(sleepInMS);
                } catch (InterruptedException e) {
                }
            }
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
        }
    }

    private long sendData() {
        dataFetcher.prepareQuickPulseDataForSend();
        final QuickPulseStatus currentQPStatus = dataSender.getQuickPulseStatus();
        switch (currentQPStatus) {
            case ERROR:
                pingMode = true;
                return waitOnErrorInMS;

            case QP_IS_OFF:
                pingMode = true;
                return waitBetweenPingsInMS;

            case QP_IS_ON:
                return waitBetweenPostsInMS;

            default:
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Critical error while sending QP data: unknown status, aborting");
                QuickPulseDataCollector.INSTANCE.disable();
                stopped = true;
                return 0;
        }
    }

    private long ping() {
        QuickPulseStatus pingResult = pingSender.ping();
        switch (pingResult) {
            case ERROR:
                return waitOnErrorInMS;

            case QP_IS_ON:
                pingMode = false;
                dataSender.startSending();
                return waitBetweenPostsInMS;

            case QP_IS_OFF:
                return waitBetweenPingsInMS;

            default:
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Critical error while ping QP: unknown status, aborting");
                QuickPulseDataCollector.INSTANCE.disable();
                stopped = true;
                return 0;
        }
    }

    public void stop() {
            stopped = true;
    }
}
