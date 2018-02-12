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

package com.microsoft.applicationinsights.extensibility.initializer.docker;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.*;
import com.microsoft.applicationinsights.extensibility.initializer.docker.internal.Constants;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 7/29/2015.
 */
public class DockerContextInitializerTests {

    private static final String DEFAULT_HOST = "docker_host";
    private static final String DEFAULT_IMAGE = "docker_image";
    private static final String DEFAULT_CONTAINER_NAME = "docker_container";
    private static final String DEFAULT_CONTAINER_ID = "docker_container_id";

    private static DockerContextInitializer initializerUnderTest;
    private static DockerContextPoller contextPollerMock = mock(DockerContextPoller.class);
    private static FileFactory fileFactoryMock = mock(FileFactory.class);
    private static DockerContext defaultDockerContext;
    private static Telemetry telemetry;

    @BeforeClass
    public static void classInit() throws Exception {
        String json = String.format(com.microsoft.applicationinsights.extensibility.initializer.docker.ConstantsTest.CONTEXT_FILE_PATTERN, DEFAULT_HOST, DEFAULT_IMAGE, DEFAULT_CONTAINER_NAME, DEFAULT_CONTAINER_ID);
        defaultDockerContext = new DockerContext(json);
        initializerUnderTest = new DockerContextInitializer(fileFactoryMock, contextPollerMock);
    }

    @Before
    public void testInit() {
        reset(contextPollerMock);
        when(contextPollerMock.getDockerContext()).thenReturn(defaultDockerContext);
        when(contextPollerMock.isCompleted()).thenReturn(true);
        telemetry = new TraceTelemetry();
    }

    @Test
    public void testTelemetryInitializedWhenContextAvailable() {
        initializerUnderTest.initialize(telemetry);

        Map<String, String> properties = telemetry.getProperties();
        Assert.assertEquals(DEFAULT_CONTAINER_NAME, telemetry.getContext().getDevice().getId());
        Assert.assertEquals(DEFAULT_IMAGE, properties.get(Constants.DOCKER_IMAGE_PROPERTY_KEY));
        Assert.assertEquals(DEFAULT_CONTAINER_NAME, properties.get(Constants.DOCKER_CONTAINER_NAME_PROPERTY_KEY));
        Assert.assertEquals(DEFAULT_CONTAINER_ID, properties.get(Constants.DOCKER_CONTAINER_ID_PROPERTY_KEY));
    }

    @Test
    public void testTelemetryPropertiesNotInitializedWhenAlreadyPopulated() {
        final String host = "predefined_host";
        telemetry.getProperties().put(Constants.DOCKER_HOST_PROPERTY_KEY, host);
        initializerUnderTest.initialize(telemetry);

        Assert.assertEquals(host, telemetry.getProperties().get(Constants.DOCKER_HOST_PROPERTY_KEY));
    }

    @Test
    public void testWhenTelemetryPropertiesAlreadyPopulatedDeviceIdStillSet() {
        final String host = "predefined_host";
        telemetry.getProperties().put(Constants.DOCKER_HOST_PROPERTY_KEY, host);
        initializerUnderTest.initialize(telemetry);

        Assert.assertEquals(host, telemetry.getProperties().get(Constants.DOCKER_HOST_PROPERTY_KEY));
        Assert.assertEquals(DEFAULT_CONTAINER_NAME, telemetry.getContext().getDevice().getId());
    }

    @Test
    public void testInitializerNotThrowWhenContextIsNull() {
        when(contextPollerMock.getDockerContext()).thenReturn(null);

        initializerUnderTest.initialize(telemetry);

        verify(contextPollerMock).getDockerContext();
    }

    @Test
    // Performance - since the poller member DockerContext is volatile the access to it is expensive.
    // Therefore we access to it only when the poller thread finished its execution and the context is not null.
    public void testDockerContextNotAccessedIfPollerNotCompleted() {
        when(contextPollerMock.isCompleted()).thenReturn(false);

        initializerUnderTest.initialize(telemetry);

        verify(contextPollerMock, times(0)).getDockerContext();
    }

    @Test
    public void testSDKInfoFileIsWrittenWithInstrumentationKey() throws IOException {
        // The expected instrumentation key below is taken from the ApplicationInsights.xml under the resources folder.
        final String expectedSdkInfo = "InstrumentationKey=A-test-instrumentation-key";

        reset(fileFactoryMock);
        String sdkInfoFilePath = String.format("%s/%s", Constants.AI_SDK_DIRECTORY, Constants.AI_SDK_INFO_FILENAME);

        initializerUnderTest = new DockerContextInitializer(fileFactoryMock, contextPollerMock);
        initializerUnderTest.initialize(telemetry);

        verify(fileFactoryMock).create(sdkInfoFilePath, expectedSdkInfo);
    }

    @Test
    public void testIfSDKInfoFileWrittenOnlyOnce() throws IOException {
        reset(fileFactoryMock);

        initializerUnderTest = new DockerContextInitializer(fileFactoryMock, contextPollerMock);

        // Sending 2 telemetries, which should invoke the file factory mock only once.
        initializerUnderTest.initialize(telemetry);
        initializerUnderTest.initialize(telemetry);

        verify(fileFactoryMock, times(1)).create(any(String.class), any(String.class));
    }

    @Test
    public void testWritingOfSdkInfoFileIsSynchonizedAndWrittenOnlyOnce() throws IOException, InterruptedException {
        reset(fileFactoryMock);

        initializerUnderTest = new DockerContextInitializer(fileFactoryMock, contextPollerMock);

        // Sending many telemetries to increase the possibility of collision in case of a bug.
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    initializerUnderTest.initialize(telemetry);
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        verify(fileFactoryMock, times(1)).create(any(String.class), any(String.class));
    }
}
