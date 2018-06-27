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

// Created by yonisha on 7/29/2015.
/** Constants for Docker SDK */
public class Constants {
  public static final String AI_SDK_DIRECTORY = "/usr/appinsights/docker";
  public static final String AI_SDK_INFO_FILENAME = "sdk.info";
  public static final String AI_SDK_INFO_FILE_CONTENT_TEMPLATE = "InstrumentationKey=%s";

  public static final String DOCKER_HOST_PROPERTY_KEY = "Docker host";
  public static final String DOCKER_IMAGE_PROPERTY_KEY = "Docker image";
  public static final String DOCKER_CONTAINER_NAME_PROPERTY_KEY = "Docker container name";
  public static final String DOCKER_CONTAINER_ID_PROPERTY_KEY = "Docker container id";
}
