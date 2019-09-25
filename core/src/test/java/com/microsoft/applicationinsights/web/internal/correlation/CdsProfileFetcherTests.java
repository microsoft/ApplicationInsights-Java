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

package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.internal.correlation.CdsProfileFetcher.CdsRetryPolicy;
import com.microsoft.applicationinsights.web.internal.correlation.mocks.MockHttpAsyncClientWrapper;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CdsProfileFetcherTests {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private CdsProfileFetcher testFetcher;
    private TelemetryConfiguration config = new TelemetryConfiguration();

    @Before
    public void prepare() {
        CdsRetryPolicy rp = new CdsRetryPolicy();
        rp.setResetPeriodInMinutes(1);
        testFetcher = new CdsProfileFetcher(rp);
    }

    @After
    public void cleanUp() throws IOException {
        testFetcher.close();
        testFetcher = null;
    }

    @Test
    public void testFetchApplicationId() throws Exception {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we might get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        // this is mimic'ed with clientWrapper.setTaskAsPending();
        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        assertEquals("AppId", result.getAppId());
    }

    @Test
    public void testFetchApplicationIdWithTaskCompleteImmediately() throws Exception {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);
        clientWrapper.setTaskAsComplete();

        testFetcher.setHttpClient(clientWrapper.getClient());

        // task is completed right away
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        assertEquals("AppId", result.getAppId());
    }

    @Test
    public void testFetchApplicationIdMultipleIkeys() throws Exception {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we should get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // call for a second ikey, should also return "pending"
        result = testFetcher.fetchApplicationId("ikey2", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        assertEquals("AppId", result.getAppId());

        clientWrapper.setAppId("AppId2");
        result = testFetcher.fetchApplicationId("ikey2", config);
        assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        assertEquals("AppId2", result.getAppId());
    }

    @Test
    public void testFetchApplicationIdFailureWithException() throws Exception {
        exception.expect(ApplicationIdResolutionException.class);
        exception.expectCause(Matchers.<Throwable>instanceOf(ExecutionException.class));
        //setup - mimic timeout from the async http call
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we should get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // instruct mock task to fail
        clientWrapper.setFailureOn(true);
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchApplicationId("ikey", config);
        fail("Should not have reached here. Instead, an exception should have been thrown.");
    }

    @Test
    public void testFetchApplicationIdFailureWithNon200StatusCode() throws Exception {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we might get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        // this is mimic'ed with clientWrapper.setTaskAsPending();
        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // mimic task completion with 404 status code
        clientWrapper.setTaskAsComplete();
        clientWrapper.setStatusCode(404);
        result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.FAILED, result.getStatus());
        assertNull(result.getAppId());
    }

    @Test
    public void testCachePurgeServiceClearsRetryCounters() throws Exception {
        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        clientWrapper.setStatusCode(500);
        result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.FAILED, result.getStatus());

        assertThat(testFetcher.failureCounters.size(), not(0));

        TimeUnit.SECONDS.sleep(75);

        assertEquals("failureCounters map should be empty, but was "+ Arrays.toString(testFetcher.failureCounters.values().toArray()), 0, testFetcher.failureCounters.values().size());
        assertEquals("tasks map should be empty, but was "+Arrays.toString(testFetcher.tasks.values().toArray()), 0, testFetcher.tasks.values().size());

        assertThat(testFetcher.failureCounters.values(), hasSize(0));
        assertThat(testFetcher.tasks.values(), hasSize(0));
    }

    @Test
    public void testCachePurgeServiceClearsTasksCache() throws Exception {
        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchApplicationId("ikey", config);
        assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        assertNull(result.getAppId());

        assertThat(testFetcher.tasks.size(), not(0));

        TimeUnit.SECONDS.sleep(75);

        assertThat(testFetcher.failureCounters.values(), hasSize(0));
        assertThat(testFetcher.tasks.values(), hasSize(0));
    }

}
