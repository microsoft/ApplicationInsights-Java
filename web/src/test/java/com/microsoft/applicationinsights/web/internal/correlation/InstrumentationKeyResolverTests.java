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
import org.junit.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class InstrumentationKeyResolverTests {

    private ApplicationIdResolver mockResolver;
    private TelemetryConfiguration configuration = new TelemetryConfiguration();
    private String testAppId = "appId";

    @Before
    public void testInitialize() throws Exception {
        mockResolver = mock(ApplicationIdResolver.class);
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.COMPLETE);

        InstrumentationKeyResolver.INSTANCE.setAppIdResolver(mockResolver);
        InstrumentationKeyResolver.INSTANCE.clearCache();
    }

    private void mockResolverShouldReturnThisStatus(final ProfileFetcherResultTaskStatus status) throws Exception {
        when(mockResolver.fetchApplicationId(anyString(), eq(configuration))).thenAnswer(new Answer<ProfileFetcherResult>() {
            @Override
            public ProfileFetcherResult answer(InvocationOnMock invocation) throws Throwable {
                return new ProfileFetcherResult(testAppId, status);
            }
        });
    }

    @After
    public void tearDown() {
        InstrumentationKeyResolver.INSTANCE.setAppIdResolver(new CdsProfileFetcher());
        mockResolver = null;
        testAppId = "appId";
    }

    @Test
    public void testResolveInstrumentationKey() throws Exception {
        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);

        //validate
        assertEquals("cid-v1:appId", appId);
        verify(mockResolver, times(1)).fetchApplicationId(eq("ikey"), any(TelemetryConfiguration.class));
    }

    @Test
    public void testResolveInstrumentationKeyWithPendingStatus() throws Exception {

        //setup
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.PENDING);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);

        //validate
        assertNull(appId);
        verify(mockResolver, times(1)).fetchApplicationId(eq("ikey"), eq(configuration));

        //mimic calling resolver again after some time
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);
        assertEquals("cid-v1:appId", appId);
        //fetcher will be called again since the task was pending (i.e. not yet in the cache)
        verify(mockResolver, times(2)).fetchApplicationId(eq("ikey"), eq(configuration));
    }

    @Test
    public void testResolveInstrumentationKeyWithFailedStatus() throws Exception {

        //setup
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.FAILED);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);

        //validate
        assertNull(appId);
        verify(mockResolver, times(1)).fetchApplicationId(eq("ikey"), eq(configuration));

        //mimic calling resolver again after some time
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);
        assertEquals("cid-v1:appId", appId);
        //fetcher will be called again since the previous attempt failed
        verify(mockResolver, times(2)).fetchApplicationId(eq("ikey"), eq(configuration));
    }

    @Test
    public void testResolveInstrumentationKeyWhenExceptionThrown() throws Exception {
        reset(mockResolver);
        //setup
        when(mockResolver.fetchApplicationId(anyString(), any(TelemetryConfiguration.class))).thenAnswer(new Answer<ProfileFetcherResult>() {
            private int count = 0;
            @Override
            public ProfileFetcherResult answer(InvocationOnMock invocation) throws Throwable {
                switch(count++) {
                    case 0:
                        throw new ApplicationIdResolutionException("Could not resolve id");
                    case 1:
                        return new ProfileFetcherResult(null, ProfileFetcherResultTaskStatus.PENDING);
                    default:
                        return new ProfileFetcherResult(testAppId, ProfileFetcherResultTaskStatus.COMPLETE);
                }
            }
        });

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);

        //validate no exception is thrown back to the caller and appId is null
        assertNull(appId);
        verify(mockResolver, times(1)).fetchApplicationId(eq("ikey"), eq(configuration));

        //mimic calling resolver again after some time
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.PENDING);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);
        //result is still null since fetcher returns a task in pending state
        assertNull(appId);
        //fetcher will be called again since the previous attempt failed
        verify(mockResolver, times(2)).fetchApplicationId(eq("ikey"), eq(configuration));

        //mimic final call which returns the completed task
        mockResolverShouldReturnThisStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration);
        assertEquals("cid-v1:appId", appId);
        verify(mockResolver, times(3)).fetchApplicationId(eq("ikey"), eq(configuration));
    }

    @Test
    public void testIkeyResolvedFromCache() throws Exception {
        //run
        assertNotNull(InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration));
        verify(mockResolver, times(1)).fetchApplicationId(anyString(), eq(configuration));

        //resolving the same ikey should not generate new call to fetcher
        assertNotNull(InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey", configuration));
        verify(mockResolver, times(1)).fetchApplicationId(anyString(), eq(configuration));

        //resolving another ikey increases call count
        assertNotNull(InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey2", configuration));
        verify(mockResolver, times(2)).fetchApplicationId(anyString(), eq(configuration));
    }
}