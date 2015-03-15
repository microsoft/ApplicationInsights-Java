package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.internal.system.SystemInformation;

/**
 * Created by gupele on 3/12/2015.
 */
abstract class AbstractPerformanceCounterBase implements PerformanceCounter {
    private final static String PROCESS_CATEGORY_FMT = "Process(%s)";

    protected static String getProcessCategoryName() {
        return String.format(PROCESS_CATEGORY_FMT, SystemInformation.INSTANCE.getProcessId());
    }
}

