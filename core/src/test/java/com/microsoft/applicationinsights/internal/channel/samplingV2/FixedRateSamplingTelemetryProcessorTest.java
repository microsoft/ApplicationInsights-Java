package com.microsoft.applicationinsights.internal.channel.samplingV2;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.TestFramework.StubTelemetryChannel;
import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dhaval Doshi 10/31/2017
 * This class performs tests for FixedRateSamplingTelemetryProcessor
 */

public class FixedRateSamplingTelemetryProcessorTest {

    @Test
    public void defaultSamplingRateTest() {
        FixedRateSamplingTelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        Assert.assertEquals(100.0, processor.getSamplingPercentage(), 12);
    }

    @Test
    public void allTelemetrySentWithDefaultSampleRate() {
        int sentCount = 0;
        final int itemsToGenerate = 100;
        TelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        for (int i = 0; i < itemsToGenerate; ++i) {
            if (processor.process(new RequestTelemetry())) {
                ++sentCount;
            }
        }
        Assert.assertEquals(itemsToGenerate, sentCount, 0);
    }

    @Test
    public void telemetryItemHasSamplingPercentageSet() {
        List<Telemetry> telemetryList = new ArrayList<Telemetry>();
        FixedRateSamplingTelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        processor.setSamplingPercentage("20.0");
        telemetryList.add(new RequestTelemetry());
        telemetryList.add(new PageViewTelemetry());
        for (Telemetry t : telemetryList) {
            processor.process(t);
        }
        Assert.assertEquals(20.0, ((SupportSampling)telemetryList.get(0)).getSamplingPercentage(), 0);
    }

    @Test
    public void telemetryItemSamplingWorksWhenSetByUser() {
        FixedRateSamplingTelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        processor.setSamplingPercentage("100.0");
        Telemetry requestTelemetry = new RequestTelemetry();
        ((SupportSampling)requestTelemetry).setSamplingPercentage(0.0);
        int sentCount = 0;
        if (processor.process(requestTelemetry)) {
            ++sentCount;
        }
        Assert.assertEquals(0, sentCount);
    }

