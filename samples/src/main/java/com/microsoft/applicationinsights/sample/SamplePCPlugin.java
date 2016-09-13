package com.microsoft.applicationinsights.sample;

import com.microsoft.applicationinsights.extensibility.PerformanceCountersCollectionPlugin;

/**
 * A sample for creating a concrete plugin that will be called before and after Performance Counters are collected.
 *
 * To activate this class you need to add it to the ApplicationInsights.xml configuration file with the 'Plugin' tag:
 *
 * <pre>
 * {@code
 *
 * 		<PerformanceCounters>
 * 		    <Plugin>full.path.to.your.Plugin</Plugin>
 *      </PerformanceCounters>
 * }
 * </pre>
 *
 * Created by gupele on 9/13/2016.
 */
public class SamplePCPlugin implements PerformanceCountersCollectionPlugin {
    @Override
    public void preCollection() {
        System.out.println("pre collection called");
    }

    @Override
    public void postCollection() {
        System.out.println("post collection called");
    }
}
