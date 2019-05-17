/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.agent.internal.agent.jmx;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;

/**
 * Created by gupele on 8/6/2015.
 */
public class JmxConnectorMXBeanImpl implements JmxConnectorMXBean {
    @Override
    public long getQueryPlanThresholdInMS() {
        return ImplementationsCoordinator.INSTANCE.getQueryPlanThresholdInMS();
    }

    @Override
    public void setQueryPlanThresholdInMS(long thresholdInMS) {
        ImplementationsCoordinator.INSTANCE.setQueryPlanThresholdInMS(thresholdInMS);
    }

    @Override
    public long getRedisThresholdInMS() {
        return ImplementationsCoordinator.INSTANCE.getRedisThresholdInNS() / 1000000;
    }

    @Override
    public void setRedisThresholdInMS(long thresholdInMS) {
        ImplementationsCoordinator.INSTANCE.setRedisThresholdInMS(thresholdInMS);
    }
}
