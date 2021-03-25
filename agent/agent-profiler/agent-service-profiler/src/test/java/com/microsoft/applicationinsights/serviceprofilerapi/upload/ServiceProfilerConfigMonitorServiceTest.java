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
package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ClientClosedException;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerConfigMonitorService;
import org.junit.*;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class ServiceProfilerConfigMonitorServiceTest {

    @Test
    public void pullsConfig() throws ClientClosedException, IOException, URISyntaxException {

        AtomicReference<Runnable> job = new AtomicReference<>();

        ScheduledExecutorService executorService = mockScheduledExecutorService(job::set);
        ServiceProfilerClientV2 serviceProfilerClient = mockServiceProfilerClient();

        ServiceProfilerConfigMonitorService serviceMonitor = new ServiceProfilerConfigMonitorService(
                executorService,
                100,
                serviceProfilerClient
        );

        AtomicReference<ProfilerConfiguration> config = new AtomicReference<>();
        serviceMonitor.initialize(Collections.singletonList(config::set));

        Assert.assertNotNull(job.get());

        job.get().run();

        Mockito.verify(serviceProfilerClient, Mockito.times(1)).getSettings(Matchers.any(Date.class));
        Assert.assertNotNull(config.get());

        Assert.assertEquals("--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker", config.get().getCollectionPlan());
        Assert.assertEquals("--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400", config.get().getCpuTriggerConfiguration());
        Assert.assertEquals("--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120", config.get().getDefaultConfiguration());
        Assert.assertEquals("--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400", config.get().getMemoryTriggerConfiguration());

    }

    private ScheduledExecutorService mockScheduledExecutorService(Consumer<Runnable> job) {
        ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);

        Mockito.doAnswer(invocation -> {
            job.accept(invocation
                    .getArgumentAt(0, Runnable.class));

            return null;
        }).when(executorService).scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(TimeUnit.class));
        return executorService;
    }

    public static ServiceProfilerClientV2 mockServiceProfilerClient() throws IOException, URISyntaxException, ClientClosedException {
        ServiceProfilerClientV2 serviceProfilerClient = Mockito.mock(ServiceProfilerClientV2.class);
        Mockito.when(serviceProfilerClient.getSettings(Matchers.any(Date.class))).thenReturn("{\"id\":\"8929ed2e-24da-4ad4-8a8b-5a5ebc03abb4\",\"lastModified\":\"2021-01-25T15:46:11" +
                ".0900613+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"--single --mode immediate --immediate-profiling-duration 120  " +
                "--expiration 5249157885138288517 --settings-moniker a-settings-moniker\",\"cpuTriggerConfiguration\":\"--cpu-trigger-enabled true --cpu-threshold 80 " +
                "--cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400\",\"memoryTriggerConfiguration\":\"--memory-trigger-enabled true --memory-threshold 20 " +
                "--memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400\",\"defaultConfiguration\":\"--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120\"," +
                "\"geoOverride\":null}");
        return serviceProfilerClient;
    }
}
