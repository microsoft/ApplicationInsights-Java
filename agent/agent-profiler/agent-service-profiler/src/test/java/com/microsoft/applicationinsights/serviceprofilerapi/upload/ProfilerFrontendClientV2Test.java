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
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.microsoft.applicationinsights.serviceprofilerapi.client.ClientClosedException;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.*;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class ProfilerFrontendClientV2Test {
    @Test
    public void settingsPullHitsCorrectUrl() throws ClientClosedException, IOException, URISyntaxException {

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        ProfilerFrontendClientV2 profilerFrontendClientV2 = new ProfilerFrontendClientV2("a-host", "a-instrumentation-key", httpClient);
        Date now = Date.from(Instant.now());
        profilerFrontendClientV2.getSettings(now);

        ArgumentMatcher<HttpGet> matcher = new ArgumentMatcher<HttpGet>() {
            @Override public boolean matches(Object argument) {
                HttpGet get = ((HttpGet) argument);
                String uri = get.getURI().toString();
                return uri.contains("a-instrumentation-key") &&
                        uri.contains("/api/profileragent/v4/settings");
            }
        };

        Mockito.verify(httpClient, Mockito.times(1))
                .execute(
                        Mockito.argThat(matcher),
                        Mockito.any(ResponseHandler.class)
                );
    }

    @Test
    public void uploadHitsCorrectUrl() throws ClientClosedException, IOException, URISyntaxException {

        UUID id = UUID.randomUUID();

        ArgumentMatcher<HttpPost> matcher = new ArgumentMatcher<HttpPost>() {
            @Override public boolean matches(Object argument) {
                HttpPost get = ((HttpPost) argument);
                String uri = get.getURI().toString();
                return uri.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id.toString()) &&
                        uri.contains("action=gettoken");
            }
        };


        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        ProfilerFrontendClientV2 profilerFrontendClientV2 = new ProfilerFrontendClientV2("a-host", "a-instrumentation-key", httpClient);
        profilerFrontendClientV2.getUploadAccess(id);

        Mockito.verify(httpClient, Mockito.times(1))
                .execute(
                        Mockito.argThat(matcher),
                        Mockito.any(ResponseHandler.class)
                );
    }

    @Test
    public void uploadFinishedHitsCorrectUrl() throws ClientClosedException, IOException, URISyntaxException {

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        ProfilerFrontendClientV2 profilerFrontendClientV2 = new ProfilerFrontendClientV2("a-host", "a-instrumentation-key", httpClient);
        UUID id = UUID.randomUUID();
        profilerFrontendClientV2.reportUploadFinish(id, "an-etag");

        ArgumentMatcher<HttpPost> matcher = new ArgumentMatcher<HttpPost>() {
            @Override public boolean matches(Object argument) {
                HttpPost get = ((HttpPost) argument);
                String uri = get.getURI().toString();
                return uri.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id.toString()) &&
                        uri.contains("action=commit");
            }
        };

        Mockito.verify(httpClient, Mockito.times(1))
                .execute(
                        Mockito.argThat(matcher),
                        Mockito.any(ResponseHandler.class)
                );
    }
}
