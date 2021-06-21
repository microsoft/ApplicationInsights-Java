package com.microsoft.gcmonitortests;

import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class VariousCollectorsTest {

    @Test
    public void testCms() throws IOException, UnableToMonitorMemoryException, InterruptedException {
        try {
            List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseConcMarkSweepGC", 50)
                    .getGcCollectionEvents();

            assetGcsArePresent(events);
        } catch (GCNotPresentException e) {
            // CMS deprecated
        }
    }

    private void assetGcsArePresent(List<GCCollectionEvent> events) {
        assertThat(youngGcIsPresent(events)).isTrue();
        assertThat(tenuredGcIsPresent(events)).isTrue();
        assertThat(systemGcIsPresent(events)).isTrue();
    }

    @Test
    public void testParallel() throws IOException, UnableToMonitorMemoryException, InterruptedException, GCNotPresentException {
        List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseParallelGC", 70)
                .getGcCollectionEvents();

        List<String> youngPools = events.get(0).getYoungPools().stream().map(MemoryPool::getName)
                .collect(Collectors.toList());

        print(events);

        assetGcsArePresent(events);
    }

    @Test
    public void testG1() throws IOException, UnableToMonitorMemoryException, InterruptedException, GCNotPresentException {
        List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseG1GC", 50)
                .getGcCollectionEvents();

        assetGcsArePresent(events);
    }

    @Test
    public void testSerial() throws IOException, UnableToMonitorMemoryException, InterruptedException {
        try {
            List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseSerialGC", 50)
                    .getGcCollectionEvents();
            assetGcsArePresent(events);
        } catch (GCNotPresentException e) {
            // to be expected for some time
        }
    }

    /*
    TODO: Enable when this can use Java 11
    @Test
    public void testShenandoah() throws IOException, AttachNotSupportedException, UnableToMonitorMemoryException, InterruptedException {
        try {
            List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseShenandoahGC", 50)
                    .getGcCollectionEvents();

            assetGcsArePresent(events);
        } catch (GCNotPresentException e) {
            // to be expected for some time
        }
    }

    @Test
    public void testZ() throws IOException, AttachNotSupportedException, UnableToMonitorMemoryException, InterruptedException {
        try {
            List<GCCollectionEvent> events = new GcProcessRunner("-XX:+UseZGC", 200)
                    .getGcCollectionEvents();
            assetGcsArePresent(events);
        } catch (GCNotPresentException e) {
            // to be expected for some time
        }
    }
*/

    private boolean tenuredGcIsPresent(List<GCCollectionEvent> events) {
        return isPresent(events, event -> event.getCollector().isTenuredCollector() && memoryValuesAreSane(event));
    }

    private boolean youngGcIsPresent(List<GCCollectionEvent> events) {
        return isPresent(events, event -> event.getCollector().isYoungCollector() && memoryValuesAreSane(event));
    }

    private boolean memoryValuesAreSane(GCCollectionEvent event) {
        return event.getMemoryUsageBeforeGc(event.getTenuredPool().get()).getUsed() > 0 &&
                event.getMemoryUsageBeforeGc(event.getYoungPools()).getUsed() > 0 &&
                event.getMemoryUsageAfterGc(event.getTenuredPool().get()).getUsed() > 0;
    }

    private boolean systemGcIsPresent(List<GCCollectionEvent> events) {
        return isPresent(events, event -> event.getGcCause().contains("System.gc()") && memoryValuesAreSane(event));
    }

    private boolean isPresent(List<GCCollectionEvent> events, Predicate<GCCollectionEvent> predicate) {
        return events
                .stream()
                .anyMatch(predicate);
    }


    private void print(List<GCCollectionEvent> events) {
        System.out.println("Obtained: " + events.size());
        events.forEach(event -> {
            Optional<MemoryPool> tenuredPool = event.getTenuredPool();
            if (tenuredPool.isPresent()) {
                System.out.println(
                        new StringJoiner(",")
                                .add(Long.toString(event.getId()))
                                .add(event.getGcCause())
                                .add(Long.toString(event.getDuration()))
                                .add(Long.toString(event.getEndTime()))
                                .add(event.getCollector().toString())
                                .add(event.getGcAction())
                                .add(Long.toString(event.getMemoryUsageBeforeGc(tenuredPool.get()).getUsed()))
                                .add(Long.toString(event.getMemoryUsageAfterGc(tenuredPool.get()).getUsed())));
            }
        });
    }
}
