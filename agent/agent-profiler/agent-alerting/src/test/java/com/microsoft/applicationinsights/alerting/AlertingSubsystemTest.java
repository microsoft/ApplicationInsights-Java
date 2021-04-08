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
package com.microsoft.applicationinsights.alerting;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import org.junit.*;

public class AlertingSubsystemTest {

    private AlertingSubsystem getAlertMonitor(Consumer<AlertBreach> consumer) {
        AlertingSubsystem monitor = AlertingSubsystem.create(consumer, Executors.newSingleThreadExecutor());

        monitor.updateConfiguration(
                new AlertingConfiguration(
                        new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
                        new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
                        new DefaultConfiguration(true, 5, 120),
                        new CollectionPlanConfiguration(true, EngineMode.immediate, ZonedDateTime.now(), 120, "a-settings-moniker")
                )
        );
        return monitor;
    }

    @Test
    public void alertTriggerIsCalled() {

        AtomicReference<AlertBreach> called = new AtomicReference<>();
        Consumer<AlertBreach> consumer = called::set;

        AlertingSubsystem service = getAlertMonitor(consumer);

        service.track(AlertMetricType.CPU, 90.0);
        service.track(AlertMetricType.CPU, 90.0);
        service.track(AlertMetricType.CPU, 90.0);

        service.awaitQueueFlush();

        Assert.assertEquals(AlertMetricType.CPU, called.get().getType());
        Assert.assertEquals(90.0, called.get().getAlertValue(), 0.01);
    }

    @Test
    public void manualAlertWorks() {
        AtomicReference<AlertBreach> called = new AtomicReference<>();
        Consumer<AlertBreach> consumer = called::set;

        AlertingSubsystem service = AlertingSubsystem.create(consumer, Executors.newSingleThreadExecutor());

        service.updateConfiguration(
                new AlertingConfiguration(
                        new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
                        new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
                        new DefaultConfiguration(true, 5, 120),
                        new CollectionPlanConfiguration(true, EngineMode.immediate, ZonedDateTime.now().plus(100, ChronoUnit.SECONDS), 120, "a-settings-moniker")
                )
        );

        Assert.assertEquals(AlertMetricType.MANUAL, called.get().getType());
    }

    @Test
    public void manualAlertDoesNotTriggerAfterExpired() {
        AtomicReference<AlertBreach> called = new AtomicReference<>();
        Consumer<AlertBreach> consumer = called::set;

        AlertingSubsystem service = AlertingSubsystem.create(consumer, Executors.newSingleThreadExecutor());

        service.updateConfiguration(
                new AlertingConfiguration(
                        new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
                        new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
                        new DefaultConfiguration(true, 5, 120),
                        new CollectionPlanConfiguration(true, EngineMode.immediate, ZonedDateTime.now().minus(100, ChronoUnit.SECONDS), 120, "a-settings-moniker")
                )
        );

        Assert.assertNull(called.get());
    }
}