    @Test
    public void dependencyTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);

    }

    @Test
    public void eventTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.EventTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.EventTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void exceptionTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.ExceptionTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.ExceptionTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void pageViewTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void requestTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void traceTelemetryIsSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void metricTelemetryIsNotSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.MetricTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void performanceCounterTelemetryIsNotSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void sessionStateTelemetryIsNotSubjectToSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.SessionStateTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, null, 10.0);
    }

    @Test
    public void requestCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        List<String> excludeTypes = new ArrayList<String>() {{add("Request");}};
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void requestCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        List<String> includeTypes = new ArrayList<String>() {{add("Request");}};
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void dependencyCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> excludeTypes = new ArrayList<String>() {{add("Dependency");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void dependencyCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> includeTypes = new ArrayList<String>() {{add("Dependency");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void eventCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.EventTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> excludeTypes = new ArrayList<String>() {{add("Event");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void eventCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.EventTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.EventTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> includeTypes = new ArrayList<String>() {{add("Event");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void exceptionCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.ExceptionTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        List<String> excludeTypes = new ArrayList<String>() {{add("Exception");}};
        telemetryListCollection.add(dependencyTelemetry);
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void exceptionCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.ExceptionTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.ExceptionTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> includeTypes = new ArrayList<String>() {{add("Exception");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void pageViewCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> excludeTypes = new ArrayList<String>() {{add("PageView");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void pageViewCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> includeTypes = new ArrayList<String>() {{add("PageView");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void traceCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> excludeTypes = new ArrayList<String>() {{add("Trace");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryListCollection, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void traceCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<List<Telemetry>> telemetryListCollection = new ArrayList<List<Telemetry>>();
        telemetryListCollection.add(dependencyTelemetry);
        List<String> includeTypes = new ArrayList<String>() {{add("Trace");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 100.0);
        itemsToSend.clear();
        telemetryListCollection.clear();
        telemetryListCollection.add(dependencyTelemetry1);
        testSampling(client, telemetryListCollection, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void multipleItemsCanBeExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        telemetryCollectionList.add(dependencyTelemetry1);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<String> excludeTypes = new ArrayList<String>() {{add("Trace"); add("Request");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryCollectionList, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void multipleItemsCanBeIncludedInSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.TraceTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        telemetryCollectionList.add(dependencyTelemetry1);
        List<String> includeTypes = new ArrayList<String>() {{add("Trace"); add("Request");}};
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryCollectionList, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void includeDoNotOverrideExcludedFromSampling() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.PageViewTelemetry",100);
        List<Telemetry> dependencyTelemetry1 = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        telemetryCollectionList.add(dependencyTelemetry1);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<String> includeTypes = new ArrayList<String>() {{add("Exception"); add("Request");}};
        List<String> excludeTypes = new ArrayList<String>() {{add("PageView"); add("Request");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryCollectionList, itemsToSend, includeTypes, excludeTypes, 10.0);
    }

    @Test
    public void unknownExcludedTypesAreIgnored() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<String> excludeTypes = new ArrayList<String>() {{add("aaa"); add("bbb");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryCollectionList, itemsToSend, null, excludeTypes, 10.0);
    }

    @Test
    public void unknownIncludedTypesAreIgnored() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<String> includeTypes = new ArrayList<String>() {{add("aaa"); add("bbb");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryCollectionList, itemsToSend, includeTypes, null, 10.0);
    }

    @Test
    public void noSamplingTracksSamplingRate() {
        List<Telemetry> dependencyTelemetry = getListOfTelemetry("com.microsoft.applicationinsights.telemetry.RequestTelemetry",100);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        List<String> includeTypes = new ArrayList<String>() {{add("Request");}};
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testSampling(client, telemetryCollectionList, itemsToSend, includeTypes, null, 100.0);
    }

    @Test
    public void testCorrelatedTelemetryDoesNotGetSampledOut() {
        List<Telemetry> dependencyTelemetry = new ArrayList<Telemetry>();
        Telemetry item = new RequestTelemetry();
        item.getContext().getOperation().setId("abc");
        Telemetry item2 = new RemoteDependencyTelemetry();
        item2.getContext().getOperation().setId("abc");
        dependencyTelemetry.add(item);
        dependencyTelemetry.add(item2);
        List<List<Telemetry>> telemetryCollectionList = new ArrayList<List<Telemetry>>();
        telemetryCollectionList.add(dependencyTelemetry);
        List<Telemetry> itemsToSend = new ArrayList<Telemetry>();
        TelemetryConfiguration configuration = createConfiguration(itemsToSend);
        TelemetryClient client = new TelemetryClient(configuration);
        testNoSampling(client, telemetryCollectionList, itemsToSend, null, null, 50.0);
    }

    private List<Telemetry> getListOfTelemetry(String type, int count) {
        List<Telemetry> telemetryList = new ArrayList<Telemetry>();
        try {
            for (int i = 0; i < count; ++i) {
                if (type.contains("Exception")) {
                    telemetryList.add((Telemetry) Class.forName(type).getConstructor(Throwable.class).newInstance(new Throwable()));
                }
                else {
                    telemetryList.add((Telemetry) Class.forName(type).getConstructor().newInstance());
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return telemetryList;

    }

    private TelemetryConfiguration createConfiguration(final List<Telemetry> itemsToSend) {

        TelemetryConfiguration configuration = new TelemetryConfiguration();
        configuration.setInstrumentationKey("00000000-0000-0000-0000-000000000000");

        class StubBufferTelemetryChannel extends StubTelemetryChannel {
            @Override
            public void send(Telemetry item) {
               itemsToSend.add(item);
               //item.reset();
            }

        }
        configuration.setChannel(new StubBufferTelemetryChannel());
        return configuration;
    }

    private void testSampling(TelemetryClient client , List<List<Telemetry>> dependencyTelemetry,
                              List<Telemetry> itemsToSend, List<String> includeTypes, List<String> excludeTypes, double samplingRate) {


        FixedRateSamplingTelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        processor.setSamplingPercentage(String.valueOf(samplingRate));
        if (includeTypes != null) {
            for (String includeType : includeTypes) {
                if (!StringUtils.isEmpty(includeType)) {
                    processor.addToIncludedType(includeType);
                }
            }
        }

        if (excludeTypes != null) {
            for (String excludeType : excludeTypes) {
                if (!StringUtils.isEmpty(excludeType)) {
                    processor.addToExcludedType(excludeType);
                }
            }
        }


        int generatedCount = 0;
        for (int i = 0; i < dependencyTelemetry.get(0).size(); ++i) {
            for (int j = 0; j < dependencyTelemetry.size(); ++j) {
                ++generatedCount;
                if (processor.process(dependencyTelemetry.get(j).get(i))) {
                    client.track(dependencyTelemetry.get(j).get(i));
                }
            }
        }

        Assert.assertTrue(itemsToSend.get(0) instanceof SupportSampling);
        Assert.assertTrue(itemsToSend.size() > 0);
        Assert.assertEquals((Double)samplingRate, ((SupportSampling) itemsToSend.get(0)).getSamplingPercentage());

        if (Double.compare(samplingRate, 100.0) == 0) {
            Assert.assertTrue(itemsToSend.size() == generatedCount);
        }

        else {
            Assert.assertTrue(itemsToSend.size() < generatedCount);
        }
    }

    private void testNoSampling(TelemetryClient client , List<List<Telemetry>> dependencyTelemetry,
                                List<Telemetry> itemsToSend, List<String> includeTypes, List<String> excludeTypes, double samplingRate) {


        FixedRateSamplingTelemetryProcessor processor = new FixedRateSamplingTelemetryProcessor();
        processor.setSamplingPercentage(String.valueOf(samplingRate));
        if (includeTypes != null) {
            for (String includeType : includeTypes) {
                if (!StringUtils.isEmpty(includeType)) {
                    processor.addToIncludedType(includeType);
                }
            }
        }

        if (excludeTypes != null) {
            for (String excludeType : excludeTypes) {
                if (!StringUtils.isEmpty(excludeType)) {
                    processor.addToExcludedType(excludeType);
                }
            }
        }

        int generatedCount = 0;
        for (int i = 0; i < dependencyTelemetry.get(0).size(); ++i) {
            for (int j = 0; j < dependencyTelemetry.size(); ++j) {
                ++generatedCount;
                if (processor.process(dependencyTelemetry.get(j).get(i))) {
                    //Mockito.doReturn(mockContext).when(dependencyTelemetry.get(i)).getContext();
                    client.track(dependencyTelemetry.get(j).get(i));
                }
            }
        }

        Assert.assertTrue(itemsToSend.size() == generatedCount);

    }
}
