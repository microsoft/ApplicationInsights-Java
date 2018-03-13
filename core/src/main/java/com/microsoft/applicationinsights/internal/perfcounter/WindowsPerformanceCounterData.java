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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Created by gupele on 3/30/2015.
 */
public final class WindowsPerformanceCounterData {
    public String displayName;
    public String categoryName;
    public String counterName;
    public String instanceName;

    public WindowsPerformanceCounterData setDisplayName(String displayName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(displayName), "displayName must be non-null and non empty value.");
        this.displayName = displayName;
        return this;
    }

    public WindowsPerformanceCounterData setCategoryName(String categoryName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(categoryName), "categoryName must be non-null and non empty value.");
        this.categoryName = categoryName;
        return this;
    }

    public WindowsPerformanceCounterData setCounterName(String counterName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(counterName), "counterName must be non-null and non empty value.");
        this.counterName = counterName;
        return this;
    }

    /**
     * Sets the instance name, the method will consult the JniPCConnector for the proper instance name.
     * @param instanceName The requested instance name.
     * @return 'this'.
     * @throws Throwable The method might throw an Error if the JniPCConnector is not able to properly connect to the native code.
     */
    public WindowsPerformanceCounterData setInstanceName(String instanceName) throws Throwable {
        String translatedInstanceName;
        try {
            translatedInstanceName = JniPCConnector.translateInstanceName(instanceName);
            this.instanceName = translatedInstanceName;
        } catch (Throwable e) {
            InternalLogger.INSTANCE.error("Failed to translate instance name '%s': '%s'", instanceName, e.toString());
            throw e;
        }
        return this;
    }
}
