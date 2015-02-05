/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.core.volume;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;

/**
 * Created by gupele on 2/5/2015.
 */
final class Tester {
    private final static String TEST_IKEY = "00000000-0000-0000-0000-000000000000";

    private int numberOfTelemetries;

    private long maxTimeToWaitInSeconds;

    private final FakeTransmissionOutput fakeTransmissionOutput;

    private final TestResultsVerifier testResultsVerifier;

    private final TelemetryClient telemetryClient;

    private long sendTimeInNanos;

    private int acceptedUntilEndOfSending;

    public Tester() {
        TelemetryConfiguration configuration = new TelemetryConfiguration();
        configuration.setInstrumentationKey(TEST_IKEY);
        configuration.setChannel(new InProcessTelemetryChannel());
        fakeTransmissionOutput = TestThreadLocalData.get();
        testResultsVerifier = fakeTransmissionOutput.getTestResultsVerifier();
        telemetryClient = new TelemetryClient(configuration);
    }

    public void reset(int numberOfTelemetries, long maxTimeToWaitInSeconds) {
        this.numberOfTelemetries = numberOfTelemetries;
        this.maxTimeToWaitInSeconds = maxTimeToWaitInSeconds;
        testResultsVerifier.reset(numberOfTelemetries);
    }

    public TestStats getResults() {
        return testResultsVerifier.getResults(sendTimeInNanos, acceptedUntilEndOfSending);
    }

    public void sendTelemetries() {
        testResultsVerifier.reset(numberOfTelemetries);
        Thread sender = new Thread(new Runnable() {
            @Override
            public void run() {
                long runElapsed = System.nanoTime();
                int i = 0;
                for (; i < numberOfTelemetries; ++i) {
                    int mod = i % 4;
                    switch (mod) {
                        case 0:
                            telemetryClient.trackEvent("EName");
                            break;

                        case 1:
                            telemetryClient.trackMetric("Name", 120.9);
                            break;

                        case 2:
                            telemetryClient.trackTrace("TMessage");
                            break;

                        default:
                            telemetryClient.trackPageView("PName");
                            break;
                    }
                }
                sendTimeInNanos = System.nanoTime() - runElapsed;
                acceptedUntilEndOfSending = testResultsVerifier.getCurrentAccepted();
            }
        });
        sender.setDaemon(true);
        sender.start();
        testResultsVerifier.waitFor(maxTimeToWaitInSeconds);
    }
}
