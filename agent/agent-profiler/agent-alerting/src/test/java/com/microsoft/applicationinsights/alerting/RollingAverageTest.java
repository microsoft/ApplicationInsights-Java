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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.analysis.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.RollingAverage;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import org.junit.*;

import static com.microsoft.applicationinsights.alerting.alert.AlertMetricType.CPU;

public class RollingAverageTest {

    @Test
    public void alertsConsumer() {
        AtomicReference<Double> called = new AtomicReference<>();
        Consumer<Double> consumer = called::set;
        RollingAverage rollingAverage = new RollingAverage()
                .setConsumer(consumer);

        rollingAverage.track(createDataPoint(0.1));

        Assert.assertNotNull(called.get());
    }

    @Test
    public void givesCorrectValue() {
        AtomicReference<Double> called = new AtomicReference<>();
        Consumer<Double> consumer = called::set;
        RollingAverage rollingAverage = new RollingAverage()
                .setConsumer(consumer);

        rollingAverage.track(createDataPoint(0.0));
        rollingAverage.track(createDataPoint(0.5));
        rollingAverage.track(createDataPoint(1.0));

        Assert.assertEquals(0.5d, called.get(), 0.01);
    }

    @Test
    public void throwsAwayDataOutsidePeriod() {
        AtomicReference<Double> called = new AtomicReference<>();
        Consumer<Double> consumer = called::set;

        AtomicLong offset = new AtomicLong(0);
        TimeSource timeSource = new TimeSource() {
            @Override
            public ZonedDateTime getNow() {
                return ZonedDateTime.now().plusSeconds(offset.get());
            }
        };

        RollingAverage rollingAverage = new RollingAverage(120, timeSource)
                .setConsumer(consumer);

        rollingAverage.track(createDataPoint(0.0));
        rollingAverage.track(createDataPoint(0.5));
        rollingAverage.track(createDataPoint(1.0));
        offset.set(150);
        rollingAverage.track(createDataPoint(0.1));
        rollingAverage.track(createDataPoint(0.1));

        Assert.assertEquals(0.1d, rollingAverage.track(createDataPoint(0.1)), 0.01);

        Assert.assertEquals(0.1d, called.get(), 0.01);
    }

    private TelemetryDataPoint createDataPoint(double v) {
        return new TelemetryDataPoint(CPU, TimeSource.DEFAULT.getNow(), v);
    }
}
