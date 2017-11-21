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

package com.microsoft.applicationinsights.web.internal.correlation.mocks;

import com.microsoft.applicationinsights.web.internal.correlation.AppProfileFetcher;
import com.microsoft.applicationinsights.web.internal.correlation.ProfileFetcherResultTaskStatus;
import com.microsoft.applicationinsights.web.internal.correlation.ProfileFetcherResult;

public class MockProfileFetcher implements AppProfileFetcher {
    
    private String appId;
    private int callCounter = 0;
    private ProfileFetcherResultTaskStatus status = ProfileFetcherResultTaskStatus.COMPLETE;

	@Override
	public ProfileFetcherResult fetchAppProfile(String instrumentationKey) {
        ++callCounter;
        return new ProfileFetcherResult(this.appId, this.status);
	}

	public void setAppIdToReturn(String appId) {
        this.appId = appId;
    }
    
    public int callCount() {
        return this.callCounter;
    }

    public void setResultStatus(ProfileFetcherResultTaskStatus status) {
        this.status = status;
    }
}