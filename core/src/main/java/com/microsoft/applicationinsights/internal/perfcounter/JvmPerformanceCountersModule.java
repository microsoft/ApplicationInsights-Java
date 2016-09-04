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

import java.util.HashMap;
import java.util.HashSet;

import com.microsoft.applicationinsights.internal.annotation.PerformanceModule;
import com.microsoft.applicationinsights.internal.config.JvmXmlElement;
import com.microsoft.applicationinsights.internal.config.PerformanceCounterJvmSectionXmlElement;
import com.microsoft.applicationinsights.internal.config.PerformanceCountersXmlElement;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.DeadLockDetectorPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.GCPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.JvmHeapMemoryUsedPerformanceCounter;

/**
 * The class loads the relevant Jvm PCs
 *
 * By default the class will create all the JVM PC
 *
 * The class will not be activated if the enclosing XML tag will have the 'UseBuiltIn' tag set to false:
 *
 * <pre>
 * {@code
 *     <PerformanceCounters>
 *         <UseBuiltIn>false</UseBuiltIn>
 *     </PerformanceCounters>
 * }
 * </pre>
 *
 * All Jvm PCs can be disabled like this, without disabling the other built in performance counters:
 *
 * <pre>
 * {@code
 *     <PerformanceCounters>
 *
 *         <Jvm enabled="false"/>
 *
 *     </PerformanceCounters>
 * }
 * </pre>
 *
 * A specific Jvm counter can be disabled like this:
 *
 * <pre>
 * {@code
 *     <PerformanceCounters>
 *         <Jvm>
 *             <JvmPC name="ThreadDeadLockDetector" enabled="false"/>
 *         </Jvm>
 *     </PerformanceCounters>
 * }
 * </pre>
 *
 * Created by gupele on 8/7/2016.
 */
@PerformanceModule("BuiltIn")
public final class JvmPerformanceCountersModule extends AbstractPerformanceCounterModule implements PerformanceCounterConfigurationAware {

    private String[] JvmPCNames = {
            DeadLockDetectorPerformanceCounter.NAME,
            JvmHeapMemoryUsedPerformanceCounter.NAME,
            GCPerformanceCounter.NAME
    };

    public JvmPerformanceCountersModule() throws Exception {
        this(new JvmPerformanceCountersFactory());
    }

    public JvmPerformanceCountersModule(PerformanceCountersFactory factory) throws Exception {
        super(factory);

        if (!(factory instanceof JvmPerformanceCountersFactory)) {
            throw new Exception("Factory must implement windows capabilities.");
        }
    }

    @Override
    public void addConfigurationData(PerformanceCountersXmlElement configuration) {
        JvmPerformanceCountersFactory f = (JvmPerformanceCountersFactory) factory;
        PerformanceCounterJvmSectionXmlElement jvmSection = configuration.getJvmSection();
        if (jvmSection == null) {
            return;
        }

        if (!jvmSection.isEnabled()) {
            f.setIsEnabled(false);
            return;
        }

        HashMap<String, JvmXmlElement> jvmPcsMap = jvmSection.getJvmXmlElementsMap();
        HashSet<String> disabledJvmPCs = new HashSet<String>();

        for (String jvmPcName : JvmPCNames) {
            JvmXmlElement pc = jvmPcsMap.get(jvmPcName);
            if (pc != null && !pc.isEnabled()) {
                disabledJvmPCs.add(jvmPcName);
            }
        }
        f.setDisabledJvmPCs(disabledJvmPCs);
    }
}
