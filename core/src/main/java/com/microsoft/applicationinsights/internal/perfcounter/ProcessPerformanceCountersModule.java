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

import com.microsoft.applicationinsights.internal.config.PerformanceCountersXmlElement;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class will be used when the SDK needs to add the 'built-in' performance counters.
 *
 * Created by gupele on 3/3/2015.
 */
public final class ProcessPerformanceCountersModule extends AbstractPerformanceCounterModule implements PerformanceCounterConfigurationAware {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPerformanceCountersModule.class);

    public ProcessPerformanceCountersModule() throws Exception {
        this(new ProcessBuiltInPerformanceCountersFactory());
    }

    public ProcessPerformanceCountersModule(PerformanceCountersFactory factory) {
        super(factory);
    }

    /**
     * This method checks for Windows performance counters from the configuration.
     * The method will work only if the process is activated under Windows OS. The method will initialize
     * the connection to the native code using {@link com.microsoft.applicationinsights.internal.perfcounter.JniPCConnector}
     * and then will go through the requested performance counters, normalize them, and will hand them to the factory.
     * @param configuration The configuration of that section
     */
    @Override
    public void addConfigurationData(PerformanceCountersXmlElement configuration) {
        if (!SystemInformation.INSTANCE.isWindows()) {
            logger.trace("Windows performance counters are not relevant on this OS.");
            return;
        }

        if (!JniPCConnector.initialize()) {
            logger.error("Failed to initialize JNI connection.");
            return;
        }
    }
}
