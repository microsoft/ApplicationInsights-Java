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

package com.microsoft.applicationinsights.web.internal;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import com.microsoft.applicationinsights.web.utils.ThreadContextValidator;

import java.util.ArrayList;

/**
 * Created by yonisha on 2/16/2015.
 */
public class ThreadContextTests {

    /**
     * Here we're using a large number of threads in order to increase the probability that threads will interrupt
     * each other in case of a wrong logic.
     */
    private final int NUMBER_OF_VALIDATOR_THREADS = 10000;

    @Test
    public void testConcurrentThreadsGetTheirOwnContext() throws InterruptedException {
        List<ThreadContextValidator> threadContextValidators = new ArrayList<ThreadContextValidator>();

        // Initializing validators.
        for (int i = 0; i < NUMBER_OF_VALIDATOR_THREADS; i++) {
            threadContextValidators.add(new ThreadContextValidator());
        }

        // Executing all validators.
        for (ThreadContextValidator validator : threadContextValidators) {
            validator.start();
        }

        // Wait for all threads to complete.
        for (ThreadContextValidator validator : threadContextValidators) {
            validator.join();
        }
    }

    @Test
    public void testNewCreatedThreadTheParentContextByDefault() throws InterruptedException {
        final String expectedRequestName = "inherited_context";
        RequestTelemetryContext requestTelemetryContext = new RequestTelemetryContext(0);
        requestTelemetryContext.getHttpRequestTelemetry().setName(expectedRequestName);

        ThreadContext.setRequestTelemetryContext(requestTelemetryContext);

        final RequestTelemetryContext[] context = new RequestTelemetryContext[1];
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                context[0] = ThreadContext.getRequestTelemetryContext();
            }
        });

        thread.start();
        thread.join();

        Assert.assertNotNull(context[0]);
        Assert.assertSame(expectedRequestName, ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry()
            .getName());
    }

    @Test
    public void testChildThreadDoesnotGetContextUpdatedWhenrelyingonItls()
        throws InterruptedException {
        final String expectedRequestName = "inherited_context";
        RequestTelemetryContext requestTelemetryContext = new RequestTelemetryContext(0);
        requestTelemetryContext.getHttpRequestTelemetry().setName(expectedRequestName);

        ThreadContext.setRequestTelemetryContext(requestTelemetryContext);

        final RequestTelemetryContext[] context = new RequestTelemetryContext[1];
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                context[0] = ThreadContext.getRequestTelemetryContext();
            }
        });

        thread.start();
        thread.join();


        final String expectedRequestName1 = "inherited_context1";
        RequestTelemetryContext requestTelemetryContext1 = new RequestTelemetryContext(0);
        requestTelemetryContext.getHttpRequestTelemetry().setName(expectedRequestName1);
        ThreadContext.setRequestTelemetryContext(requestTelemetryContext1);

        Assert.assertNotSame(expectedRequestName, ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry()
            .getName());
    }

    @Test
    public void testNewThreadCreatedWithWrappedRunnableGetsTheParentContext() throws InterruptedException {
        final String expectedRequestName = "inherited_context";
        RequestTelemetryContext requestTelemetryContext = new RequestTelemetryContext(0);
        requestTelemetryContext.getHttpRequestTelemetry().setName(expectedRequestName);

        ThreadContext.setRequestTelemetryContext(requestTelemetryContext);

        final RequestTelemetryContext[] context = new RequestTelemetryContext[1];
        Thread thread = new Thread(new MyRunnable(new Runnable() {
            @Override
            public void run() {
                    context[0] = ThreadContext.getRequestTelemetryContext();
            }
        }, requestTelemetryContext));

        thread.start();
        thread.join();

        Assert.assertNotNull(context[0]);
        Assert.assertSame(expectedRequestName, ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry()
                            .getName());
    }

    private static class MyRunnable implements Runnable {
        private final Runnable task;
        private final RequestTelemetryContext rtc;

        MyRunnable(Runnable task,
            RequestTelemetryContext rtc) {
            this.task = task;
            this.rtc = rtc;
        }

        @Override
        public void run() {
            if (ThreadContext.getRequestTelemetryContext() != null) {
                ThreadContext.remove();
            }
            ThreadContext.setRequestTelemetryContext(rtc);
            task.run();
        }
    }
}
