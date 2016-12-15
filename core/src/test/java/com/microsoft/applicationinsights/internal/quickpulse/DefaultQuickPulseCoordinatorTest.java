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

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * Created by gupele on 12/15/2016.
 */
public class DefaultQuickPulseCoordinatorTest {
    @Test
    public void testOnlyPings() throws InterruptedException {
        final QuickPulseDataFetcher mockFetcher = mock(QuickPulseDataFetcher.class);
        final QuickPulseDataSender mockSender = mock(QuickPulseDataSender.class);
        final QuickPulsePingSender mockPingSender = mock(QuickPulsePingSender.class);
        Mockito.doReturn(QuickPulseStatus.QP_IS_OFF).when(mockPingSender).ping();

        final QuickPulseCoordinatorInitData initData = new QuickPulseCoordinatorInitDataBuilder()
                .withDataFetcher(mockFetcher)
                .withDataSender(mockSender)
                .withPingSender(mockPingSender)
                .withWaitBetweenPingsInMS(10L)
                .withWaitBetweenPostsInMS(10L)
                .withWaitOnErrorInMS(10L)
                .build();

        final DefaultQuickPulseCoordinator coordinator = new DefaultQuickPulseCoordinator(initData);
        Thread thread = new Thread(coordinator);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(1000);
        coordinator.stop();

        thread.join();

        Mockito.verify(mockFetcher, Mockito.never()).prepareQuickPulseDataForSend();

        Mockito.verify(mockSender, Mockito.never()).startSending();
        Mockito.verify(mockSender, Mockito.never()).getQuickPulseStatus();

        Mockito.verify(mockPingSender, Mockito.atLeast(1)).ping();
    }

    @Test
    public void testOnePingAndThenOnePost() throws InterruptedException {
        final QuickPulseDataFetcher mockFetcher = mock(QuickPulseDataFetcher.class);
        final QuickPulseDataSender mockSender = mock(QuickPulseDataSender.class);
        Mockito.doReturn(QuickPulseStatus.QP_IS_OFF).when(mockSender).getQuickPulseStatus();

        final QuickPulsePingSender mockPingSender = mock(QuickPulsePingSender.class);
        Mockito.when(mockPingSender.ping()).thenReturn(QuickPulseStatus.QP_IS_ON, QuickPulseStatus.QP_IS_OFF);

        final QuickPulseCoordinatorInitData initData = new QuickPulseCoordinatorInitDataBuilder()
                .withDataFetcher(mockFetcher)
                .withDataSender(mockSender)
                .withPingSender(mockPingSender)
                .withWaitBetweenPingsInMS(10L)
                .withWaitBetweenPostsInMS(10L)
                .withWaitOnErrorInMS(10L)
                .build();

        final DefaultQuickPulseCoordinator coordinator = new DefaultQuickPulseCoordinator(initData);
        Thread thread = new Thread(coordinator);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(1000);
        coordinator.stop();

        thread.join();

        Mockito.verify(mockFetcher, Mockito.atLeast(1)).prepareQuickPulseDataForSend();

        Mockito.verify(mockSender, Mockito.times(1)).startSending();
        Mockito.verify(mockSender, Mockito.times(1)).getQuickPulseStatus();

        Mockito.verify(mockPingSender, Mockito.atLeast(1)).ping();
    }
}