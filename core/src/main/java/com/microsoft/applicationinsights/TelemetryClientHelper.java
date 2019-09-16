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

package com.microsoft.applicationinsights;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

public final class TelemetryClientHelper {

    private final TelemetryClient telemetryClient;

    public TelemetryClientHelper() {
        telemetryClient = new TelemetryClient();
    }

    public TelemetryClientHelper(TelemetryClient client) {
        telemetryClient = client;
    }

    public TelemetryClient getTelemetryClient() {
        return this.telemetryClient;
    }

    public CloseableHttpResponse monitorHttpDependency(String dependencyName, String commandName,
            CloseableHttpClient httpClient, HttpUriRequest httpRequest) throws Exception {
        Instant start = Instant.now();
        CloseableHttpResponse response = null;
        HttpContext context = HttpClientContext.create();

        try {
            response = httpClient.execute(httpRequest, context);
        } catch (Exception ex) {
            trackDependencyException(dependencyName, commandName, ex);
            throw ex;
        } finally {
            Instant finish = Instant.now();
            RemoteDependencyTelemetry dependency = buildTelemetryEvent(response, httpRequest, dependencyName,
                    commandName, start, finish);
            telemetryClient.trackDependency(dependency);
        }

        return response;
    }

    public <T> T monitorDependency(String dependencyName, String commandName, Callable<T> callable) throws Exception {
        boolean success = false;
        Instant start = Instant.now();
        T result = null;

        try {
            result = callable.call();
            success = true;
        } catch (Exception ex) {
            trackDependencyException(dependencyName, commandName, ex);
            throw ex;
        } finally {
            Instant finish = Instant.now();
            com.microsoft.applicationinsights.telemetry.Duration duration = new com.microsoft.applicationinsights.telemetry.Duration(
                    Duration.between(start, finish).toMillis());
            telemetryClient.trackDependency(dependencyName, commandName, duration, success);
        }

        return result;
    }

    private void trackDependencyException(String dependencyName, String commandName, Exception ex) {
        Map<String, String> exceptionProperties = new HashMap<>(2);
        exceptionProperties.put("dependencyName", dependencyName);
        exceptionProperties.put("commandName", commandName);

        telemetryClient.trackException(ex, exceptionProperties, null);
    }

    private RemoteDependencyTelemetry buildTelemetryEvent(HttpResponse response, HttpUriRequest httpRequest,
            String dependencyName, String commandName, Instant start, Instant finish) {
        com.microsoft.applicationinsights.telemetry.Duration duration = new com.microsoft.applicationinsights.telemetry.Duration(
                Duration.between(start, finish).toMillis());
        int statusCode = response == null ? 0 : response.getStatusLine().getStatusCode();
        boolean success = isSuccess(statusCode);

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(dependencyName, commandName, duration,
                success);

        telemetry.setResultCode(response == null ? "N/A" : response.getStatusLine().toString());
        telemetry.setType(String.format("HTTP - %s", httpRequest.getMethod()));
        return telemetry;
    }

    private boolean isSuccess(int statusCode) {
        return (statusCode >= 200 && statusCode <= 299);
    }
}
