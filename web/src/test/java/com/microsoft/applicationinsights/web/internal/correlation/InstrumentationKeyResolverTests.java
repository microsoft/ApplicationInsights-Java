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

import com.microsoft.applicationinsights.web.internal.correlation.mocks.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InstrumentationKeyResolverTests {
    
    @Before
    public void testInitialize() {
       InstrumentationKeyResolver.INSTANCE.clearCache();
    }

    @Test
    public void testResolveInstrumentationKey() {

        //setup
        MockProfileFetcher mockFetcher = new MockProfileFetcher();
        mockFetcher.setAppIdToReturn("appId");
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockFetcher);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");

        //validate
        Assert.assertNotNull(appId);
        Assert.assertEquals("appId", appId);
        Assert.assertEquals(1, mockFetcher.callCount());
    }

    @Test
    public void testResolveInstrumentationKeyWithPendingStatus() {

        //setup
        MockProfileFetcher mockFetcher = new MockProfileFetcher();
        mockFetcher.setAppIdToReturn("appId");
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.PENDING);
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockFetcher);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");

        //validate
        Assert.assertNull(appId);
        Assert.assertEquals(1, mockFetcher.callCount());

        //mimic calling resolver again after some time
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        Assert.assertEquals("appId", appId);
        //fetcher will be called again since the task was pending (i.e. not yet in the cache)
        Assert.assertEquals(2, mockFetcher.callCount());
    }

    @Test
    public void testResolveInstrumentationKeyWithFailedStatus() {

        //setup
        MockProfileFetcher mockFetcher = new MockProfileFetcher();
        mockFetcher.setAppIdToReturn("appId");
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.FAILED);
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockFetcher);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");

        //validate
        Assert.assertNull(appId);
        Assert.assertEquals(1, mockFetcher.callCount());

        //mimic calling resolver again after some time
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        Assert.assertEquals("appId", appId);
        //fetcher will be called again since the previous attempt failed
        Assert.assertEquals(2, mockFetcher.callCount());
    }
    
    @Test
    public void testResolveInstrumentationKeyWhenExceptionThrown() {

        //setup
        MockProfileFetcher mockFetcher = new MockProfileFetcher();
        mockFetcher.setAppIdToReturn("appId");
        mockFetcher.setExceptionOn(true);
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockFetcher);

        //run
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");

        //validate no exception is thrown back to the caller and appId is null
        Assert.assertNull(appId);
        Assert.assertEquals(1, mockFetcher.callCount());

        //mimic calling resolver again after some time
        mockFetcher.setExceptionOn(false);
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.PENDING);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        //result is still null since fetcher returns a task in pending state
        Assert.assertNull(appId);
        //fetcher will be called again since the previous attempt failed
        Assert.assertEquals(2, mockFetcher.callCount());
        
        //mimic final call which returns the completed task
        mockFetcher.setResultStatus(ProfileFetcherResultTaskStatus.COMPLETE);
        appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        Assert.assertEquals("appId", appId);
        Assert.assertEquals(3, mockFetcher.callCount());
    }

    @Test
    public void testIkeyResolvedFromCache() {

        //setup
        MockProfileFetcher mockFetcher = new MockProfileFetcher();
        mockFetcher.setAppIdToReturn("appId");
        InstrumentationKeyResolver.INSTANCE.setProfileFetcher(mockFetcher);

        //run
        Assert.assertEquals(0, mockFetcher.callCount());
        InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        Assert.assertEquals(1, mockFetcher.callCount());

        //resolving the same ikey should not generate new call to fetcher
        InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey");
        Assert.assertEquals(1, mockFetcher.callCount());

        //resolving another ikey increases call count
        InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey("ikey2");
        Assert.assertEquals(2, mockFetcher.callCount());
    }
}