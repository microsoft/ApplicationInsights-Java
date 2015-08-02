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

package com.microsoft.applicationinsights.extensibility.initializer.docker.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 7/29/2015.
 */
public class DockerContextPollerTests {

    private static File contextFileMock = mock(File.class);
    private static DockerContextFactory dockerContextFactoryMock = mock(DockerContextFactory.class);
    private static DockerContextPoller contextPollerUnderTest;
    private static DockerContext dockerContextMock = mock(DockerContext.class);

    @Before
    public void testInit() throws Exception {
        when(dockerContextFactoryMock.createDockerContext(any(File.class))).thenReturn(dockerContextMock);

        contextPollerUnderTest = new DockerContextPoller(contextFileMock, dockerContextFactoryMock);
        contextPollerUnderTest.THREAD_POLLING_INTERVAL_MS = 0;
    }

    @Test
    public void testDockerContextInitializedIfFileExists() {
        when(contextFileMock.exists()).thenReturn(true);
        contextPollerUnderTest.run();

        DockerContext dockerContext = contextPollerUnderTest.getDockerContext();

        Assert.assertEquals(dockerContextMock, dockerContext);
    }

    @Test
    public void testIfContextFileNotExistThenPollerTryAgain() {
        final int numberOfRetries = 10;
        final int[] count = {0};
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ++count[0] == numberOfRetries ;
            }
        }).when(contextFileMock).exists();

        contextPollerUnderTest.run();

        verify(contextFileMock, times(numberOfRetries)).exists();
    }
}
