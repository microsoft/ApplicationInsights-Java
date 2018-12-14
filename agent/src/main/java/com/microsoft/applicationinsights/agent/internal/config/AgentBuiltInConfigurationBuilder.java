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

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.List;

/**
 * Created by gupele on 6/5/2015.
 */
public class AgentBuiltInConfigurationBuilder {
    private boolean enabled = false;
    private boolean httpEnabled = false;
    private boolean jdbcEnabled = false;
    private boolean hibernateEnabled = false;
    private boolean jedisEnabled = false;
    private boolean jmxEnabled = false;
    private boolean w3cEnabled = false;
    private boolean isW3CBackportEnabled = true;
    private long jedisThresholdInMS = 10000L;
    private Long maxSqlQueryLimitInMS = 10000L;
    private DataOfConfigurationForException dataOfConfigurationForException = new DataOfConfigurationForException();
    private List<ClassInstrumentationData> simpleBuiltInClasses;

    public AgentBuiltInConfiguration create() {
        if (!enabled) {
            this.dataOfConfigurationForException.setEnabled(false);
        }

        InternalLogger.INSTANCE.trace(String.format("Outbound W3C tracing is enabled : %s", w3cEnabled));
        InternalLogger.INSTANCE.trace(String.format("Outbound W3C backport mode is enabled : %s", isW3CBackportEnabled));

        return new AgentBuiltInConfiguration(enabled,
                                             simpleBuiltInClasses,
                                             httpEnabled && enabled,
                                             w3cEnabled && enabled,
                                             isW3CBackportEnabled && enabled,
                                             jdbcEnabled && enabled,
                                             hibernateEnabled && enabled,
                                             jedisEnabled && enabled,
                                             enabled && jmxEnabled,
                                             maxSqlQueryLimitInMS,
                                             jedisThresholdInMS,
                                             dataOfConfigurationForException);
    }

    public AgentBuiltInConfigurationBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setHttpEnabled(boolean httpEnabled, boolean w3cEnabled,
        boolean isW3CBackportEnabled) {
        this.httpEnabled = httpEnabled;
        this.w3cEnabled = w3cEnabled;
        this.isW3CBackportEnabled = isW3CBackportEnabled;
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

    public void setDataOfConfigurationForException(DataOfConfigurationForException dataOfConfigurationForException) {
        if (dataOfConfigurationForException != null) {
            this.dataOfConfigurationForException = dataOfConfigurationForException;
        }
    }

    public void setSimpleBuiltInClasses(List<ClassInstrumentationData> simpleBuiltInClasses) {
        this.simpleBuiltInClasses = simpleBuiltInClasses;
    }
}
