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
package com.microsoft.applicationinsights.alerting.config;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;

public class CollectionPlanConfigurationBuilder {
    private boolean single = false;
    private EngineMode mode;
    private ZonedDateTime expiration;
    private long immediateProfilingDuration;
    private String settingsMoniker;

    public CollectionPlanConfiguration createDefaultConfiguration() {
        return new CollectionPlanConfiguration(single, mode, expiration, immediateProfilingDuration, settingsMoniker);
    }

    public CollectionPlanConfigurationBuilder setCollectionPlanSingle(boolean single) {
        this.single = single;
        return this;
    }

    public CollectionPlanConfigurationBuilder setMode(EngineMode mode) {
        this.mode = mode;
        return this;
    }

    public CollectionPlanConfigurationBuilder setExpiration(long expiration) {
        this.expiration = parseBinaryDate(expiration);
        return this;
    }

    public static ZonedDateTime parseBinaryDate(long expiration) {
        long ticks = expiration & 0x3fffffffffffffffL;
        long seconds = ticks / 10000000L;
        long nanos = (ticks % 10000000L) * 100L;
        long offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
        return Instant.ofEpochSecond(seconds + offset, nanos).atZone(ZoneOffset.UTC);
    }

    public CollectionPlanConfigurationBuilder setImmediateProfilingDuration(long immediateProfilingDuration) {
        this.immediateProfilingDuration = immediateProfilingDuration;
        return this;
    }

    public CollectionPlanConfigurationBuilder setSettingsMoniker(String settingsMoniker) {
        this.settingsMoniker = settingsMoniker;
        return this;
    }
}