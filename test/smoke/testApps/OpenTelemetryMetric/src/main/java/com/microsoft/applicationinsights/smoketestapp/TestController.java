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

package com.microsoft.applicationinsights.smoketestapp;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  @GetMapping("/trackDoubleCounterMetric")
  public String trackDoubleCounterMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackDoubleCounterMetric");
    DoubleCounter counter = meter
        .counterBuilder("trackDoubleCounterMetric")
        .ofDoubles()
        .setUnit("1")
        .build();

    counter.add(1.0, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(2.0, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(1.0, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(2.0, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "green"));
    counter.add(5.0, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(4.0, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));

    return "OK!";
  }

  @GetMapping("/trackLongCounterMetric")
  public String trackLongCounterMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackLongCounterMetric");
    LongCounter counter = meter
        .counterBuilder("trackLongCounterMetric")
        .setUnit("1")
        .build();

    counter.add(1L, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(2L, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(1L, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(2L, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "green"));
    counter.add(5L, Attributes.of(AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(4L, Attributes.of(AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));

    return "OK!";
  }

  @GetMapping("/trackDoubleGaugeMetric")
  public String trackDoubleGaugeMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackDoubleGaugeMetric");
    meter.gaugeBuilder("trackDoubleGaugeMetric")
        .setDescription("the current temperature")
        .setUnit("C")
        .buildWithCallback(
            m -> {
              m.record(10.0, Attributes.of(AttributeKey.stringKey("thing1"), "thing2"));
            });

    return "OK!";
  }

  @GetMapping("/trackLongGaugeMetric")
  public String trackLongGaugeMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackLongGaugeMetric");
    meter.gaugeBuilder("trackLongGaugeMetric")
        .ofLongs()
        .setDescription("the current temperature")
        .setUnit("C")
        .buildWithCallback(
            m -> {
              m.record(10L, Attributes.of(AttributeKey.stringKey("thing1"), "thing2"));
            });

    return "OK!";
  }

  @GetMapping("/trackDoubleHistogramMetric")
  public String trackDoubleHistogramMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackDoubleHistogramMetric");
    DoubleHistogram doubleHistogram = meter.histogramBuilder("trackDoubleHistogramMetric")
        .setDescription("http.client.duration")
        .setUnit("ms")
        .build();

    doubleHistogram.record(456.0);

    return "OK!";
  }

  @GetMapping("/trackLongHistogramMetric")
  public String trackLongHistogramMetric() {
    Meter meter = GlobalOpenTelemetry.get().getMeter("trackLongHistogramMetric");
    LongHistogram longHistogram = meter.histogramBuilder("trackLongHistogramMetric")
        .ofLongs()
        .setDescription("http.client.duration")
        .setUnit("ms")
        .build();

    longHistogram.record(456L);

    return "OK!";
  }
}
