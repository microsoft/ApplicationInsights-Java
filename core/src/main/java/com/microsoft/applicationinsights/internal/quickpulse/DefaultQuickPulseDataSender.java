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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

/**
 * Created by gupele on 12/12/2016.
 */
final class DefaultQuickPulseDataSender implements QuickPulseDataSender {

    private final QuickPulseNetworkHelper networkHelper = new QuickPulseNetworkHelper();
    private final HttpClient httpClient;
    private volatile QuickPulseHeaderInfo quickPulseHeaderInfo;
    private volatile boolean stopped = false;
    private long lastValidTransmission = 0;

    private final ArrayBlockingQueue<HttpPost> sendQueue;

    public DefaultQuickPulseDataSender(final HttpClient httpClient, final ArrayBlockingQueue<HttpPost> sendQueue) {
        this.httpClient = httpClient;
        this.sendQueue = sendQueue;
    }

    @Override
    public void run() {
        try {
            while (!stopped) {
                HttpPost post = sendQueue.take();
                if (quickPulseHeaderInfo.getQuickPulseStatus() != QuickPulseStatus.QP_IS_ON) {
                    continue;
                }

                final long sendTime = System.nanoTime();
                HttpResponse response = null;
                try {
                    response = httpClient.execute(post);
                    if (networkHelper.isSuccess(response)) {
                        QuickPulseHeaderInfo quickPulseHeaderInfo = networkHelper.getQuickPulseHeaderInfo(response);
                        switch (quickPulseHeaderInfo.getQuickPulseStatus()) {
                            case QP_IS_OFF:
                            case QP_IS_ON:
                                lastValidTransmission = sendTime;
                                this.quickPulseHeaderInfo = quickPulseHeaderInfo;
                                break;

                            case ERROR:
                                onPostError(sendTime);
                                break;

                            default:
                                break;
                        }
                    }
                } catch (IOException e) {
                    onPostError(sendTime);
                } finally {
                    if (response != null) {
                        LazyHttpClient.dispose(response);
                    }
                }
            }
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                stopped = true;
                quickPulseHeaderInfo = new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }

    @Override
    public void startSending() {
        if (!stopped) {
            quickPulseHeaderInfo = new QuickPulseHeaderInfo(QuickPulseStatus.QP_IS_ON);
        }
    }

    @Override
    public QuickPulseHeaderInfo getQuickPulseHeaderInfo() {
        return quickPulseHeaderInfo;
    }

    @Override
    public void stop() {
        stopped = true;
        quickPulseHeaderInfo = new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
    }

    private void onPostError(long sendTime) {
        if (stopped) {
            return;
        }

        final double timeFromLastValidTransmission = (sendTime - lastValidTransmission) / 1000000000.0;
        if (timeFromLastValidTransmission >= 20.0) {
            quickPulseHeaderInfo = new QuickPulseHeaderInfo(QuickPulseStatus.ERROR);
        }
    }
}
