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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yonisha on 7/29/2015.
 *
 * Represents the Docker context, which includes the host name, image name, container name and container ID.
 * The Docker context file is written in the following structure:
 *      Docker host=host_name,Docker image=image_name,Docker container id=con_id,Docker container name=con_name
 */
public class DockerContext {
    private String hostName;
    private Map<String, String> properties = new HashMap<String, String>();

    public DockerContext(String json) throws Exception {
        extract(json);
    }

    public String getHostName() {
        return this.hostName;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    private void extract(String context) throws Exception {
        String[] properties = context.split(",");

        for (String kv : properties) {
            String[] split = kv.split("=");
            String key = split[0];
            String value = split[1];

            if (key.equalsIgnoreCase(Constants.DOCKER_HOST_PROPERTY_KEY)) {
                this.hostName = value;
            } else {
                this.properties.put(key, value);
            }
        }
    }
}