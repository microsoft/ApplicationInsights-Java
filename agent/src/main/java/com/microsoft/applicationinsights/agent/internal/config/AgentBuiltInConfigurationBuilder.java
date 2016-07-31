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

/**
 * Created by gupele on 6/5/2015.
 */
public class AgentBuiltInConfigurationBuilder {
    private boolean enabled = true;
    private boolean runtimeExceptionDetectionEnabled = true;
    private boolean httpEnabled = true;
    private boolean jdbcEnabled = true;
    private boolean hibernateEnabled = true;
    private boolean jedisEnabled = true;
    private boolean jmxEnabled = true;
    private long jedisThresholdInMS = 10000L;
    private Long maxSqlQueryLimitInMS = 10000L;

    public AgentBuiltInConfiguration create() {
        return new AgentBuiltInConfiguration(enabled,
                                             runtimeExceptionDetectionEnabled && enabled,
                                             httpEnabled && enabled,
                                             jdbcEnabled && enabled,
                                             hibernateEnabled && enabled,
                                             hibernateEnabled && jedisEnabled,
                                             enabled && jmxEnabled,
                                             maxSqlQueryLimitInMS,
                                             jedisThresholdInMS);
    }

    public AgentBuiltInConfigurationBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setRuntimeExceptionDetectionEnabled(boolean runtimeExceptionDetectionEnabled) {
        this.runtimeExceptionDetectionEnabled = runtimeExceptionDetectionEnabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setJdbcEnabled(boolean jdbcEnabled) {
        this.jdbcEnabled = jdbcEnabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setHibernateEnabled(boolean hibernateEnabled) {
        this.hibernateEnabled = hibernateEnabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setSqlMaxQueryLimitInMS(Long maxSqlQueryLimitInMS) {
        if (maxSqlQueryLimitInMS == null) {
            this.maxSqlQueryLimitInMS = 10000L;
        } else {
            this.maxSqlQueryLimitInMS = maxSqlQueryLimitInMS;
        }
        return this;
    }

    public AgentBuiltInConfigurationBuilder setJedisValues(boolean jedisEnabled, long jedisThresholdInMS) {
        this.jedisEnabled = jedisEnabled;
        this.jedisThresholdInMS = jedisThresholdInMS < 0 ? 0 : jedisThresholdInMS;
        return this;
    }
}
