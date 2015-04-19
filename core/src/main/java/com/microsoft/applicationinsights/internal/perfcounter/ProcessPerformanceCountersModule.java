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

import java.util.ArrayList;

import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import com.microsoft.applicationinsights.internal.config.PerformanceCountersXmlElement;
import com.microsoft.applicationinsights.internal.config.WindowsPerformanceCounterXmlElement;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;

/**
 * The class will be used when the SDK needs to add the 'built-in' performance counters.
 *
 * Created by gupele on 3/3/2015.
 */
@PerformanceModule("BuiltIn")
public final class ProcessPerformanceCountersModule extends AbstractPerformanceCounterModule implements PerformanceCounterConfigurationAware {
    public ProcessPerformanceCountersModule() throws Exception {
        this(new ProcessBuiltInPerformanceCountersFactory());
    }

    public ProcessPerformanceCountersModule(PerformanceCountersFactory factory) throws Exception {
        super(factory);

        if (!(factory instanceof WindowsPerformanceCountersFactory)) {
            throw new Exception("Factory must implement windows capabilities.");
        }
    }

    /**
     * This method checks for Windows performance counters from the configuration.
     * The method will work only if the process is activated under Windows OS. The method will initialize
     * the connection to the native code using {@link com.microsoft.applicationinsights.internal.perfcounter.JniPCConnector}
     * and then will go through the requested performance counters, normalize them, and will hand them to the factory.
     * @param configuration
     */
    @Override
    public void addConfigurationData(PerformanceCountersXmlElement configuration) {
        if (!SystemInformation.INSTANCE.isWindows()) {
            InternalLogger.INSTANCE.trace("Windows performance counters are not relevant on this OS.");
            return;
        }

        if (!JniPCConnector.initialize()) {
            InternalLogger.INSTANCE.error("Failed to initialize JNI connection.");
            return;
        }

        ArrayList<WindowsPerformanceCounterXmlElement> windowsPCs = configuration.getWindowsPCs();

        if (windowsPCs == null || windowsPCs.isEmpty()) {
            return;
        }

        ArrayList<WindowsPerformanceCounterData> configurationRequests = new ArrayList<WindowsPerformanceCounterData>();
        for (WindowsPerformanceCounterXmlElement element : windowsPCs) {
            try {
                WindowsPerformanceCounterData data =
                        new WindowsPerformanceCounterData().
                                setDisplayName(element.getDisplayName()).
                                setCategoryName(element.getCategoryName()).
                                setCounterName(element.getCounterName()).
                                setInstanceName(element.getInstanceName());

                configurationRequests.add(data);
            } catch (Throwable e) {
                InternalLogger.INSTANCE.error("Failed to initialize Windows performance counter '%s'.", e.getMessage());
            }
        }

        if (configurationRequests.isEmpty()) {
            InternalLogger.INSTANCE.error("Failed to initialize Windows performance counters: All requested performance counters are not valid.");
        } else {
            ((WindowsPerformanceCountersFactory)factory).setWindowsPCs(configurationRequests);
        }
    }
}
