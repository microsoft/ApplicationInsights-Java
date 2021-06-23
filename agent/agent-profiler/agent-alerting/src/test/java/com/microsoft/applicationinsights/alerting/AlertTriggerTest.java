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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.analysis.AlertPipelineTrigger;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTriggerTest {

    @Test
    void underThresholdDataDoesNotTrigger() {
        AlertConfiguration config = new AlertConfiguration(AlertMetricType.CPU, true, 0.5f, 1, 1000);
        AtomicBoolean called = new AtomicBoolean(false);
        AlertPipelineTrigger trigger = getAlertTrigger(config, called);
        for (int i = 0; i < 100; i++) {
            trigger.accept(0.4);
        }

        assertThat(called.get()).isFalse();
    }

    @Test
    void overThresholdDataDoesTrigger() {

        AlertConfiguration config = new AlertConfiguration(AlertMetricType.CPU, true, 0.5f, 1, 1);
        AtomicBoolean called = new AtomicBoolean(false);
        AlertPipelineTrigger trigger = getAlertTrigger(config, called);

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.51);
        }

        assertThat(called.get()).isTrue();
    }


    @Test
    void doesNotReTriggerDueToCooldown() {
        AlertConfiguration config = new AlertConfiguration(AlertMetricType.CPU, true, 0.5f, 1, 1000);
        AtomicBoolean called = new AtomicBoolean(false);
        AlertPipelineTrigger trigger = getAlertTrigger(config, called);

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.51);
        }
        assertThat(called.get()).isTrue();
        called.set(false);

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.1);
        }

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.51);
        }

        assertThat(called.get()).isFalse();
    }


    @Test
    void doesNotReTriggerAfterCooldown() throws InterruptedException {
        AlertConfiguration config = new AlertConfiguration(AlertMetricType.CPU, true, 0.5f, 1, 1);
        AtomicBoolean called = new AtomicBoolean(false);
        AlertPipelineTrigger trigger = getAlertTrigger(config, called);

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.51);
        }
        assertThat(called.get()).isTrue();
        called.set(false);

        Thread.sleep(2000);

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.1);
        }

        for (int i = 0; i < 100; i++) {
            trigger.accept(0.51);
        }

        assertThat(called.get()).isTrue();
    }

    private static AlertPipelineTrigger getAlertTrigger(AlertConfiguration config, AtomicBoolean called) {
        Consumer<AlertBreach> consumer = alert -> {
            assertThat(alert.getType()).isEqualTo(AlertMetricType.CPU);
            assertThat(alert.getAlertConfiguration()).isEqualTo(config);
            called.set(true);
        };

        return new AlertPipelineTrigger(config, consumer);
    }
}
