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
import org.junit.Test;

/**
 * Created by yonisha on 7/29/2015.
 */
public class DockerContextTests {
    private static final String CONTEXT_FILE_PATTERN = "docker-host=%s,docker-image=%s,docker-container-name=%s,docker-container-id=%s";
    private static final String DEFAULT_HOST = "docker_host";
    private static final String DEFAULT_IMAGE = "docker_image";
    private static final String DEFAULT_CONTAINER_NAME = "docker_container";
    private static final String DEFAULT_CONTAINER_ID = "docker_container_id";

    @Test
    public void testContextJsonParsedCorrectly() throws Exception {
        String content = String.format(CONTEXT_FILE_PATTERN, DEFAULT_HOST, DEFAULT_IMAGE, DEFAULT_CONTAINER_NAME, DEFAULT_CONTAINER_ID);

        DockerContext dockerContext = new DockerContext(content);

        Assert.assertEquals(DEFAULT_HOST, dockerContext.getHostName());
        Assert.assertEquals(DEFAULT_IMAGE, dockerContext.getProperties().get(Constants.DOCKER_IMAGE_PROPERTY_KEY));
        Assert.assertEquals(DEFAULT_CONTAINER_NAME, dockerContext.getProperties().get(Constants.DOCKER_CONTAINER_NAME_PROPERTY_KEY));
        Assert.assertEquals(DEFAULT_CONTAINER_ID, dockerContext.getProperties().get(Constants.DOCKER_CONTAINER_ID_PROPERTY_KEY));
    }
}
