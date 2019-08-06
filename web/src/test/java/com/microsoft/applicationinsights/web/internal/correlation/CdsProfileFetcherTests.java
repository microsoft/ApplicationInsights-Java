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

import com.microsoft.applicationinsights.web.internal.correlation.CdsProfileFetcher.CdsRetryPolicy;
import com.microsoft.applicationinsights.web.internal.correlation.mocks.MockHttpAsyncClientWrapper;
import org.apache.http.ParseException;
import org.junit.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CdsProfileFetcherTests {

    private CdsProfileFetcher testFetcher;

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
    public void testFetchApplicationId() throws InterruptedException, ExecutionException, ParseException, IOException {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we might get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        // this is mimic'ed with clientWrapper.setTaskAsPending();
        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        Assert.assertEquals("AppId", result.getAppId());
    }

    @Test
    public void testFetchApplicationIdWithTaskCompleteImmediately() throws InterruptedException, ExecutionException, ParseException, IOException {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);
        clientWrapper.setTaskAsComplete();

        testFetcher.setHttpClient(clientWrapper.getClient());

        // task is completed right away
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        Assert.assertEquals("AppId", result.getAppId());
    }

    @Test
    public void testFetchApplicationIdMultipleIkeys() throws InterruptedException, ExecutionException, ParseException, IOException {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we should get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // call for a second ikey, should also return "pending"
        result = testFetcher.fetchAppProfile("ikey2");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        Assert.assertEquals("AppId", result.getAppId());

        clientWrapper.setAppId("AppId2");
        result = testFetcher.fetchAppProfile("ikey2");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.COMPLETE, result.getStatus());
        Assert.assertEquals("AppId2", result.getAppId());
    }

    @Test(expected = ExecutionException.class)
    public void testFetchApplicationIdFailureWithException() throws InterruptedException, ExecutionException, ParseException, IOException {

        //setup - mimic timeout from the async http call
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we should get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // instruct mock task to fail
        clientWrapper.setFailureOn(true);
        clientWrapper.setTaskAsComplete();
        result = testFetcher.fetchAppProfile("ikey");
        Assert.fail("Should not have reached here. Instead, an exception should have been thrown.");
    }

    @Test
    public void testFetchApplicationIdFailureWithNon200StatusCode() throws InterruptedException, ExecutionException, ParseException, IOException {

        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        // the first time we try to fetch the profile, we might get a "pending" task status
        // since the profile fetcher uses asynchronous calls to retrieve the profile from CDS
        // this is mimic'ed with clientWrapper.setTaskAsPending();
        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // mimic task completion with 404 status code
        clientWrapper.setTaskAsComplete();
        clientWrapper.setStatusCode(404);
        result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.FAILED, result.getStatus());
        Assert.assertNull(result.getAppId());
    }

    @Test
    public void testCachePurgeServiceClearsRetryCounters() throws InterruptedException, ExecutionException, IOException {
        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        // mimic task completion
        clientWrapper.setTaskAsComplete();
        clientWrapper.setStatusCode(500);
        result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.FAILED, result.getStatus());

        assertThat(testFetcher.failureCounters.size(), not(0));

        TimeUnit.SECONDS.sleep(75);

        assertEquals("failureCounters map should be empty, but was "+ Arrays.toString(testFetcher.failureCounters.values().toArray()), 0, testFetcher.failureCounters.values().size());
        assertEquals("tasks map should be empty, but was "+Arrays.toString(testFetcher.tasks.values().toArray()), 0, testFetcher.tasks.values().size());

        assertThat(testFetcher.failureCounters.values(), hasSize(0));
        assertThat(testFetcher.tasks.values(), hasSize(0));
    }

    @Test
    public void testCachePurgeServiceClearsTasksCache() throws InterruptedException, ExecutionException, IOException {
        //setup
        MockHttpAsyncClientWrapper clientWrapper = new MockHttpAsyncClientWrapper();
        clientWrapper.setAppId("AppId");
        clientWrapper.setFailureOn(false);

        testFetcher.setHttpClient(clientWrapper.getClient());

        clientWrapper.setTaskAsPending();
        ProfileFetcherResult result = testFetcher.fetchAppProfile("ikey");
        Assert.assertEquals(ProfileFetcherResultTaskStatus.PENDING, result.getStatus());
        Assert.assertNull(result.getAppId());

        assertThat(testFetcher.tasks.size(), not(0));

        TimeUnit.SECONDS.sleep(75);

        assertThat(testFetcher.failureCounters.values(), hasSize(0));
        assertThat(testFetcher.tasks.values(), hasSize(0));
    }

}
