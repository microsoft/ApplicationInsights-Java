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
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.azure.core.http.*;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilerFrontendClientV2Test {
    @Test
    void settingsPullHitsCorrectUrl() throws IOException {

        AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(request -> {
                    requestHolder.set(request);
                    return Mono.just(new MockHttpResponse(request, 200));
                })
                .build();

        ProfilerFrontendClientV2 profilerFrontendClientV2 =
                new ProfilerFrontendClientV2(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

        Date now = Date.from(Instant.now());
        profilerFrontendClientV2.getSettings(now);

        HttpRequest request = requestHolder.get();
        String url = request.getUrl().toString();

        assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(url.contains("a-instrumentation-key")).isTrue();
        assertThat(url.contains("/api/profileragent/v4/settings")).isTrue();
    }

    @Test
    void uploadHitsCorrectUrl() throws IOException {

        AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(request -> {
                    requestHolder.set(request);
                    return Mono.just(new MockHttpResponse(request, 200));
                })
                .build();

        ProfilerFrontendClientV2 profilerFrontendClientV2 =
                new ProfilerFrontendClientV2(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

        UUID id = UUID.randomUUID();
        profilerFrontendClientV2.getUploadAccess(id);

        HttpRequest request = requestHolder.get();
        String url = request.getUrl().toString();

        assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id)).isTrue();
        assertThat(url.contains("action=gettoken")).isTrue();
    }

    @Test
    void uploadFinishedHitsCorrectUrl() throws IOException {

        AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(request -> {
                    requestHolder.set(request);
                    return Mono.just(new MockHttpResponse(request, 200));
                })
                .build();

        ProfilerFrontendClientV2 profilerFrontendClientV2 =
                new ProfilerFrontendClientV2(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

        UUID id = UUID.randomUUID();
        profilerFrontendClientV2.reportUploadFinish(id, "an-etag");

        HttpRequest request = requestHolder.get();
        String url = request.getUrl().toString();

        assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id)).isTrue();
        assertThat(url.contains("action=commit")).isTrue();
    }
}
