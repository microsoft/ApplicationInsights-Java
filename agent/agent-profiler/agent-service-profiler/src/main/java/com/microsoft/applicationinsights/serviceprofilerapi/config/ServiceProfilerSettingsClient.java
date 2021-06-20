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

package com.microsoft.applicationinsights.serviceprofilerapi.config;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import com.azure.core.exception.HttpResponseException;
import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import io.reactivex.Maybe;

/**
 * Client that pulls setting from the service profiler endpoint and emits them if changed
 */
public class ServiceProfilerSettingsClient {
    private final ServiceProfilerClientV2 serviceProfilerClient;
    private Date lastModified;

    public ServiceProfilerSettingsClient(ServiceProfilerClientV2 serviceProfilerClient) {
        this.serviceProfilerClient = serviceProfilerClient;
        lastModified = new Date(1970, Calendar.JANUARY, 1);
    }

    /**
     * Pulls the latest settings. If they have not been modified empty is returned.
     *
     * @return
     */
    public Maybe<ProfilerConfiguration> pullSettings() {
        try {
            String config = serviceProfilerClient.getSettings(lastModified);
            ProfilerConfiguration serviceProfilerConfiguration = toServiceProfilerConfiguration(config);
            if (serviceProfilerConfiguration != null && !serviceProfilerConfiguration.getLastModified().equals(lastModified)) {
                lastModified = serviceProfilerConfiguration.getLastModified();
                return Maybe.just(serviceProfilerConfiguration);
            }
            return Maybe.empty();
        } catch (HttpResponseException e) {
            if (e.getResponse().getStatusCode() == 304) {
                return Maybe.empty();
            } else {
                return Maybe.error(e);
            }
        } catch (Exception e) {
            return Maybe.error(e);
        }
    }

    private ProfilerConfiguration toServiceProfilerConfiguration(String config) throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(Date.class, new Rfc3339DateJsonAdapter())
                .build();
        JsonAdapter<ProfilerConfiguration> jsonAdapter = moshi.adapter(ProfilerConfiguration.class);

        return jsonAdapter.fromJson(config);
    }
}
