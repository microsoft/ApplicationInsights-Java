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
    private boolean httpEnabled = true;
    private boolean jdbcEnabled = true;
    private boolean hibernateEnabled = true;
    private Long maxSqlQueryLimit = Long.MAX_VALUE;

    public AgentBuiltInConfiguration create() {
        System.out.println("maxSqlQueryLimit" + maxSqlQueryLimit);
        return new AgentBuiltInConfiguration(enabled, httpEnabled && enabled, jdbcEnabled && enabled, hibernateEnabled && enabled, maxSqlQueryLimit);
    }

    public AgentBuiltInConfigurationBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AgentBuiltInConfigurationBuilder setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
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

    public AgentBuiltInConfigurationBuilder setSqlMaxQueryLimit(Long maxSqlQueryLimit) {
        System.out.println(maxSqlQueryLimit);
        if (maxSqlQueryLimit == null) {
            this.maxSqlQueryLimit = Long.MAX_VALUE;
        } else {
            this.maxSqlQueryLimit = maxSqlQueryLimit;
        }
        return this;
    }
}
