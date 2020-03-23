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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum InstrumentationKeyResolver {
    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationKeyResolver.class);

    private static final String CorrelationIdFormat = "cid-v1:%s";
    private volatile ApplicationIdResolver appIdResolver;
    private final ConcurrentMap<String, String> appIdCache;

    InstrumentationKeyResolver() {
        this.appIdCache = new ConcurrentHashMap<>();
        this.appIdResolver = new CdsProfileFetcher();
    }

    public void clearCache() {
        this.appIdCache.clear();
    }

    /* @VisisbleForTesting */
    void setAppIdResolver(ApplicationIdResolver appIdResolver) {
        this.appIdResolver = appIdResolver;
    }

    /**
     * @param instrumentationKey The instrumentation key.
     * @return The applicationId associated with the instrumentation key or null if it cannot be retrieved.
     * @deprecated Use {@link #resolveInstrumentationKey(String, TelemetryConfiguration)}
     */
    @Deprecated
    public String resolveInstrumentationKey(String instrumentationKey) {
         return resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());
    }

    public String resolveInstrumentationKey(String instrumentationKey, TelemetryConfiguration config) {
        if (StringUtils.isEmpty(instrumentationKey)) {
            throw new IllegalArgumentException("instrumentationKey must not be null or empty");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null or empty");
        }

        try {
            String appId = this.appIdCache.get(instrumentationKey);

            if (appId != null) {
                return appId;
            }

            ProfileFetcherResult result = this.appIdResolver.fetchApplicationId(instrumentationKey, config);
            appId = processResult(result, instrumentationKey);

            if (appId != null) {
                this.appIdCache.putIfAbsent(instrumentationKey, appId);
            }

            return appId;
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("InstrumentationKeyResolver: failed to resolve instrumentation key: {}", config.getInstrumentationKey(), e);
            }
        }

        return null;
    }

    private String processResult(ProfileFetcherResult result, String instrumentationKey) {

        String appId = null;

        switch (result.getStatus()) {
            case PENDING:
                logger.trace("InstrumentationKeyResolver - pending resolution of instrumentation key: {}", instrumentationKey);
                break;
            case FAILED:
                logger.error("InstrumentationKeyResolver - failed to resolve instrumentation key: {}", instrumentationKey);
                break;
            case COMPLETE:
                logger.trace("InstrumentationKeyResolver - successfully resolved instrumentation key: {}", instrumentationKey);
                appId = String.format(CorrelationIdFormat, result.getAppId());
                break;
            default:
                logger.error("InstrumentationKeyResolver - unexpected status. Instrumentation key: {}", instrumentationKey);
                break;
        }

        return appId;
    }
}