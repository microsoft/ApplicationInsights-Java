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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuiltInInstrumentationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BuiltInInstrumentationBuilder.class);

    private boolean enabled;

    private boolean httpEnabled;
    private boolean w3cEnabled;
    private boolean w3cBackCompatEnabled = true;

    private boolean jdbcEnabled;

    private boolean loggingEnabled;

    private boolean jedisEnabled;

    private long queryPlanThresholdInMS = 10000;

    public BuiltInInstrumentation create() {

        logger.trace("Outbound W3C tracing is enabled: {}", w3cEnabled);
        logger.trace("Outbound W3C backport mode is enabled: {}", w3cBackCompatEnabled);

        return new BuiltInInstrumentation(enabled,
                httpEnabled && enabled,
                w3cEnabled && enabled,
                w3cBackCompatEnabled && enabled,
                jdbcEnabled && enabled,
                loggingEnabled && enabled,
                jedisEnabled && enabled,
                queryPlanThresholdInMS
        );
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setHttpEnabled(boolean httpEnabled, boolean w3cEnabled, boolean w3cBackCompatEnabled) {
        this.httpEnabled = httpEnabled;
        this.w3cEnabled = w3cEnabled;
        this.w3cBackCompatEnabled = w3cBackCompatEnabled;
    }

    public void setJdbcEnabled(boolean jdbcEnabled) {
        this.jdbcEnabled = jdbcEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    public void setJedisEnabled(boolean jedisEnabled) {
        this.jedisEnabled = jedisEnabled;
    }

    public void setQueryPlanThresholdInMS(Long queryPlanThresholdInMS) {
        if (queryPlanThresholdInMS == null) {
            this.queryPlanThresholdInMS = 10000;
        } else {
            this.queryPlanThresholdInMS = queryPlanThresholdInMS;
        }
    }
}
