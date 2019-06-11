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

package com.microsoft.applicationinsights.agent.internal.config;

public class BuiltInInstrumentation {

    private final boolean enabled;

    private final boolean httpEnabled;
    private final boolean w3cEnabled;
    private final boolean w3cBackCompatEnabled;

    private final boolean jdbcEnabled;

    private final boolean loggingEnabled;

    private final boolean jedisEnabled;

    private final long queryPlanThresholdInMS;

    public BuiltInInstrumentation(boolean enabled,
                                  boolean httpEnabled,
                                  boolean w3cEnabled,
                                  boolean w3cBackCompatEnabled,
                                  boolean jdbcEnabled,
                                  boolean loggingEnabled,
                                  boolean jedisEnabled,
                                  long queryPlanThresholdInMS) {
        this.enabled = enabled;
        this.httpEnabled = httpEnabled;
        this.w3cEnabled = w3cEnabled;
        this.w3cBackCompatEnabled = w3cBackCompatEnabled;
        this.jdbcEnabled = jdbcEnabled;
        this.loggingEnabled = loggingEnabled;
        this.jedisEnabled = jedisEnabled;
        this.queryPlanThresholdInMS = queryPlanThresholdInMS;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isW3cEnabled() {
        return w3cEnabled;
    }

    public boolean isW3cBackCompatEnabled() {
        return w3cBackCompatEnabled;
    }

    public boolean isJdbcEnabled() {
        return jdbcEnabled;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public boolean isJedisEnabled() {
        return jedisEnabled;
    }

    public long getQueryPlanThresholdInMS() {
        return queryPlanThresholdInMS;
    }
}
