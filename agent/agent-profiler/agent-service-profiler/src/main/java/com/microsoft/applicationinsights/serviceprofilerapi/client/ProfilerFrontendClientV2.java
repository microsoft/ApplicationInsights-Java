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

package com.microsoft.applicationinsights.serviceprofilerapi.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.UUID;

import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.TimestampContract;
import com.squareup.moshi.Moshi.Builder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for interacting with the Service Profiler API endpoint
 */
public class ProfilerFrontendClientV2 implements ServiceProfilerClientV2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerFrontendClientV2.class);

    private static final String PROFILER_API_PREFIX = "api/profileragent/v4";

    private static final String INSTRUMENTATION_KEY_PARAMETER = "iKey";

    private static final String SETTINGS_PATH = PROFILER_API_PREFIX + "/settings";
    public static final String OLD_TIMESTAMP_PARAMETER = "oldTimestamp";
    public static final String FEATURE_VERSION_PARAMETER = "featureVersion";
    public static final String FEATURE_VERSION = "1.0.0";
    public static final String API_FEATURE_VERSION = "2020-10-14-preview";

    private final String hostUrl;
    private final String instrumentationKey;
    private final CloseableHttpClient httpClient;

    private boolean closed;

    public ProfilerFrontendClientV2(String hostUrl, String instrumentationKey) {
        this(hostUrl, instrumentationKey, HttpClientBuilder
                .create()
                .build());
    }

    public ProfilerFrontendClientV2(String hostUrl, String instrumentationKey, CloseableHttpClient httpClient) {
        this.hostUrl = hostUrl;
        this.instrumentationKey = instrumentationKey;
        this.httpClient = httpClient;
        closed = false;
    }

    /**
     * Obtain permission to upload a profile to service profiler
     */
    @Override
    public BlobAccessPass getUploadAccess(UUID profileId)
            throws URISyntaxException, IOException, ClientClosedException {
        assertNotClosed();

        URI requestUri = uploadRequestUri(profileId);
        LOGGER.debug("Etl upload access request: {}", requestUri.toString());

        return this.executePostWithRedirect(
                requestUri,
                response -> {
                    StatusLine statusLine = response.getStatusLine();
                    HttpEntity entity = response.getEntity();

                    if (statusLine.getStatusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                        throw new HttpResponseException(
                                statusLine.getStatusCode(), statusLine.getReasonPhrase());
                    }

                    if (entity != null) {
                        Header[] locations = response.getHeaders("Location");
                        if (locations != null && locations.length > 0) {
                            return new BlobAccessPass(null, locations[0].getValue(), null);
                        } else {
                            return null;
                        }
                    }

                    return null;
                }, 0);
    }

    /**
     * Unfortunately need to implement POST redirect logic. I believe this is due to our httpclient suffering from:
     * https://issues.apache.org/jira/browse/HTTPCLIENT-1680
     */
    public <T> T executePostWithRedirect(final URI requestUri,
                                         final ResponseHandler<? extends T> responseHandler,
                                         int redirectCount) throws IOException {
        if (redirectCount > 50) {
            throw new RuntimeException("Max redirects");
        }

        HttpPost request = new HttpPost(requestUri);

        return httpClient
                .execute(request, response -> {
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY ||
                            response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY ||
                            response.getStatusLine().getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT) {

                        Header[] locations = response.getHeaders("Location");
                        if (locations != null && locations.length > 0) {
                            String location = locations[0].getValue();
                            try {
                                return executePostWithRedirect(new URI(location), responseHandler, redirectCount + 1);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException("Failed to parse URI", e);
                            }
                        } else {
                            throw new RuntimeException("No Location provided on redirect");
                        }
                    } else {
                        return responseHandler.handleResponse(response);
                    }
                });
    }

    /**
     * Report to Service Profiler that the profile upload has been completed
     */
    @Override
    public ArtifactAcceptedResponse reportUploadFinish(UUID profileId, String etag)
            throws URISyntaxException, UnsupportedCharsetException, ClientClosedException, IOException {

        assertNotClosed();

        URI requestUri = uploadFinishedRequestUri(profileId, etag);

        return executePostWithRedirect(
                requestUri,
                response -> {
                    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED &&
                            response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED
                    ) {
                        LOGGER.error("Trace upload failed: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                        return null;
                    } else {
                        String responseData = EntityUtils.toString(response.getEntity());

                        return new Builder()
                                .build()
                                .adapter(ArtifactAcceptedResponse.class)
                                .fromJson(responseData);
                    }
                }, 0);
    }

    /**
     * Obtain current settings that have been configured within the UI
     */
    @Override public String getSettings(Date oldTimeStamp)
            throws IOException, URISyntaxException, ClientClosedException {
        assertNotClosed();

        URI requestUri = getSettingsPath(oldTimeStamp);
        LOGGER.debug("Settings pull request: {}", requestUri.toString());

        HttpGet request = new HttpGet(requestUri);

        return httpClient.execute(
                request,
                response -> {
                    StatusLine statusLine = response.getStatusLine();
                    HttpEntity entity = response.getEntity();
                    if (statusLine.getStatusCode() >= 300) {
                        throw new HttpResponseException(
                                statusLine.getStatusCode(), statusLine.getReasonPhrase());
                    }
                    if (entity != null) {
                        return EntityUtils.toString(entity);
                    }
                    return null;
                });
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
            httpClient.close();
        }
    }

    private void assertNotClosed() throws ClientClosedException {
        if (closed) {
            throw new ClientClosedException();
        }
    }

    // api/profileragent/v4/settings?ikey=xyz&featureVersion=1.0.0&oldTimestamp=123
    private URI getSettingsPath(Date oldTimeStamp)
            throws URISyntaxException {
        return new URIBuilder(hostUrl)
                .setPath(SETTINGS_PATH)
                .addParameter(INSTRUMENTATION_KEY_PARAMETER, instrumentationKey)
                .addParameter(
                        OLD_TIMESTAMP_PARAMETER, TimestampContract.timestampToString(oldTimeStamp))
                .addParameter(FEATURE_VERSION_PARAMETER, FEATURE_VERSION)
                .build();
    }

    //api/apps/{ikey}/artifactkinds/{artifactKind}/artifacts/{artifactId}?action=gettoken&extension={ext}&api-version=2020-10-14-preview
    private URI uploadRequestUri(UUID profileId) throws URISyntaxException {
        return getRequestBuilder(profileId)
                .addParameter("action", "gettoken")
                .build();
    }


    //api/apps/{ikey}/artifactkinds/{artifactKind}/artifacts/{artifactId}?action=commit&extension={ext}&etag={ETag}&api-version=2020-10-14-preview
    private URI uploadFinishedRequestUri(UUID profileId, String etag) throws URISyntaxException {
        return getRequestBuilder(profileId)
                .addParameter("action", "commit")
                .addParameter("etag", "\"" + etag + "\"")
                .build();
    }

    private URIBuilder getRequestBuilder(UUID profileId) throws URISyntaxException {
        return new URIBuilder(hostUrl)
                .setPath("api/apps/" + instrumentationKey + "/artifactkinds/profile/artifacts/" + profileId.toString())
                .addParameter(INSTRUMENTATION_KEY_PARAMETER, instrumentationKey)
                .addParameter("extension", "jfr")
                .addParameter("api-version", API_FEATURE_VERSION);
    }
}
